package com.smarthiring.service;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.Locale;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

@Component
public class CvTextExtractor {

    private static final long MAX_FILE_SIZE = 5L * 1024L * 1024L;
    private static final int MAX_TEXT_LENGTH = 20_000;

    public ExtractedCv extract(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(BAD_REQUEST, "Please choose a CV file");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new ResponseStatusException(BAD_REQUEST, "CV files must be 5 MB or smaller");
        }

        String fileName = safeFileName(file.getOriginalFilename());
        String extension = extension(fileName);
        if (!extension.equals("pdf") && !extension.equals("docx") && !extension.equals("txt")) {
            throw new ResponseStatusException(BAD_REQUEST, "Upload a PDF, DOCX, or TXT CV");
        }

        try {
            byte[] bytes = file.getBytes();
            String text = switch (extension) {
                case "pdf" -> extractPdf(bytes);
                case "docx" -> extractDocx(bytes);
                default -> new String(bytes, StandardCharsets.UTF_8);
            };
            String cleaned = clean(text);
            if (cleaned.isBlank()) {
                throw new ResponseStatusException(BAD_REQUEST, "We couldn't find readable text in this CV");
            }
            return new ExtractedCv(fileName, cleaned.substring(0, Math.min(cleaned.length(), MAX_TEXT_LENGTH)));
        } catch (ResponseStatusException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new ResponseStatusException(BAD_REQUEST, "We couldn't read this CV. Try a text-based PDF, DOCX, or TXT file");
        }
    }

    private String extractPdf(byte[] bytes) throws Exception {
        try (var document = Loader.loadPDF(bytes)) {
            return new PDFTextStripper().getText(document);
        }
    }

    private String extractDocx(byte[] bytes) throws Exception {
        try (var document = new XWPFDocument(new ByteArrayInputStream(bytes));
             var extractor = new XWPFWordExtractor(document)) {
            return extractor.getText();
        }
    }

    private String safeFileName(String originalName) {
        String fallback = originalName == null || originalName.isBlank() ? "cv" : originalName;
        String name = Paths.get(fallback).getFileName().toString().trim();
        if (name.length() <= 255) {
            return name;
        }
        int dot = name.lastIndexOf('.');
        String suffix = dot >= 0 && name.length() - dot <= 12 ? name.substring(dot) : "";
        return name.substring(0, 255 - suffix.length()) + suffix;
    }

    private String extension(String fileName) {
        int dot = fileName.lastIndexOf('.');
        return dot < 0 ? "" : fileName.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    private String clean(String text) {
        return text == null ? "" : text.replace('\u0000', ' ').replaceAll("[\\t\\x0B\\f\\r ]+", " ")
                .replaceAll(" *\\n+ *", "\\n").trim();
    }

    public record ExtractedCv(String fileName, String text) {}
}
