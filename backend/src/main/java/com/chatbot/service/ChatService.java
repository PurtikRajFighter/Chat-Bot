package com.chatbot.service;

import com.chatbot.model.Chat;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.util.*;

/**
 * ChatService — the brain of the chatbot.
 *
 * Handles:
 * - System prompt selection per mode
 * - Building the conversation history for the AI
 * - Calling the AI API (OpenAI or Ollama compatible)
 * - Saving messages to chats.xlsx
 */
@Service
public class ChatService {

    @Autowired
    private ExcelService excelService;

    @Autowired
    private TokenService tokenService;

    @Value("${ai.api.url}")
    private String aiApiUrl;

    @Value("${ai.api.key:}")
    private String aiApiKey;

    @Value("${ai.model}")
    private String aiModel;

    // Reusable HTTP client
    private final HttpClient httpClient = HttpClient.newHttpClient();

    // =========================================================
    // SYSTEM PROMPTS PER MODE
    // =========================================================

    /**
     * Returns the system prompt for the given chat mode.
     * Each mode gives the AI a different persona.
     */
    private String getSystemPrompt(String mode) {
        return switch (mode.toUpperCase()) {
            case "FANTASY" -> """
                    You are a creative storyteller and fantasy narrator. Your role is to weave immersive,
                    imaginative fantasy stories and adventures with the user. Use rich descriptions,
                    vivid world-building, and engaging characters. Always keep the story going and invite
                    the user to participate in the narrative.
                    """;
            case "COMPANION" -> """
                    You are a warm, empathetic, and supportive companion. Your role is to listen actively,
                    provide emotional support, and be a friendly presence. Show genuine interest in the user's
                    day, feelings, and thoughts. Be caring, non-judgmental, and uplifting. Ask thoughtful
                    follow-up questions.
                    """;
            case "FLIRT" -> """
                    You are a charming and playful conversationalist with a light romantic tone.
                    Be witty, complimentary, and fun. Keep the conversation light-hearted and tasteful.
                    Use gentle, non-explicit flirty banter and compliments. Maintain respectful boundaries
                    at all times. No explicit or adult content.
                    """;
            default -> // NORMAL mode
                    """
                    You are a helpful, friendly, and knowledgeable AI assistant. Answer questions clearly
                    and concisely. Be honest about what you know and don't know. Help the user with tasks,
                    explanations, coding, writing, analysis, and general questions.
                    """;
        };
    }

    // =========================================================
    // MAIN CHAT METHOD
    // =========================================================

    /**
     * Processes a user message:
     * 1. Validates tokens
     * 2. Deducts token
     * 3. Saves user message to Excel
     * 4. Calls AI API
     * 5. Saves AI response to Excel
     * 6. Returns the AI reply
     *
     * @param userId  ID of the user sending the message
     * @param mode    Chat mode (NORMAL, FANTASY, COMPANION, FLIRT)
     * @param message The user's message text
     * @return The AI's reply text
     */
    public Map<String, Object> sendMessage(String userId, String mode, String message) {
        Map<String, Object> result = new HashMap<>();

        // 1. Check token balance
        if (!tokenService.hasTokens(userId)) {
            result.put("success", false);
            result.put("error", "Insufficient tokens. Please purchase more tokens to continue.");
            result.put("tokens", 0);
            return result;
        }

        // 2. Deduct token BEFORE making AI call (prevents gaming the system)
        tokenService.deductToken(userId);

        // 3. Save user message to chats.xlsx
        Chat userChat = new Chat(
                UUID.randomUUID().toString(),
                userId, mode, message, "user", LocalDateTime.now()
        );
        excelService.saveChat(userChat);

        // 4. Load recent chat history to send as context (last 10 messages)
        List<Chat> history = excelService.getChatsByUser(userId, mode);
        int startIdx = Math.max(0, history.size() - 11); // 10 previous + current
        List<Chat> recentHistory = history.subList(startIdx, history.size());

        // 5. Build the messages array for the AI API
        String aiReply;
        try {
            aiReply = callAiApi(mode, recentHistory);
        } catch (Exception e) {
            // If AI fails, refund the token
            tokenService.addTokens(userId, 1);
            // Re-delete the user message (or just return error)
            result.put("success", false);
            result.put("error", "AI service unavailable: " + e.getMessage());
            return result;
        }

        // 6. Save AI response to chats.xlsx
        Chat assistantChat = new Chat(
                UUID.randomUUID().toString(),
                userId, mode, aiReply, "assistant", LocalDateTime.now()
        );
        excelService.saveChat(assistantChat);

        // 7. Return success response
        result.put("success", true);
        result.put("reply", aiReply);
        result.put("tokens", tokenService.getBalance(userId));
        result.put("mode", mode);
        return result;
    }

    /**
     * Retrieves chat history for a user.
     */
    public List<Chat> getChatHistory(String userId, String mode) {
        return excelService.getChatsByUser(userId, mode);
    }

    // =========================================================
    // AI API CALL (OpenAI-compatible format)
    // =========================================================

    /**
     * Calls the AI API with the conversation history.
     * Supports both OpenAI and Ollama (both use the same /v1/chat/completions format).
     */
    private String callAiApi(String mode, List<Chat> history) throws IOException, InterruptedException {
        // Build the messages JSON array
        StringBuilder messages = new StringBuilder("[");

        // Add system prompt first
        messages.append("{\"role\":\"system\",\"content\":")
                .append(jsonString(getSystemPrompt(mode)))
                .append("}");

        // Add conversation history
        for (Chat chat : history) {
            messages.append(",{\"role\":\"")
                    .append(chat.getRole())
                    .append("\",\"content\":")
                    .append(jsonString(chat.getMessage()))
                    .append("}");
        }
        messages.append("]");

        // Build request body
        String requestBody = String.format(
                "{\"model\":\"%s\",\"messages\":%s,\"temperature\":0.8,\"max_tokens\":1000}",
                aiModel, messages
        );

        // Build HTTP request
        HttpRequest.Builder reqBuilder = HttpRequest.newBuilder()
                .uri(URI.create(aiApiUrl))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody));

        // Add auth header only if API key is configured (not needed for Ollama)
        if (aiApiKey != null && !aiApiKey.isBlank()) {
            reqBuilder.header("Authorization", "Bearer " + aiApiKey);
        }

        HttpResponse<String> response = httpClient.send(reqBuilder.build(), HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new IOException("AI API returned status " + response.statusCode() + ": " + response.body());
        }

        // Parse the response — extract content from choices[0].message.content
        return parseAiResponse(response.body());
    }

    /**
     * Simple JSON parser to extract the AI reply from the API response.
     * Avoids adding a full JSON library dependency.
     */
    private String parseAiResponse(String json) {
        // Look for "content":"..." in the response
        String marker = "\"content\":";
        // Skip the system message content (first occurrence could be in the request echo for some APIs)
        // Find the last "content" key which will be the assistant's message
        int idx = json.lastIndexOf(marker);
        if (idx == -1) throw new RuntimeException("Unexpected AI response format: " + json);

        int start = json.indexOf("\"", idx + marker.length()) + 1;
        // Handle content: null case
        if (json.substring(idx + marker.length()).trim().startsWith("null")) {
            return "I'm sorry, I couldn't generate a response. Please try again.";
        }

        StringBuilder content = new StringBuilder();
        boolean escaped = false;
        for (int i = start; i < json.length(); i++) {
            char c = json.charAt(i);
            if (escaped) {
                switch (c) {
                    case 'n': content.append('\n'); break;
                    case 't': content.append('\t'); break;
                    case '"': content.append('"'); break;
                    case '\\': content.append('\\'); break;
                    default: content.append(c);
                }
                escaped = false;
            } else if (c == '\\') {
                escaped = true;
            } else if (c == '"') {
                break; // end of string
            } else {
                content.append(c);
            }
        }
        return content.toString().trim();
    }

    /**
     * Escapes a string for safe JSON embedding.
     */
    private String jsonString(String value) {
        if (value == null) return "\"\"";
        String escaped = value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
        return "\"" + escaped + "\"";
    }
}

