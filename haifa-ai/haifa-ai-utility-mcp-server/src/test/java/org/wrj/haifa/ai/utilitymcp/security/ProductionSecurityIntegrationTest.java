package org.wrj.haifa.ai.utilitymcp.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {
        "spring.security.oauth2.resourceserver.jwt.issuer-uri=https://issuer.example",
        "haifa.ai.utility-mcp.security.audience=utility-mcp",
        "haifa.ai.utility-mcp.security.allow-missing-origin=false",
        "haifa.ai.utility-mcp.security.allowed-origins[0]=https://agent.example",
        "haifa.ai.utility-mcp.security.requests-per-minute=100"
})
@AutoConfigureMockMvc
@ActiveProfiles("production")
class ProductionSecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Test
    void rejectsMissingToken() throws Exception {
        int status = invoke(null).getResponse().getStatus();
        assertThat(status).isEqualTo(401);
    }

    @Test
    void rejectsTokenMissingEitherRequiredScope() throws Exception {
        int listOnly = invoke("mcp:tools:list").getResponse().getStatus();
        int callOnly = invoke("mcp:tools:call").getResponse().getStatus();
        assertThat(listOnly).isEqualTo(403);
        assertThat(callOnly).isEqualTo(403);
    }

    @Test
    void passesAuthenticationAndAuthorizationWithBothScopes() throws Exception {
        int status = invoke("mcp:tools:list mcp:tools:call").getResponse().getStatus();
        assertThat(status).isNotIn(401, 403);
    }

    private org.springframework.test.web.servlet.MvcResult invoke(String scopes) throws Exception {
        var request = post("/mcp")
                .header("Origin", "https://agent.example")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON, MediaType.TEXT_EVENT_STREAM)
                .content("{}");
        if (scopes != null) {
            request.with(jwt().jwt(token -> token.claim("scope", scopes).audience(java.util.List.of("utility-mcp"))));
        }
        return mockMvc.perform(request).andReturn();
    }
}
