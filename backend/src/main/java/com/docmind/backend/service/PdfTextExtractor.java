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
            String text = stripper.getText(pdf).replace("\u0000", "").replace("\r\n", "\n").replace("\r", "\n").strip();
            return new Extracted(pages, text);

        } catch (ResponseStatusException e) {
            throw e; // our own "too many pages" answer
        } catch (InvalidPasswordException e) {
            throw unreadable("Password-protected PDFs are not supported");
        } catch (IOException | RuntimeException e) {
            // A broken or hostile file can fail in many ways inside the library
            throw unreadable("That file is not a readable PDF");
        }
    }

    private static ResponseStatusException unreadable(String reason) {
        return new ResponseStatusException(HttpStatusCode.valueOf(422), reason);
    }
}