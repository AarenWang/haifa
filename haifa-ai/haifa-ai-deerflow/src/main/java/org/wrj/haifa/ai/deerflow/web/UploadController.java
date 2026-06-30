package org.wrj.haifa.ai.deerflow.web;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
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
    public Mono<UploadResponse> upload(@RequestPart("file") FilePart filePart, @RequestParam(name = "threadId", required = false) String threadId) {
        return Mono.fromCallable(() -> {
            UploadRecord record = uploadStorageService.store(filePart, threadId);
            documentConversionService.convert(record.getFileId());
            return toResponse(record);
        }).subscribeOn(Schedulers.boundedElastic());
    }

    @GetMapping
    public Mono<UploadListResponse> list(@RequestParam(name = "threadId", required = false) String threadId) {
        return Mono.fromCallable(() -> {
            List<UploadRecord> records = uploadStorageService.list(threadId);
            List<UploadResponse> responses = records.stream()
                    .map(this::toResponse)
                    .collect(Collectors.toList());
            return new UploadListResponse(responses);
        }).subscribeOn(Schedulers.boundedElastic());
    }

    @GetMapping("/{fileId}")
    public Mono<UploadResponse> get(@PathVariable String fileId) {
        return Mono.fromCallable(() -> {
            UploadRecord record = uploadStorageService.find(fileId);
            if (record == null) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "File not found: " + fileId);
            }
            return toResponse(record);
        }).subscribeOn(Schedulers.boundedElastic());
    }

    @GetMapping("/{fileId}/content")
    public Mono<UploadContentResponse> content(@PathVariable String fileId) {
        return Mono.fromCallable(() -> {
            UploadRecord record = uploadStorageService.find(fileId);
            if (record == null) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "File not found: " + fileId);
            }
            String content = record.getConvertedContent();
            if (content == null) {
                content = "";
            }
            return new UploadContentResponse(
                    record.getFileId(),
                    record.getOriginalFilename(),
                    content);
        }).subscribeOn(Schedulers.boundedElastic());
    }

    @DeleteMapping("/{fileId}")
    public Mono<Void> delete(@PathVariable String fileId) {
        return Mono.<Void>fromCallable(() -> {
            uploadStorageService.delete(fileId);
            return null;
        }).subscribeOn(Schedulers.boundedElastic());
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
}
