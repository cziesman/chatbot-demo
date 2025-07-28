package com.redhat.chatbot.service;

import java.awt.image.BufferedImage;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OnnxValue;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtException;
import ai.onnxruntime.OrtSession;
import com.redhat.chatbot.config.ModelInferenceConfig;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class OnnxLayoutLMService {

    @Autowired
    private ModelInferenceConfig config;

    private OrtEnvironment environment;

    private OrtSession session;

    @PostConstruct
    public void initialize() {

        try {
            environment = OrtEnvironment.getEnvironment();

            // Load ONNX model
            OrtSession.SessionOptions options = new OrtSession.SessionOptions();
            options.setOptimizationLevel(OrtSession.SessionOptions.OptLevel.BASIC_OPT);

            session = environment.createSession(config.getLayoutLmModelPath(), options);
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize ONNX LayoutLM model", e);
        }
    }

    @PreDestroy
    public void cleanup() {

        try {
            if (session != null) {
                session.close();
            }
            if (environment != null) {
                environment.close();
            }
        } catch (Exception e) {
            // Log error but don't throw
        }
    }

    public List<String> extractTextFromPdf(byte[] pdfBytes) {

        try {
            List<BufferedImage> images = convertPdfToImages(pdfBytes);
            List<String> extractedTexts = new ArrayList<>();

            for (BufferedImage image : images) {
                String pageText = processImageWithOnnx(image);
                extractedTexts.add(pageText);
            }

            return extractedTexts;
        } catch (Exception e) {
            throw new RuntimeException("Failed to extract text from PDF using ONNX LayoutLM", e);
        }
    }

    private List<BufferedImage> convertPdfToImages(byte[] pdfBytes) throws Exception {

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

    private String processImageWithOnnx(BufferedImage image) throws OrtException {

        // Preprocess image
        float[][][][] imageArray = preprocessImageForOnnx(image);

        // Create input tensor
        long[] shape = {1, 3, 384, 384}; // Batch, Channels, Height, Width
        FloatBuffer buffer = FloatBuffer.allocate((int) (shape[0] * shape[1] * shape[2] * shape[3]));

        // Flatten image array into buffer
        for (int c = 0; c < 3; c++) {
            for (int h = 0; h < 384; h++) {
                for (int w = 0; w < 384; w++) {
                    buffer.put(imageArray[0][c][h][w]);
                }
            }
        }
        buffer.rewind();

        OnnxTensor inputTensor = OnnxTensor.createTensor(environment, buffer, shape);

        // Run inference
        Map<String, OnnxTensor> inputs = Map.of("image", inputTensor);
        OrtSession.Result result = session.run(inputs);

        // Process output
        String extractedText = processOnnxOutput(result);

        // Cleanup
        inputTensor.close();
        result.close();

        return extractedText;
    }

    private float[][][][] preprocessImageForOnnx(BufferedImage image) {

        int targetWidth = 384;
        int targetHeight = 384;

        float[][][][] processedImage = new float[1][3][targetHeight][targetWidth];

        // Resize image
        BufferedImage resized = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);
        resized.getGraphics().drawImage(image.getScaledInstance(targetWidth, targetHeight,
                BufferedImage.SCALE_SMOOTH), 0, 0, null);

        // Convert to normalized float array
        for (int y = 0; y < targetHeight; y++) {
            for (int x = 0; x < targetWidth; x++) {
                int rgb = resized.getRGB(x, y);

                // ImageNet normalization
                processedImage[0][0][y][x] = (((rgb >> 16) & 0xFF) / 255.0f - 0.485f) / 0.229f; // R
                processedImage[0][1][y][x] = (((rgb >> 8) & 0xFF) / 255.0f - 0.456f) / 0.224f;  // G
                processedImage[0][2][y][x] = ((rgb & 0xFF) / 255.0f - 0.406f) / 0.225f;         // B
            }
        }

        return processedImage;
    }

    private String processOnnxOutput(OrtSession.Result result) throws OrtException {

        // Process model output to extract text
        // This depends on the specific LayoutLMv3 ONNX model output format

        StringBuilder extractedText = new StringBuilder();

        // Get output tensors
        for (Map.Entry<String, OnnxValue> entry : result) {
            if (entry.getValue() instanceof OnnxTensor tensor) {

                // Process tensor data based on output type
                // This is a simplified version - implement based on actual model
                if ("text_output".equals(entry.getKey())) {
                    float[][] textData = (float[][]) tensor.getValue();
                    // Convert predictions to text
                    extractedText.append(decodeTextPredictions(textData));
                }
            }
        }

        return extractedText.toString();
    }

    private String decodeTextPredictions(float[][] predictions) {

        // Implement text decoding logic based on your model's output format
        // This is a placeholder implementation
        return "Extracted text from LayoutLMv3 ONNX model";
    }

}
