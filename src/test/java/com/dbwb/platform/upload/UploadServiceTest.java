package com.dbwb.platform.upload;

import com.dbwb.platform.common.exception.BusinessRuleViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UploadServiceTest {

    @TempDir
    Path tempDir;

    private UploadService uploadService;

    @BeforeEach
    void setUp() {
        UploadProperties properties = new UploadProperties();
        properties.setDirectory(tempDir.toString());
        properties.setMaxFileSizeMb(1);
        uploadService = new UploadService(properties);
    }

    @Test
    void storesAValidImageAndReturnsItsFilename() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "logo.png", "image/png", new byte[]{1, 2, 3});

        String filename = uploadService.storeImage(file);

        assertThat(filename).endsWith(".png");
        assertThat(Files.exists(tempDir.resolve(filename))).isTrue();
    }

    @Test
    void rejectsNonImageContentTypes() {
        MockMultipartFile file = new MockMultipartFile("file", "menu.csv", "text/csv", new byte[]{1});

        assertThatThrownBy(() -> uploadService.storeImage(file))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("JPEG, PNG, WEBP, or GIF");
    }

    @Test
    void rejectsFilesLargerThanTheConfiguredLimit() {
        MockMultipartFile file = new MockMultipartFile("file", "big.png", "image/png", new byte[2 * 1024 * 1024]);

        assertThatThrownBy(() -> uploadService.storeImage(file))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("smaller than");
    }

    @Test
    void rejectsAnEmptyFile() {
        MockMultipartFile file = new MockMultipartFile("file", "empty.png", "image/png", new byte[0]);

        assertThatThrownBy(() -> uploadService.storeImage(file))
                .isInstanceOf(BusinessRuleViolationException.class);
    }
}
