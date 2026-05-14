package com.SmartHITL.AI_Application.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtFilter) {
        this.jwtFilter = jwtFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> {})
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authorizeHttpRequests(auth -> auth

                        // PUBLIC ENDPOINTS
                        .requestMatchers("/test/**").permitAll()
                        .requestMatchers("/api/auth/**").permitAll()

                        // USER ACCESS
                        .requestMatchers("/api/user/**")
                        .hasAnyAuthority("USER", "ADMIN")

                        // TICKET ACCESS
                        .requestMatchers("/api/tickets/**")
                        .hasAnyAuthority("USER", "ADMIN")

                        // MESSAGES — USER + ADMIN
                        .requestMatchers("/api/messages/**")
                        .hasAnyAuthority("USER", "ADMIN")

                        // ADMIN CHAT — ADMIN only
                        .requestMatchers("/api/chat/**")
                        .hasAuthority("ADMIN")

                        // ADMIN ACCESS
                        .requestMatchers("/api/admin/**")
                        .hasAuthority("ADMIN")

                        // EVERYTHING ELSE NEEDS AUTH
                        .anyRequest().authenticated()
                )

                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}