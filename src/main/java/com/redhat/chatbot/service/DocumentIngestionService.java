package com.redhat.chatbot.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.redhat.chatbot.model.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class DocumentIngestionService {

    @Autowired
    private OpenVinoLayoutLMService openVinoLayoutLMService;

    @Autowired
    private OnnxLayoutLMService onnxLayoutLMService;

    @Autowired
    private FallbackPdfExtractionService fallbackPdfExtractionService;

    @Autowired
    private EmbeddingService embeddingService;

    @Autowired
    private VectorStoreService vectorStoreService;

    public Document ingestPdf(MultipartFile file) throws IOException {

        String filename = file.getOriginalFilename();
        byte[] pdfBytes = file.getBytes();

        List<String> extractedText = extractTextWithFallback(pdfBytes);
        String fullText = String.join("\n", extractedText);

        // Create document
        Document document = new Document(filename, fullText);
        document.setId(UUID.randomUUID().toString());

        // Chunk the document
        List<String> chunks = chunkText(fullText, 512);
        document.setChunks(chunks);

        // Add metadata
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("filename", filename);
        metadata.put("size", file.getSize());
        metadata.put("type", "pdf");
        metadata.put("pages", extractedText.size());
        document.setMetadata(metadata);

        // Generate embeddings and store in vector database
        storeDocumentChunks(document, chunks, metadata);

        return document;
    }

    private List<String> extractTextWithFallback(byte[] pdfBytes) {

        try {
            // Try OpenVINO first
            return openVinoLayoutLMService.extractTextFromPdf(pdfBytes);
        } catch (Exception e1) {
            try {
                // Try ONNX as fallback
                return onnxLayoutLMService.extractTextFromPdf(pdfBytes);
            } catch (Exception e2) {
                // Use PDFBox as final fallback
                return fallbackPdfExtractionService.extractTextFromPdf(pdfBytes);
            }
        }
    }

    private void storeDocumentChunks(Document document, List<String> chunks, Map<String, Object> metadata) {

        for (int i = 0; i < chunks.size(); i++) {
            String chunk = chunks.get(i);
            if (chunk.trim().isEmpty()) {
                continue;
            }

            float[] embedding = embeddingService.generateEmbedding(chunk);

            Map<String, Object> chunkMetadata = new HashMap<>(metadata);
            chunkMetadata.put("chunk_index", i);
            chunkMetadata.put("document_id", document.getId());
            chunkMetadata.put("chunk_text", chunk);

            vectorStoreService.storeVector(
                    document.getId() + "_chunk_" + i,
                    embedding,
                    chunk,
                    chunkMetadata
            );
        }
    }

    private List<String> chunkText(String text, int maxChunkSize) {

        List<String> chunks = new ArrayList<>();
        String[] sentences = text.split("\\. ");

        StringBuilder currentChunk = new StringBuilder();

        for (String sentence : sentences) {
            if (currentChunk.length() + sentence.length() > maxChunkSize) {
                if (!currentChunk.isEmpty()) {
                    chunks.add(currentChunk.toString().trim());
                    currentChunk = new StringBuilder();
                }
            }
            currentChunk.append(sentence).append(". ");
        }

        if (!currentChunk.isEmpty()) {
            chunks.add(currentChunk.toString().trim());
        }

        return chunks;
    }

}
