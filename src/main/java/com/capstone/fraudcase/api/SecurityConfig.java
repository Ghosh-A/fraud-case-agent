package com.capstone.fraudcase.api;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Demo role model (synthetic users only):
 * AGENT — intake + follow-ups; INVESTIGATOR — run investigations, view evidence;
 * REVIEWER — recommendations/citations/reviews; AUDITOR — audit trail.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.ignoringRequestMatchers("/api/**"))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/css/**", "/error", "/actuator/health").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/cases").hasAnyRole("AGENT", "INVESTIGATOR")
                        .requestMatchers(HttpMethod.POST, "/api/cases/*/investigations")
                        .hasAnyRole("INVESTIGATOR", "REVIEWER")
                        .requestMatchers(HttpMethod.POST, "/api/cases/*/reviews").hasRole("REVIEWER")
                        .requestMatchers(HttpMethod.POST, "/api/cases/*/messages")
                        .hasAnyRole("AGENT", "INVESTIGATOR", "REVIEWER")
                        .requestMatchers(HttpMethod.GET, "/api/cases/*/audit").hasRole("AUDITOR")
                        .requestMatchers("/api/**").authenticated()
                        .requestMatchers("/cases/new", "/cases/*/message").hasAnyRole("AGENT", "INVESTIGATOR")
                        .requestMatchers("/cases/*/investigate").hasAnyRole("INVESTIGATOR", "REVIEWER")
                        .requestMatchers("/cases/*/review").hasRole("REVIEWER")
                        .anyRequest().authenticated())
                .formLogin(form -> form.permitAll())
                .httpBasic(basic -> {
                })
                .logout(logout -> logout.permitAll());
        return http.build();
    }

    @Bean
    public UserDetailsService users(PasswordEncoder encoder) {
        return new InMemoryUserDetailsManager(
                User.withUsername("agent").password(encoder.encode("agent123")).roles("AGENT").build(),
                User.withUsername("investigator").password(encoder.encode("inv123")).roles("INVESTIGATOR").build(),
                User.withUsername("reviewer").password(encoder.encode("rev123")).roles("REVIEWER").build(),
                User.withUsername("auditor").password(encoder.encode("aud123")).roles("AUDITOR").build(),
                User.withUsername("admin").password(encoder.encode("admin123"))
                        .roles("AGENT", "INVESTIGATOR", "REVIEWER", "AUDITOR").build());
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }
}
