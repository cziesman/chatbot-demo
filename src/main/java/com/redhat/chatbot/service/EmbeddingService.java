package com.redhat.chatbot.service;

import java.util.Random;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class EmbeddingService {

    private final Random random = new Random();

    @Value("${rag.embedding.model-name}")
    private String modelName;

    @Value("${rag.qdrant.vector-size}")
    private int vectorSize;

    public float[] generateEmbedding(String text) {
        // TODO: Implement actual embedding generation
        // This could use a local model or call an external service
        // For now, generating random embeddings as placeholder

        float[] embedding = new float[vectorSize];
        for (int i = 0; i < vectorSize; i++) {
            embedding[i] = (float) (random.nextGaussian() * 0.1f);
        }

        // Normalize the embedding
        float norm = 0;
        for (float value : embedding) {
            norm += value * value;
        }
        norm = (float) Math.sqrt(norm);

        for (int i = 0; i < embedding.length; i++) {
            embedding[i] /= norm;
        }

        return embedding;
    }

    public float[] generateEmbeddings(String[] texts) {
        // Batch processing for multiple texts
        float[] allEmbeddings = new float[texts.length * vectorSize];

        for (int i = 0; i < texts.length; i++) {
            float[] embedding = generateEmbedding(texts[i]);
            System.arraycopy(embedding, 0, allEmbeddings, i * vectorSize, vectorSize);
        }

        return allEmbeddings;
    }

}
