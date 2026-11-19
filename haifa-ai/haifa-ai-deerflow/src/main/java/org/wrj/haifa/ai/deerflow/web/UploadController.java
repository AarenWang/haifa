package org.wrj.haifa.ai.deerflow.web;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.codec.multipart.Part;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.util.StringUtils;
import org.wrj.haifa.ai.deerflow.upload.ConversionStatus;
import org.wrj.haifa.ai.deerflow.upload.DocumentConversionService;
import org.wrj.haifa.ai.deerflow.upload.UploadContentResponse;
import org.wrj.haifa.ai.deerflow.upload.UploadListResponse;
import org.wrj.haifa.ai.deerflow.upload.UploadRecord;
import org.wrj.haifa.ai.deerflow.upload.UploadResponse;
import org.wrj.haifa.ai.deerflow.upload.UploadStorageService;

import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@RestController
@RequestMapping("/api/deerflow/uploads")
public class UploadController {

    private final UploadStorageService uploadStorageService;
    private final DocumentConversionService documentConversionService;

    public UploadController(UploadStorageService uploadStorageService, DocumentConversionService documentConversionService) {
        this.uploadStorageService = uploadStorageService;
        this.documentConversionService = documentConversionService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Mono<UploadResponse> upload(ServerWebExchange exchange, @RequestParam(name = "threadId", required = false) String threadId) {
        return exchange.getMultipartData()
                .flatMap(parts -> {
                    Part filePart = parts.getFirst("file");
                    if (filePart == null) {
                        return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "file part is required"));
                    }
                    return Mono.fromCallable(() -> {
                        String requiredThreadId = requireThreadId(threadId);
                        UploadRecord record = uploadStorageService.store(filePart, requiredThreadId);
                        UploadRecord convertedRecord = documentConversionService.convert(record.getFileId());
                        return toResponse(convertedRecord);
                    }).subscribeOn(Schedulers.boundedElastic());
                })
                .onErrorMap(IllegalArgumentException.class, this::mapUploadException);
    }

    @GetMapping
    public Mono<UploadListResponse> list(@RequestParam(name = "threadId", required = false) String threadId) {
        return Mono.fromCallable(() -> {
            String requiredThreadId = requireThreadId(threadId);
            List<UploadRecord> records = uploadStorageService.list(requiredThreadId);
            List<UploadResponse> responses = records.stream()
                    .map(this::toResponse)
                    .collect(Collectors.toList());
            return new UploadListResponse(responses);
        }).subscribeOn(Schedulers.boundedElastic())
                .onErrorMap(IllegalArgumentException.class, this::mapUploadException);
    }

    @GetMapping("/{fileId}")
    public Mono<UploadResponse> get(@PathVariable String fileId, @RequestParam(name = "threadId", required = false) String threadId) {
        return Mono.fromCallable(() -> {
            String requiredThreadId = requireThreadId(threadId);
            UploadRecord record = uploadStorageService.findByFileIdAndThreadId(fileId, requiredThreadId);
            if (record == null) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "File not found: " + fileId);
            }
            return toResponse(record);
        }).subscribeOn(Schedulers.boundedElastic())
                .onErrorMap(IllegalArgumentException.class, this::mapUploadException);
    }

    @GetMapping("/{fileId}/content")
    public Mono<UploadContentResponse> content(@PathVariable String fileId, @RequestParam(name = "threadId", required = false) String threadId) {
        return Mono.fromCallable(() -> {
            String requiredThreadId = requireThreadId(threadId);
            UploadRecord record = uploadStorageService.findByFileIdAndThreadId(fileId, requiredThreadId);
            if (record == null) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "File not found: " + fileId);
            }
            String content = uploadStorageService.readContent(fileId, requiredThreadId);
            return new UploadContentResponse(
                    record.getFileId(),
                    record.getOriginalFilename(),
                    content);
        }).subscribeOn(Schedulers.boundedElastic())
                .onErrorMap(IllegalArgumentException.class, this::mapUploadException);
    }

    @DeleteMapping("/{fileId}")
    public Mono<Void> delete(@PathVariable String fileId, @RequestParam(name = "threadId", required = false) String threadId) {
        return Mono.<Void>fromCallable(() -> {
            String requiredThreadId = requireThreadId(threadId);
            UploadRecord record = uploadStorageService.findByFileIdAndThreadId(fileId, requiredThreadId);
            if (record == null) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "File not found: " + fileId);
            }
            uploadStorageService.delete(fileId, requiredThreadId);
            return null;
        }).subscribeOn(Schedulers.boundedElastic())
                .onErrorMap(IllegalArgumentException.class, this::mapUploadException);
    }

    private UploadResponse toResponse(UploadRecord record) {
        return new UploadResponse(
                record.getFileId(),
                record.getOriginalFilename(),
                record.getContentType() != null ? record.getContentType() : "application/octet-stream",
                record.getSize(),
                record.getExtension(),
                record.getThreadId(),
                record.getCreatedAt() != null ? record.getCreatedAt().toString() : "",
                mapStatus(record.getConversionStatus()),
                record.getContentPreview());
    }

    private static String mapStatus(ConversionStatus status) {
        if (status == null) return "pending";
        return switch (status) {
            case PENDING -> "pending";
            case COMPLETED -> "completed";
            case FAILED -> "failed";
            case UNSUPPORTED -> "failed";
        };
    }

    private static String requireThreadId(String threadId) {
        if (!StringUtils.hasText(threadId)) {
            throw new IllegalArgumentException("threadId is required");
        }
        return threadId.trim();
    }

    private ResponseStatusException mapUploadException(IllegalArgumentException ex) {
        HttpStatus status = ex.getMessage() != null && ex.getMessage().contains("exceeds maximum allowed")
                ? HttpStatus.PAYLOAD_TOO_LARGE
                : HttpStatus.BAD_REQUEST;
        return new ResponseStatusException(status, ex.getMessage(), ex);
    }
}
