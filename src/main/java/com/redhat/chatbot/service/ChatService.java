package com.redhat.chatbot.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.redhat.chatbot.model.ChatMessage;
import com.redhat.chatbot.model.SearchResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
public class ChatService {

    private final WebClient webClient;

    private final ObjectMapper objectMapper;

    @Autowired
    private EmbeddingService embeddingService;

    @Autowired
    private VectorStoreService vectorStoreService;

    @Value("${rag.granite.endpoint}")
    private String graniteEndpoint;

    @Value("${rag.granite.model-name}")
    private String modelName;

    public ChatService() {

        this.webClient = WebClient.builder().build();
        this.objectMapper = new ObjectMapper();
    }

    public ChatMessage processMessage(String userMessage) {

        try {
            // Generate embedding for user query
            float[] queryEmbedding = embeddingService.generateEmbedding(userMessage);

            // Search for relevant documents
            List<SearchResult> searchResults = vectorStoreService.searchSimilar(queryEmbedding, 5);

            // Build context from search results
            StringBuilder context = new StringBuilder();
            for (SearchResult result : searchResults) {
                context.append("Document: ").append(result.getFilename())
                        .append("\nContent: ").append(result.getContent())
                        .append("\n\n");
            }

            // Create prompt with context
            String prompt = buildPrompt(userMessage, context.toString());

            // Call Granite model
            String response = callGraniteModel(prompt);

            return new ChatMessage(response, "assistant");
        } catch (Exception e) {
            return new ChatMessage("Sorry, I encountered an error processing your request: " + e.getMessage(), "assistant");
        }
    }

    private String buildPrompt(String userMessage, String context) {

        return String.format(
                """
                        Context information:
                        %s
                        
                        Based on the context above, please answer the following question:
                        %s
                        
                        If the context doesn't contain relevant information, please say so.""",
                context, userMessage
        );
    }

    private String callGraniteModel(String prompt) {

        try {
            Map<String, Object> request = new HashMap<>();
            request.put("model", modelName);
            request.put("messages", List.of(
                    Map.of("role", "user", "content", prompt)
            ));
            request.put("max_tokens", 500);
            request.put("temperature", 0.7);

            String response = webClient.post()
                    .uri(graniteEndpoint)
                    .header("Content-Type", "application/json")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            JsonNode jsonResponse = objectMapper.readTree(response);
            return jsonResponse.path("choices").get(0).path("message").path("content").asText();
        } catch (Exception e) {
            throw new RuntimeException("Failed to call Granite model", e);
        }
    }

}
