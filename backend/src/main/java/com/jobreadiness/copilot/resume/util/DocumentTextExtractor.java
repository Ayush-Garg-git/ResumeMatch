package com.jobreadiness.copilot.resume.util;

import com.jobreadiness.copilot.common.exception.BadRequestException;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.io.InputStream;

@Component
public class DocumentTextExtractor {

    public String extractText(MultipartFile file) throws IOException {
        String filename = file.getOriginalFilename();
        if (filename == null) {
            throw new BadRequestException("Filename cannot be empty");
        }

        String text;
        if (filename.toLowerCase().endsWith(".pdf")) {
            text = extractTextFromPdf(file.getInputStream());
        } else if (filename.toLowerCase().endsWith(".docx")) {
            text = extractTextFromDocx(file.getInputStream());
        } else {
            throw new BadRequestException("Unsupported file type. Only PDF and DOCX files are allowed.");
        }

        if (text == null || text.trim().length() < 40) {
            throw new BadRequestException("The uploaded document appears to be a scanned image or empty PDF without extractable text. Please upload a standard text-based PDF or DOCX exported directly from Google Docs / Word.");
        }

        return text;
    }

    private String extractTextFromPdf(InputStream inputStream) throws IOException {
        byte[] bytes = inputStream.readAllBytes();
        try (PDDocument document = Loader.loadPDF(bytes)) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true); // Groups multi-column and table text geometrically to prevent interleaving
            return stripper.getText(document);
        }
    }

    private String extractTextFromDocx(InputStream inputStream) throws IOException {
        try (XWPFDocument document = new XWPFDocument(inputStream);
             XWPFWordExtractor extractor = new XWPFWordExtractor(document)) {
            return extractor.getText();
        }
    }
}
