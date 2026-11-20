package com.docmind.backend.service;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;

// Opens a PDF and reads the page count and the text. Any PDF is untrusted input from a
// stranger, so everything that can go wrong becomes a clean 422 and nothing leaks.
@Component
public class PdfTextExtractor {

    // A small file can still hold thousands of pages, so we cap them
    static final int MAX_PAGES = 200;

    // text: the text of all pages, with a page break character after EVERY page (also an empty one),
    // so a position in the text can always be turned into a page number (see PageMap)
    public record Extracted(int pageCount, String text) {
    }

    public Extracted extract(byte[] pdfBytes) {
        try (PDDocument pdf = Loader.loadPDF(pdfBytes)) {
            int pages = pdf.getNumberOfPages();
            if (pages > MAX_PAGES) {
                throw unreadable("That PDF has too many pages (the limit is " + MAX_PAGES + ")");
            }

            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setLineSeparator("\n"); // the same on Windows and Linux
            stripper.setPageEnd("");         // we add our own page break below

            // One page at a time, so we always know where each page ends
            StringBuilder text = new StringBuilder();
            for (int page = 1; page <= pages; page++) {
                stripper.setStartPage(page);
                stripper.setEndPage(page);
                text.append(clean(stripper.getText(pdf))).append(PageMap.PAGE_BREAK);
            }
            return new Extracted(pages, text.toString());

        } catch (ResponseStatusException e) {
            throw e; // our own "too many pages" answer
        } catch (InvalidPasswordException e) {
            throw unreadable("Password-protected PDFs are not supported");
        } catch (IOException | RuntimeException e) {
            // A broken or hostile file can fail in many ways inside the library
            throw unreadable("That file is not a readable PDF");
        }
    }

    // Same line endings everywhere, no empty characters, and no page break characters of the PDF's own
    // (only OUR page breaks may count as page ends)
    private static String clean(String pageText) {
        return pageText.replace("\u0000", "")
                .replace(PageMap.PAGE_BREAK, ' ')
                .replace("\r\n", "\n")
                .replace("\r", "\n")
                .strip();
    }

    private static ResponseStatusException unreadable(String reason) {
        return new ResponseStatusException(HttpStatusCode.valueOf(422), reason);
    }
}