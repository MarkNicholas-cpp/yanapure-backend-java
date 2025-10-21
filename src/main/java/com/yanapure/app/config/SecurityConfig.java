package com.yanapure.app.config;

import java.util.Arrays;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/** Security and CORS configuration */
@Configuration
public class SecurityConfig {

  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration configuration = new CorsConfiguration();

    // Allow specific origins (configured via properties)
    configuration.setAllowedOriginPatterns(List.of("*")); // For development

    // Allow common HTTP methods
    configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));

    // Allow common headers
    configuration.setAllowedHeaders(
        Arrays.asList(
            "Authorization",
            "Content-Type",
            "X-Requested-With",
            "Accept",
            "Origin",
            "Access-Control-Request-Method",
            "Access-Control-Request-Headers"));

    // Allow credentials
    configuration.setAllowCredentials(true);

    // Expose headers to client
    configuration.setExposedHeaders(
        Arrays.asList("Access-Control-Allow-Origin", "Access-Control-Allow-Credentials"));

    // Cache preflight response for 1 hour
    configuration.setMaxAge(3600L);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);

    return source;
  }
}
