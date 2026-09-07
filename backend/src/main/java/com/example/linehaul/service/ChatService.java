package com.example.linehaul.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Answers free-form questions about the current Linehaul data.
 *
 * There is no intent parsing or entity/pattern matching here on purpose:
 * every question, however it is phrased, is answered the same way -
 * 1) pull a fresh snapshot of the current routes/orders/drivers/vehicles
 *    from the database (see {@link LinehaulContextBuilder}),
 * 2) hand that snapshot plus the user's question to an AI model (Groq, see
 *    {@link GroqClient}) with instructions to answer only from that data,
 * 3) return whatever the model says.
 */
@Service
public class ChatService {

    private static final Logger log = LoggerFactory.getLogger(ChatService.class);

    private static final String HELP = """
            I can help you with information about orders, routes, drivers and vehicles.

            Try asking:
            • How many routes are there?
            • Show available drivers.
            • How many vehicles are available?
            • How many READY orders are there?
            • What is the status of LH-1029?""";

    private static final String SYSTEM_PROMPT_TEMPLATE = 
            """
     You are the AI assistant for a Linehaul Management System.

        Your job is to answer user questions using the application's
        available Linehaul API tools.

        IMPORTANT RULES:

        1. Use the API tools when the user asks for current
           Linehaul data.

        2. Do not invent data.

        3. Do not make assumptions about data that was not
           returned by an API.

        4. You may call multiple tools when the question
           requires information from multiple APIs.

        5. After receiving API results, analyze those results
           and answer the user's question.

        6. For counting questions, count the records returned
           by the API.

        7. For comparison questions, compare the values
           returned by the API.

        8. For questions such as:
           "How many routes are blocked?"
           retrieve the routes and determine the answer
           from the returned route data.

        9. For questions about a specific route, order,
           driver or vehicle, use the corresponding API.

        10. You are read-only.
            Never create, update, delete or dispatch anything.

        11. Keep answers clear and easy to understand.

        12. Do not mention internal implementation details
            unless the user asks about them.

                
                """;

    private final LinehaulContextBuilder contextBuilder;
    private final GroqClient groqClient;

    public ChatService(LinehaulContextBuilder contextBuilder, GroqClient groqClient) {
        this.contextBuilder = contextBuilder;
        this.groqClient = groqClient;
    }

    public String answer(String question) {
        String trimmed = question == null ? "" : question.trim();
        if (trimmed.isEmpty()) {
            return HELP;
        }

        String context = contextBuilder.build();
        String systemPrompt = SYSTEM_PROMPT_TEMPLATE.formatted(context);

        log.debug("Asking Groq: {}", trimmed);
        return groqClient.chat(systemPrompt, trimmed);
    }
}
