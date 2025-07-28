package com.redhat.chatbot.service;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.redhat.chatbot.config.ModelInferenceConfig;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
public class OpenVinoLayoutLMService {

    private final WebClient webClient;

    private final ObjectMapper objectMapper;

    @Autowired
    private ModelInferenceConfig config;

    public OpenVinoLayoutLMService() {

        this.webClient = WebClient.builder().build();
        this.objectMapper = new ObjectMapper();
    }

    public List<String> extractTextFromPdf(byte[] pdfBytes) {

        try {
            // Convert PDF to images
            List<BufferedImage> images = convertPdfToImages(pdfBytes);

            List<String> extractedTexts = new ArrayList<>();

            for (BufferedImage image : images) {
                // Process each page
                String pageText = processImageWithLayoutLM(image);
                extractedTexts.add(pageText);
            }

            return extractedTexts;
        } catch (Exception e) {
            throw new RuntimeException("Failed to extract text from PDF using LayoutLM", e);
        }
    }

    private List<BufferedImage> convertPdfToImages(byte[] pdfBytes) throws IOException {

        List<BufferedImage> images = new ArrayList<>();

        try (PDDocument document = Loader.loadPDF(pdfBytes)) {
            PDFRenderer renderer = new PDFRenderer(document);

            for (int page = 0; page < document.getNumberOfPages(); page++) {
                BufferedImage image = renderer.renderImageWithDPI(page, 150);
                images.add(image);
            }
        }

        return images;
    }

    private String processImageWithLayoutLM(BufferedImage image) {

        try {
            // Prepare image data for OpenVINO model server
            Map<String, Object> request = prepareInferenceRequest(image);

            // Call OpenVINO model server
            String response = webClient.post()
                    .uri(config.getOpenvinoEndpoint() + "/v1/models/layoutlmv3:predict")
                    .header("Content-Type", "application/json")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            // Parse response and extract text
            return parseLayoutLMResponse(response);
        } catch (Exception e) {
            // Fallback to simple OCR or return empty
            return "";
        }
    }

    private Map<String, Object> prepareInferenceRequest(BufferedImage image) {
        // Convert image to base64 or tensor format expected by OpenVINO
        // This is a simplified version - you'll need to implement proper preprocessing

        Map<String, Object> request = new HashMap<>();
        Map<String, Object> inputs = new HashMap<>();

        // Preprocess image (resize, normalize, etc.)
        float[][][] imageArray = preprocessImage(image);

        inputs.put("image", imageArray);
        request.put("inputs", inputs);

        return request;
    }

    private float[][][] preprocessImage(BufferedImage image) {
        // Implement image preprocessing for LayoutLMv3
        // Resize to model input size (typically 224x224 or 384x384)
        int targetWidth = 384;
        int targetHeight = 384;

        float[][][] processedImage = new float[3][targetHeight][targetWidth];

        // Resize and normalize image
        BufferedImage resized = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);
        resized.getGraphics().drawImage(image.getScaledInstance(targetWidth, targetHeight,
                BufferedImage.SCALE_SMOOTH), 0, 0, null);

        // Convert to normalized float array (RGB channels)
        for (int y = 0; y < targetHeight; y++) {
            for (int x = 0; x < targetWidth; x++) {
                int rgb = resized.getRGB(x, y);

                // Extract RGB values and normalize to [0, 1]
                processedImage[0][y][x] = ((rgb >> 16) & 0xFF) / 255.0f; // R
                processedImage[1][y][x] = ((rgb >> 8) & 0xFF) / 255.0f;  // G
                processedImage[2][y][x] = (rgb & 0xFF) / 255.0f;         // B
            }
        }

        return processedImage;
    }

    private String parseLayoutLMResponse(String response) {

        try {
            JsonNode jsonResponse = objectMapper.readTree(response);
            JsonNode outputs = jsonResponse.path("outputs");

            // Parse the model output to extract text
            // This depends on the specific LayoutLMv3 model output format
            StringBuilder extractedText = new StringBuilder();

            // Simplified parsing - you'll need to implement based on actual model output
            if (outputs.has("text_predictions")) {
                JsonNode textPredictions = outputs.path("text_predictions");
                for (JsonNode prediction : textPredictions) {
                    extractedText.append(prediction.asText()).append(" ");
                }
            }

            return extractedText.toString().trim();
        } catch (Exception e) {
            return "";
        }
    }

}
