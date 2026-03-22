package com.chatbot.controller;

import com.chatbot.dto.ChatRequest;
import com.chatbot.model.Chat;
import com.chatbot.service.ChatService;
import com.chatbot.service.TokenService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ChatController — handles sending messages and fetching chat history.
 *
 * Endpoints:
 *   POST /api/chat/send     — send a message and get AI reply
 *   GET  /api/chat/history  — get user's chat history
 */
@RestController
@RequestMapping("/api/chat")
public class ChatController {

    @Autowired
    private ChatService chatService;

    @Autowired
    private TokenService tokenService;

    /**
     * Send a message to the chatbot.
     *
     * Request body: { "message": "Hello", "mode": "NORMAL" }
     * Response: { "success": true, "reply": "...", "tokens": 9 }
     *
     * Valid modes: NORMAL, FANTASY, COMPANION, FLIRT
     */
    @PostMapping("/send")
    public ResponseEntity<Map<String, Object>> sendMessage(
            @RequestBody ChatRequest request,
            Authentication auth) {

        String userId = (String) auth.getPrincipal();

        // Validate input
        if (request.getMessage() == null || request.getMessage().isBlank()) {
            Map<String, Object> err = new HashMap<>();
            err.put("success", false);
            err.put("error", "Message cannot be empty");
            return ResponseEntity.badRequest().body(err);
        }

        // Default to NORMAL mode if not specified
        String mode = (request.getMode() != null && !request.getMode().isBlank())
                ? request.getMode().toUpperCase()
                : "NORMAL";

        // Validate mode
        if (!List.of("NORMAL", "FANTASY", "COMPANION", "FLIRT").contains(mode)) {
            Map<String, Object> err = new HashMap<>();
            err.put("success", false);
            err.put("error", "Invalid mode. Choose: NORMAL, FANTASY, COMPANION, FLIRT");
            return ResponseEntity.badRequest().body(err);
        }

        // Process the message through ChatService
        Map<String, Object> result = chatService.sendMessage(userId, mode, request.getMessage().trim());

        // Return 402 (Payment Required) if out of tokens
        if (!(boolean) result.get("success") && result.containsKey("error")
                && result.get("error").toString().contains("Insufficient")) {
            return ResponseEntity.status(402).body(result);
        }

        return result.containsKey("error")
                ? ResponseEntity.status(500).body(result)
                : ResponseEntity.ok(result);
    }

    /**
     * Get chat history for the logged-in user.
     * Optional query param: mode (filter by mode)
     *
     * GET /api/chat/history?mode=NORMAL
     */
    @GetMapping("/history")
    public ResponseEntity<Map<String, Object>> getChatHistory(
            @RequestParam(required = false) String mode,
            Authentication auth) {

        String userId = (String) auth.getPrincipal();
        List<Chat> chats = chatService.getChatHistory(userId, mode);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("chats", chats);
        response.put("count", chats.size());
        response.put("tokens", tokenService.getBalance(userId));
        return ResponseEntity.ok(response);
    }
}

