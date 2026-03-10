package com.telecom.copilot_backend;

import com.telecom.copilot_backend.service.OllamaRestClient;
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
        // Ollama config — OllamaRestClient is mocked so no real Ollama server needed
        "ollama.base-url=http://localhost:11434",
        "ollama.model=llama3.2",
        // Exclude Vertex AI auto-config
        "spring.autoconfigure.exclude=org.springframework.ai.autoconfigure.vertexai.gemini.VertexAiGeminiAutoConfiguration"
})
class CopilotBackendApplicationTests {

    /** MockBean prevents OllamaRestClient from making real HTTP calls during context load */
    @MockBean
    OllamaRestClient ollamaRestClient;

    @Test
    void contextLoads() {
    }
}


