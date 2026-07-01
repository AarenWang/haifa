package org.wrj.haifa.ai.deerflow.web;

import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.wrj.haifa.ai.deerflow.artifact.ArtifactRecord;
import org.wrj.haifa.ai.deerflow.artifact.ArtifactService;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@RestController
@RequestMapping("/api/deerflow/artifacts")
public class ArtifactController {

    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_INSTANT;
    private static final int PREVIEW_CHARS = 80_000;

    private final ArtifactService artifactService;

    public ArtifactController(ArtifactService artifactService) {
        this.artifactService = artifactService;
    }

    @GetMapping
    public Mono<List<ArtifactResponse>> list(
            @RequestParam(name = "threadId", required = false) String threadId,
            @RequestParam(name = "runId", required = false) String runId) {
        return Mono.fromCallable(() -> artifactService.list(threadId, runId).stream()
                .map(record -> toResponse(record, ""))
                .toList()).subscribeOn(Schedulers.boundedElastic());
    }

    @GetMapping("/{artifactId}")
    public Mono<ArtifactResponse> get(@PathVariable String artifactId) {
        return Mono.fromCallable(() -> {
            ArtifactRecord record = artifactService.find(artifactId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Artifact not found"));
            return toResponse(record, artifactService.readMarkdownPreview(artifactId, PREVIEW_CHARS));
        }).subscribeOn(Schedulers.boundedElastic())
                .onErrorMap(IllegalArgumentException.class, ex ->
                        new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex));
    }

    @GetMapping("/{artifactId}/download")
    public Mono<ResponseEntity<Resource>> download(@PathVariable String artifactId) {
        return Mono.fromCallable(() -> {
            ArtifactRecord record = artifactService.find(artifactId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Artifact not found"));
            Path path = artifactService.resolveForDownload(artifactId);
            FileSystemResource resource = new FileSystemResource(path);
            MediaType mediaType = MediaType.parseMediaType(record.mimeType());
            return ResponseEntity.ok()
                    .contentType(mediaType)
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            ContentDisposition.attachment().filename(record.filename()).build().toString())
                    .contentLength(record.size())
                    .body((Resource) resource);
        }).subscribeOn(Schedulers.boundedElastic())
                .onErrorMap(IllegalArgumentException.class, ex ->
                        new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex));
    }

    private static ArtifactResponse toResponse(ArtifactRecord record, String preview) {
        return new ArtifactResponse(
                record.artifactId(),
                record.runId(),
                record.threadId(),
                record.filename(),
                record.mimeType(),
                record.size(),
                record.createdAt() == null ? "" : ISO.format(record.createdAt()),
                preview == null ? "" : preview
        );
    }
}
