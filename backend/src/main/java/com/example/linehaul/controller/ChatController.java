package com.example.linehaul.controller;

import com.example.linehaul.dto.ChatRequest;
import com.example.linehaul.dto.ChatResponse;
import com.example.linehaul.service.ChatService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private static final Logger log = LoggerFactory.getLogger(ChatController.class);

    private static final String DATA_PROBLEM =
            "Sorry, I couldn't retrieve the current Linehaul data. Please try again.";

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping
    public ChatResponse ask(@RequestBody(required = false) ChatRequest request) {
        String message = request == null ? "" : request.getMessage();
        try {
            return new ChatResponse(chatService.answer(message));
        } catch (Exception exception) {
            log.warn("Chat question could not be answered: {}", exception.getMessage());
            return new ChatResponse(DATA_PROBLEM);
        }
    }
}
