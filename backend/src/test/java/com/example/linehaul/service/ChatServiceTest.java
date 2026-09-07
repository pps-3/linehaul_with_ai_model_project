package com.example.linehaul.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChatServiceTest {

    @Test
    void emptyQuestionReturnsHelpWithoutCallingGroq() {
        LinehaulContextBuilder contextBuilder = new LinehaulContextBuilder(null, null, null, null) {
            @Override
            public String build() {
                throw new AssertionError("context should not be built for an empty question");
            }
        };
        GroqClient groqClient = new GroqClient("key", "http://unused", "model", 0.2, 700, 5000, 20000) {
            @Override
            public String chat(String systemPrompt, String userPrompt) {
                throw new AssertionError("Groq should not be called for an empty question");
            }
        };

        ChatService chatService = new ChatService(contextBuilder, groqClient);

        String answer = chatService.answer("   ");

        assertTrue(answer.contains("I can help you with information"));
    }

    @Test
    void forwardsTheDataSnapshotAndQuestionToGroq() {
        LinehaulContextBuilder contextBuilder = new LinehaulContextBuilder(null, null, null, null) {
            @Override
            public String build() {
                return "ROUTES (1 total)\nLH-1 | READY | ...";
            }
        };
        GroqClient groqClient = new GroqClient("key", "http://unused", "model", 0.2, 700, 5000, 20000) {
            @Override
            public String chat(String systemPrompt, String userPrompt) {
                assertTrue(systemPrompt.contains("ROUTES (1 total)"), "system prompt should include the data snapshot");
                assertEquals("How many routes are there?", userPrompt);
                return "There is 1 route.";
            }
        };

        ChatService chatService = new ChatService(contextBuilder, groqClient);

        String answer = chatService.answer("How many routes are there?");

        assertEquals("There is 1 route.", answer);
    }

    @Test
    void propagatesGroqFailuresSoTheControllerCanReportAFriendlyMessage() {
        LinehaulContextBuilder contextBuilder = new LinehaulContextBuilder(null, null, null, null) {
            @Override
            public String build() {
                return "ROUTES (0 total)\n";
            }
        };
        GroqClient groqClient = new GroqClient("key", "http://unused", "model", 0.2, 700, 5000, 20000) {
            @Override
            public String chat(String systemPrompt, String userPrompt) {
                throw new IllegalStateException("Groq returned an unexpected response.");
            }
        };

        ChatService chatService = new ChatService(contextBuilder, groqClient);

        assertThrows(IllegalStateException.class, () -> chatService.answer("How many routes are there?"));
    }
}
