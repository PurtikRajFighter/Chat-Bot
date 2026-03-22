package com.chatbot;

import com.chatbot.config.DiscountConfig;
import com.chatbot.config.PackageConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * ChatBotApplication — Spring Boot entry point.
 *
 * Run this class to start the local chatbot server.
 * Server starts on http://localhost:8080
 *
 * Make sure Ollama is running if using local AI:
 *   ollama serve
 *   ollama pull llama3.2
 */
@SpringBootApplication
@EnableConfigurationProperties({PackageConfig.class, DiscountConfig.class})
public class ChatBotApplication {
    public static void main(String[] args) {
        SpringApplication.run(ChatBotApplication.class, args);
    }
}

