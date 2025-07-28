package com.redhat.chatbot.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ModelInferenceConfig {

    @Value("${rag.model.layoutlm.path:/models/layoutlmv3.onnx}")
    private String layoutLmModelPath;

    @Value("${rag.model.layoutlm.tokenizer-path:/models/layoutlmv3-tokenizer}")
    private String tokenizerPath;

    @Value("${rag.model.device:CPU}")
    private String device;

    @Value("${rag.model.openvino.endpoint:http://openvino-service:8080}")
    private String openvinoEndpoint;

    // Getters
    public String getLayoutLmModelPath() {

        return layoutLmModelPath;
    }

    public String getTokenizerPath() {

        return tokenizerPath;
    }

    public String getDevice() {

        return device;
    }

    public String getOpenvinoEndpoint() {

        return openvinoEndpoint;
    }

}
