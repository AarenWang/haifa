package org.wrj.haifa.ai.deerflow.persistence.store;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.wrj.haifa.ai.deerflow.upload.UploadRecord;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class UploadStoreTest {

    @Autowired
    private UploadStore uploadStore;

    @Test
    void savesAndRetrievesUploadMetadata() {
        UploadRecord record = new UploadRecord();
        record.setFileId("file-1");
        record.setOriginalFilename("test.txt");
        record.setContentType("text/plain");
        record.setSize(42);
        record.setExtension("txt");
        record.setThreadId("thread-1");
        record.setStoredPath("/tmp/test.txt");

        uploadStore.save(record);

        UploadRecord found = uploadStore.find("file-1");
        assertThat(found).isNotNull();
        assertThat(found.getOriginalFilename()).isEqualTo("test.txt");
        assertThat(found.getSize()).isEqualTo(42);
    }

    @Test
    void listByThreadIdWorks() {
        UploadRecord r1 = new UploadRecord();
        r1.setFileId("file-a");
        r1.setOriginalFilename("a.txt");
        r1.setThreadId("thread-list");
        r1.setStoredPath("/tmp/a.txt");
        uploadStore.save(r1);

        UploadRecord r2 = new UploadRecord();
        r2.setFileId("file-b");
        r2.setOriginalFilename("b.txt");
        r2.setThreadId("thread-list");
        r2.setStoredPath("/tmp/b.txt");
        uploadStore.save(r2);

        assertThat(uploadStore.list("thread-list")).hasSize(2);
        assertThat(uploadStore.list("nonexistent")).isEmpty();
    }

    @Test
    void deleteRemovesMetadata() {
        UploadRecord record = new UploadRecord();
        record.setFileId("file-del");
        record.setOriginalFilename("del.txt");
        record.setThreadId("thread-del");
        record.setStoredPath("/tmp/del.txt");
        uploadStore.save(record);

        assertThat(uploadStore.find("file-del")).isNotNull();
        uploadStore.delete("file-del", "thread-del");
        assertThat(uploadStore.find("file-del")).isNull();
    }

    @Test
    void findByFileIdAndThreadIdRespectsOwnership() {
        UploadRecord record = new UploadRecord();
        record.setFileId("file-own");
        record.setOriginalFilename("own.txt");
        record.setThreadId("owner-thread");
        record.setStoredPath("/tmp/own.txt");
        uploadStore.save(record);

        assertThat(uploadStore.findByFileIdAndThreadId("file-own", "owner-thread")).isNotNull();
        assertThat(uploadStore.findByFileIdAndThreadId("file-own", "wrong-thread")).isNull();
    }

    @Test
    void persistsConvertedContentAndPreview() {
        UploadRecord record = new UploadRecord();
        record.setFileId("file-converted");
        record.setOriginalFilename("converted.txt");
        record.setThreadId("thread-converted");
        record.setStoredPath("/tmp/converted.txt");
        record.setConverted(true);
        record.setConvertedContent("full converted content");
        record.setContentPreview("preview");
        record.setConversionStatus(org.wrj.haifa.ai.deerflow.upload.ConversionStatus.COMPLETED);

        uploadStore.save(record);

        UploadRecord found = uploadStore.find("file-converted");
        assertThat(found).isNotNull();
        assertThat(found.isConverted()).isTrue();
        assertThat(found.getConversionStatus()).isEqualTo(org.wrj.haifa.ai.deerflow.upload.ConversionStatus.COMPLETED);
        assertThat(found.getConvertedContent()).isEqualTo("full converted content");
        assertThat(found.getContentPreview()).isEqualTo("preview");
    }
}
