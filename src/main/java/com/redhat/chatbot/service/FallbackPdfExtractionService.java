package com.redhat.chatbot.service;

import java.awt.Rectangle;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.PDFTextStripperByArea;
import org.springframework.stereotype.Service;

@Service
public class FallbackPdfExtractionService {

    public List<String> extractTextFromPdf(byte[] pdfBytes) {

        try {
            return extractWithPdfBox(pdfBytes);
        } catch (Exception e) {
            throw new RuntimeException("Failed to extract text from PDF", e);
        }
    }

    private List<String> extractWithPdfBox(byte[] pdfBytes) throws IOException {

        List<String> pages = new ArrayList<>();

        try (PDDocument document = Loader.loadPDF(pdfBytes)) {
            PDFTextStripper stripper = new PDFTextStripper();

            for (int page = 1; page <= document.getNumberOfPages(); page++) {
                stripper.setStartPage(page);
                stripper.setEndPage(page);

                String pageText = stripper.getText(document);
                pages.add(pageText);
            }
        }

        return pages;
    }

    public String extractTextWithLayout(byte[] pdfBytes) throws IOException {

        try (PDDocument document = Loader.loadPDF(pdfBytes)) {
            PDFTextStripperByArea stripper = new PDFTextStripperByArea();
            stripper.setSortByPosition(true);

            StringBuilder fullText = new StringBuilder();

            for (int page = 0; page < document.getNumberOfPages(); page++) {
                // Define regions for better layout preservation
                Rectangle region = new Rectangle(0, 0, 612, 792); // Standard letter size
                stripper.addRegion("page" + page, region);
                stripper.extractRegions(document.getPage(page));

                String pageText = stripper.getTextForRegion("page" + page);
                fullText.append(pageText).append("\n\n");
            }

            return fullText.toString();
        }
    }

}
