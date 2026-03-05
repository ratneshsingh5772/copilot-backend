package com.telecom.copilot_backend;

import com.telecom.copilot_backend.service.GeminiRestClient;
import org.junit.jupiter.api.Test;
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
        "spring.ai.vertex.ai.gemini.chat.options.model=gemini-2.0-flash-lite",
        // Exclude the real Vertex AI auto-config — no GCP credentials needed
        "spring.autoconfigure.exclude=org.springframework.ai.autoconfigure.vertexai.gemini.VertexAiGeminiAutoConfiguration",
        "gemini.api.key=test-key",
        "gemini.api.model=gemini-2.0-flash-lite"
})
class CopilotBackendApplicationTests {

    /** MockBean prevents GeminiRestClient from making real HTTP calls during context load */
    @MockBean
    GeminiRestClient geminiRestClient;

    @Test
    void contextLoads() {
    }
}
