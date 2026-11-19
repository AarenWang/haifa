package org.wrj.haifa.ai.utilitymcp.config;

import java.time.Clock;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.Ordered;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.core.env.Environment;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtDecoders;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.authorization.AuthorizationManagers;
import org.springframework.security.authorization.AuthorityAuthorizationManager;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.wrj.haifa.ai.utilitymcp.security.OriginRateLimitFilter;
import org.wrj.haifa.ai.utilitymcp.observability.McpRequestLoggingFilter;

@Configuration
public class SecurityConfiguration {

    @Bean
    FilterRegistrationBean<McpRequestLoggingFilter> mcpRequestLoggingFilter(ObjectMapper objectMapper) {
        FilterRegistrationBean<McpRequestLoggingFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new McpRequestLoggingFilter(objectMapper));
        registration.addUrlPatterns("/mcp");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 10);
        return registration;
    }

    @Bean
    FilterRegistrationBean<OriginRateLimitFilter> originRateLimitFilter(
            UtilityMcpProperties properties,
            Clock clock) {
        FilterRegistrationBean<OriginRateLimitFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new OriginRateLimitFilter(properties, clock));
        registration.addUrlPatterns("/mcp");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 20);
        return registration;
    }

    @Bean
    @Profile("!production")
    SecurityFilterChain localSecurity(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(authorize -> authorize.anyRequest().permitAll())
                .build();
    }

    @Bean
    @Profile("production")
    SecurityFilterChain productionSecurity(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/actuator/health/**", "/actuator/info").permitAll()
                        .requestMatchers("/mcp").access(AuthorizationManagers.allOf(
                                AuthorityAuthorizationManager.<RequestAuthorizationContext>hasAuthority("SCOPE_mcp:tools:list"),
                                AuthorityAuthorizationManager.<RequestAuthorizationContext>hasAuthority("SCOPE_mcp:tools:call")))
                        .anyRequest().authenticated())
                .oauth2ResourceServer(resourceServer -> resourceServer.jwt(Customizer.withDefaults()))
                .build();
    }

    @Bean
    @Profile("production")
    JwtDecoder productionJwtDecoder(Environment environment, UtilityMcpProperties properties) {
        String issuer = environment.getRequiredProperty("spring.security.oauth2.resourceserver.jwt.issuer-uri");
        JwtDecoder decoder = JwtDecoders.fromIssuerLocation(issuer);
        OAuth2TokenValidator<Jwt> audience = jwt -> jwt.getAudience().contains(properties.getSecurity().getAudience())
                ? OAuth2TokenValidatorResult.success()
                : OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", "Required audience is missing", null));
        if (decoder instanceof org.springframework.security.oauth2.jwt.NimbusJwtDecoder nimbus) {
            nimbus.setJwtValidator(new DelegatingOAuth2TokenValidator<>(JwtValidators.createDefaultWithIssuer(issuer), audience));
        }
        return decoder;
    }
}
