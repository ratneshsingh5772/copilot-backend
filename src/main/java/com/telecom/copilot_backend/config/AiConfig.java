package com.telecom.copilot_backend.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.vertexai.gemini.VertexAiGeminiChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiConfig {

    /**
     * Builds a {@link ChatClient} backed by Vertex AI Gemini.
     * Connection details (project, location, model) are configured via
     * {@code application.properties}.
     */
    @Bean
    public ChatClient chatClient(VertexAiGeminiChatModel geminiChatModel) {
        return ChatClient.builder(geminiChatModel).build();
    }
}

