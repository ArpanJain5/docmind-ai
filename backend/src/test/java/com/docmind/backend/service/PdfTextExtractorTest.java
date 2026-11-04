package com.docmind.backend.service;

import com.docmind.backend.TestPdfs;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PdfTextExtractorTest {

    private final PdfTextExtractor extractor = new PdfTextExtractor();

    @Test
    void readsThePageCountAndTheTextOfEveryPage() {
        PdfTextExtractor.Extracted result =
                extractor.extract(TestPdfs.withPages("First page text", "Second page text"));

        assertThat(result.pageCount()).isEqualTo(2);
        assertThat(result.text())
        .contains("First page text")
        .contains("Second page text")
        .doesNotContain("\r");
    }

    @Test
    void aPdfWithoutTextGivesEmptyText() {
        PdfTextExtractor.Extracted result = extractor.extract(TestPdfs.blank(2));

        assertThat(result.pageCount()).isEqualTo(2);
        assertThat(result.text()).isBlank();
    }

    @Test
    void aFileThatStartsLikeAPdfButIsGarbageGives422() {
        byte[] garbage = "%PDF-1.4\nthis is not a real pdf at all\n".getBytes(StandardCharsets.US_ASCII);

        assertThatThrownBy(() -> extractor.extract(garbage))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode().value()).isEqualTo(422));
    }

    @Test
    void aPasswordProtectedPdfGives422() {
        assertThatThrownBy(() -> extractor.extract(TestPdfs.passwordProtected()))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode().value()).isEqualTo(422));
    }

    @Test
    void aPdfWithTooManyPagesGives422() {
        byte[] tooLong = TestPdfs.blank(PdfTextExtractor.MAX_PAGES + 1);

        assertThatThrownBy(() -> extractor.extract(tooLong))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode().value()).isEqualTo(422));
    }
}