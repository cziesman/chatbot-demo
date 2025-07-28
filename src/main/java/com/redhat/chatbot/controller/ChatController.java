package com.redhat.chatbot.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import com.redhat.chatbot.model.ChatMessage;
import com.redhat.chatbot.service.ChatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Controller
public class ChatController {

    private final List<ChatMessage> chatHistory = new ArrayList<>();

    @Autowired
    private ChatService chatService;

    @GetMapping("/")
    public String chatPage() {

        return "chat";
    }

    @PostMapping("/api/chat")
    @ResponseBody
    public ChatMessage sendMessage(@RequestBody ChatMessage userMessage) {
        // Add user message to history
        userMessage.setRole("user");
        chatHistory.add(userMessage);

        // Process message and get response
        ChatMessage response = chatService.processMessage(userMessage.getContent());
        chatHistory.add(response);

        return response;
    }

    @GetMapping("/api/chat/history")
    @ResponseBody
    public List<ChatMessage> getChatHistory() {

        return new ArrayList<>(chatHistory);
    }

    @GetMapping("/api/chat/stream")
    public SseEmitter streamChat(@RequestParam String message) {

        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);

        CompletableFuture.runAsync(() -> {
            try {
                ChatMessage userMessage = new ChatMessage(message, "user");
                chatHistory.add(userMessage);

                ChatMessage response = chatService.processMessage(message);
                chatHistory.add(response);

                emitter.send(SseEmitter.event()
                        .name("message")
                        .data(response));

                emitter.complete();
            } catch (Exception e) {
                emitter.completeWithError(e);
            }
        });

        return emitter;
    }

}
