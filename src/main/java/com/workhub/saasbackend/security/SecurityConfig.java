package com.workhub.saasbackend.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.workhub.saasbackend.exception.ApiError;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.io.IOException;
import java.time.Instant;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   JwtAuthenticationFilter jwtAuthenticationFilter,
                                                   TenantFilter tenantFilter,
                                                   ObjectMapper objectMapper) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(authenticationEntryPoint(objectMapper))
                        .accessDeniedHandler(accessDeniedHandler(objectMapper))
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health").permitAll()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(tenantFilter, JwtAuthenticationFilter.class);

        return http.build();
    }

        private AuthenticationEntryPoint authenticationEntryPoint(ObjectMapper objectMapper) {
                return (request, response, exception) -> writeError(
                                objectMapper,
                                request,
                                response,
                                HttpStatus.UNAUTHORIZED,
                                "Authentication failed"
                );
        }

        private AccessDeniedHandler accessDeniedHandler(ObjectMapper objectMapper) {
                return (request, response, exception) -> writeError(
                                objectMapper,
                                request,
                                response,
                                HttpStatus.FORBIDDEN,
                                "Access denied"
                );
        }

        private void writeError(ObjectMapper objectMapper,
                                                        HttpServletRequest request,
                                                        HttpServletResponse response,
                                                        HttpStatus status,
                                                        String message) throws IOException {
                ApiError error = new ApiError(
                                Instant.now(),
                                status.value(),
                                message,
                                request.getRequestURI()
                );

                response.setStatus(status.value());
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                objectMapper.writeValue(response.getWriter(), error);
        }
}
