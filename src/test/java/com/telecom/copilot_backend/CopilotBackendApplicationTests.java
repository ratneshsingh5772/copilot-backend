package com.telecom.copilot_backend;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
        // H2 in-memory database — MySQL compatibility mode for syntax support
        "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;MODE=MySQL;NON_KEYWORDS=VALUE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        // Use H2Dialect — avoids MySQLDialect version warnings
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        // 'create' skips the DROP phase (H2 doesn't support MySQL FK drop syntax)
        "spring.jpa.hibernate.ddl-auto=create",
        "spring.jpa.open-in-view=false",
        // Disable Vertex AI auto-configuration — replaced by mock ChatClient below
        "spring.ai.vertex.ai.gemini.project-id=test-project",
        "spring.ai.vertex.ai.gemini.location=us-central1",
        "spring.ai.vertex.ai.gemini.chat.options.model=gemini-1.5-flash",
        // Exclude the real Vertex AI auto-config — no GCP credentials needed
        "spring.autoconfigure.exclude=org.springframework.ai.autoconfigure.vertexai.gemini.VertexAiGeminiAutoConfiguration"
})
class CopilotBackendApplicationTests {

    /**
     * @MockBean replaces both the AiConfig-produced ChatClient AND the
     * VertexAiGeminiChatModel it depends on, so no real GCP client is
     * created and no "credentials not found" shutdown warning is emitted.
     */
    @MockBean
    ChatClient chatClient;

    @Test
    void contextLoads() {
    }
}
