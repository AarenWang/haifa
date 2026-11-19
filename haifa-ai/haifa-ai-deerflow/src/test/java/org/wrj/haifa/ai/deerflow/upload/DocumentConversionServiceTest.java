package org.wrj.haifa.ai.deerflow.upload;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.wrj.haifa.ai.deerflow.config.DeerFlowProperties;
import org.wrj.haifa.ai.deerflow.persistence.store.UploadStore;

class DocumentConversionServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void convertsDocxContent() throws Exception {
        Path docxPath = tempDir.resolve("requirements.docx");
        writeDocx(docxPath);

        UploadRecord record = new UploadRecord(
                "file-1",
                "requirements.docx",
                "file-1_requirements.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                Files.size(docxPath),
                "docx",
                "thread-1",
                docxPath.toString());

        DeerFlowProperties properties = new DeerFlowProperties();
        properties.setMaxConvertedChars(60000);
        UploadStorageService uploadStorageService = mock(UploadStorageService.class);
        UploadStore uploadStore = mock(UploadStore.class);
        when(uploadStorageService.find("file-1")).thenReturn(record);
        when(uploadStore.save(any(UploadRecord.class))).thenAnswer(invocation -> invocation.getArgument(0));

        DocumentConversionService service = new DocumentConversionService(uploadStorageService, properties, uploadStore);

        UploadRecord converted = service.convert("file-1");

        assertThat(converted.getConversionStatus()).isEqualTo(ConversionStatus.COMPLETED);
        assertThat(converted.getConvertedContent())
                .contains("Haifa DeerFlow upload test")
                .contains("factory requirements");
        assertThat(converted.getContentPreview()).contains("Haifa DeerFlow");
    }

    private static void writeDocx(Path path) throws Exception {
        try (XWPFDocument document = new XWPFDocument()) {
            document.createParagraph().createRun().setText("Haifa DeerFlow upload test");
            XWPFTable table = document.createTable(1, 1);
            table.getRow(0).getCell(0).setText("factory requirements");
            try (OutputStream outputStream = Files.newOutputStream(path)) {
                document.write(outputStream);
            }
        }
    }
}
