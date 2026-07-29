package com.smarthiring.service;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CvTextExtractorTest {

    private final CvTextExtractor extractor = new CvTextExtractor();

    @Test
    void extractsAndCleansTextCv() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "worker-cv.txt", "text/plain", "Waiter\r\n\r\nPOS   service".getBytes()
        );

        CvTextExtractor.ExtractedCv result = extractor.extract(file);

        assertThat(result.fileName()).isEqualTo("worker-cv.txt");
        assertThat(result.text()).contains("Waiter").contains("POS service");
    }

    @Test
    void rejectsUnsupportedCvType() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "worker-cv.exe", "application/octet-stream", "not a cv".getBytes()
        );

        assertThatThrownBy(() -> extractor.extract(file))
                .hasMessageContaining("PDF, DOCX, or TXT");
    }
}
