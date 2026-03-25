package com.communityplatform.community.config;

import com.communityplatform.community.security.CustomUserDetailsService;
import com.communityplatform.community.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * SecurityConfig — the central wiring point for all Spring Security behaviour.
 *
 * This class does five things:
 *   1. Declares which HTTP endpoints are public and which require authentication
 *   2. Registers our JwtAuthenticationFilter in the filter chain
 *   3. Configures stateless session management (no server-side sessions)
 *   4. Provides a BCryptPasswordEncoder bean for hashing passwords
 *   5. Provides an AuthenticationManager bean used by AuthService at login
 *
 * ── Spring Security 6 vs older versions ─────────────────────
 *  Spring Security 6 (used in Spring Boot 3) removed the old
 *  WebSecurityConfigurerAdapter class. The modern approach uses
 *  @Bean methods returning SecurityFilterChain, PasswordEncoder,
 *  and AuthenticationManager — no class extension needed.
 *
 *  The lambda-style DSL (.csrf(csrf -> csrf.disable())) replaced
 *  the chained method style (.csrf().disable()) from Security 5.
 *
 * ── @EnableMethodSecurity ────────────────────────────────────
 *  Enables @PreAuthorize annotations on controller/service methods.
 *  Example:
 *    @PreAuthorize("hasRole('MODERATOR')")
 *    public void deletePost(Long id) { ... }
 *
 *  Without this annotation, @PreAuthorize is silently ignored.
 *
 * ── Filter chain order ───────────────────────────────────────
 *  Spring Security maintains an ordered chain of filters.
 *  We insert JwtAuthenticationFilter BEFORE Spring's built-in
 *  UsernamePasswordAuthenticationFilter using addFilterBefore().
 *  This ensures our JWT check runs first — if valid, the request
 *  arrives at UsernamePasswordAuthenticationFilter already authenticated.
 *
 * ── CSRF disabled — why is that safe here? ──────────────────
 *  CSRF attacks exploit browser cookie behaviour. They are NOT
 *  relevant for stateless JWT-based APIs because:
 *    - We don't use cookies — tokens are sent in the Authorization header
 *    - A malicious page cannot read/steal the Authorization header
 *    - There is no session cookie for a browser to silently include
 *  Disabling CSRF is standard and correct for REST APIs using bearer tokens.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomUserDetailsService userDetailsService;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    // ── 1. HTTP Security rules ───────────────────────────────────
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)

                .authorizeHttpRequests(auth -> auth

                        // ── Public endpoints (no token required) ────────────
                        .requestMatchers("/api/auth/**").permitAll()

                        // Anyone can read communities, posts, comments, reviews
                        .requestMatchers(HttpMethod.GET, "/api/communities/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/posts/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/comments/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/reviews/**").permitAll()

                        // ── Moderator-only endpoints ─────────────────────────
                        .requestMatchers("/api/mod/**").hasRole("MODERATOR")

                        // ── Everything else requires authentication ──────────
                        .anyRequest().authenticated()
                )

                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                .authenticationProvider(authenticationProvider())

                .addFilterBefore(
                        jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }

    // ── 2. Password encoder ──────────────────────────────────────
    /**
     * BCryptPasswordEncoder hashes passwords before storing them and
     * verifies submitted passwords during login.
     * Injected into AuthService for registration and into
     * DaoAuthenticationProvider for login verification.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // ── 3. Authentication provider ───────────────────────────────
    /**
     * DaoAuthenticationProvider connects:
     *   - CustomUserDetailsService (loads user from DB by email)
     *   - BCryptPasswordEncoder    (verifies submitted password against stored hash)
     *
     * Called by AuthenticationManager.authenticate() during login.
     */
    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    // ── 4. Authentication manager ────────────────────────────────
    /**
     * Exposed as a bean so AuthService can inject it and call:
     *   authenticationManager.authenticate(
     *       new UsernamePasswordAuthenticationToken(email, rawPassword)
     *   )
     * Spring Boot builds this from the AuthenticationProvider above.
     */
    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration authConfig
    ) throws Exception {
        return authConfig.getAuthenticationManager();
    }
}
