package com.redhat.chatbot.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.redhat.chatbot.model.SearchResult;
import io.qdrant.client.QdrantClient;
import io.qdrant.client.grpc.Collections;
import io.qdrant.client.grpc.Collections.CreateCollection;
import io.qdrant.client.grpc.Collections.Distance;
import io.qdrant.client.grpc.Collections.VectorParams;
import io.qdrant.client.grpc.Collections.VectorsConfig;
import io.qdrant.client.grpc.JsonWithInt;
import io.qdrant.client.grpc.Points.PointId;
import io.qdrant.client.grpc.Points.PointStruct;
import io.qdrant.client.grpc.Points.ScoredPoint;
import io.qdrant.client.grpc.Points.SearchPoints;
import io.qdrant.client.grpc.Points.UpsertPoints;
import io.qdrant.client.grpc.Points.Vector;
import io.qdrant.client.grpc.Points.Vectors;
import io.qdrant.client.grpc.Points.WithPayloadSelector;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class VectorStoreService {

    @Autowired
    private QdrantClient qdrantClient;

    @Value("${rag.qdrant.collection-name}")
    private String collectionName;

    @Value("${rag.qdrant.vector-size}")
    private int vectorSize;

    @PostConstruct
    public void initializeCollection() {

        try {
            // Check if collection exists
            Collections.CollectionInfo response = qdrantClient.getCollectionInfoAsync(collectionName).get();
        } catch (Exception e) {
            // Collection doesn't exist, create it
            createCollection();
        }
    }

    private void createCollection() {

        try {
            VectorParams vectorParams = VectorParams.newBuilder()
                    .setSize(vectorSize)
                    .setDistance(Distance.Cosine)
                    .build();

            CreateCollection createCollection = CreateCollection.newBuilder()
                    .setCollectionName(collectionName)
                    .setVectorsConfig(VectorsConfig.newBuilder()
                            .setParams(vectorParams)
                            .build())
                    .build();

            qdrantClient.createCollectionAsync(createCollection).get();
        } catch (Exception e) {
            throw new RuntimeException("Failed to create Qdrant collection", e);
        }
    }

    public void storeVector(String id, float[] vector, String text, Map<String, Object> metadata) {

        try {
            PointStruct.Builder pointBuilder = PointStruct.newBuilder()
                    .setId(PointId.newBuilder().setUuid(id).build())
                    .setVectors(Vectors.newBuilder().setVector(
                                    Vector.newBuilder()
                                            .addAllData(floatArrayToList(vector))
                                            .build())
                            .build());

            // Add metadata
            pointBuilder.putPayload("text", JsonWithInt.Value.newBuilder().setStringValue(text).build());
            for (Map.Entry<String, Object> entry : metadata.entrySet()) {
                JsonWithInt.Value.Builder valueBuilder = JsonWithInt.Value.newBuilder();
                if (entry.getValue() instanceof String) {
                    valueBuilder.setStringValue((String) entry.getValue());
                } else if (entry.getValue() instanceof Integer) {
                    valueBuilder.setIntegerValue((Integer) entry.getValue());
                } else if (entry.getValue() instanceof Long) {
                    valueBuilder.setIntegerValue((Long) entry.getValue());
                } else if (entry.getValue() instanceof Double) {
                    valueBuilder.setDoubleValue((Double) entry.getValue());
                }
                pointBuilder.putPayload(entry.getKey(), valueBuilder.build());
            }

            UpsertPoints upsertPoints = UpsertPoints.newBuilder()
                    .setCollectionName(collectionName)
                    .addPoints(pointBuilder.build())
                    .build();

            qdrantClient.upsertAsync(upsertPoints).get();
        } catch (Exception e) {
            throw new RuntimeException("Failed to store vector", e);
        }
    }

    public List<SearchResult> searchSimilar(float[] queryVector, int limit) {

        try {
            SearchPoints searchPoints = SearchPoints.newBuilder()
                    .setCollectionName(collectionName)
                    .addAllVector(floatArrayToList(queryVector))
                    .setLimit(limit)
                    .setWithPayload(WithPayloadSelector.newBuilder().setEnable(true).build())
                    .build();

            List<ScoredPoint> response = qdrantClient.searchAsync(searchPoints).get();

            List<SearchResult> results = new ArrayList<>();
            for (ScoredPoint point : response) {
                String text = point.getPayloadMap().get("text").getStringValue();
                String filename = point.getPayloadMap().get("filename").getStringValue();
                String documentId = point.getPayloadMap().get("document_id").getStringValue();

                results.add(new SearchResult(text, point.getScore(), documentId, filename));
            }

            return results;
        } catch (Exception e) {
            throw new RuntimeException("Failed to search vectors", e);
        }
    }

    private List<Float> floatArrayToList(float[] array) {

        List<Float> list = new ArrayList<>();
        for (float value : array) {
            list.add(value);
        }
        return list;
    }

}
