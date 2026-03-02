package com.example.jobup.config;

import com.example.jobup.services.UserDetailsServiceImpl;
import jakarta.servlet.http.HttpServletResponse;
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
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;
    private final UserDetailsServiceImpl userDetailsService;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .authorizeHttpRequests(auth -> auth
                        // ===== Public =====
                        .requestMatchers(org.springframework.http.HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/auth/**").permitAll()
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        .requestMatchers("/ws/**", "/topic/**", "/app/**").permitAll()

                        // Files: public viewing of profile/portfolio & direct file view/download
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/files/owner/*/category/PROFILE_PICTURE").permitAll()
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/files/owner/*/category/WORKER_PORTFOLIO").permitAll()
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/files/owner/*/category/WORKER_CERTIFICATE").permitAll()
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/files/*/download").permitAll()
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/files/*").permitAll()

                        // ===== Open READ-ONLY catalog (optional) =====
                        // Allow anyone to browse workers, but protect writes
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/workers/**").permitAll()

                        // ===== Strict role-based areas =====
                        // All client-area APIs
                        .requestMatchers("/api/client/**").hasRole("CLIENT")

                        // All worker-area APIs
                        .requestMatchers("/api/worker/**").hasRole("WORKER")

                        // Files: write/delete require auth (any logged-in user; tighten if needed)
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/files/**").permitAll()
                        .requestMatchers(org.springframework.http.HttpMethod.POST,   "/api/files/**").authenticated()
                        .requestMatchers(org.springframework.http.HttpMethod.DELETE, "/api/files/**").authenticated()

                        // Example: posts created by CLIENTS only
                        .requestMatchers(org.springframework.http.HttpMethod.POST, "/api/posts").hasRole("CLIENT")
                        .requestMatchers(org.springframework.http.HttpMethod.POST, "/api/posts/multipart").hasRole("CLIENT")
                        .requestMatchers(HttpMethod.GET, "/api/deals/worker/*/rating-stats").hasAnyRole("CLIENT","WORKER")

                        .requestMatchers(HttpMethod.GET,  "/api/deals/*/can-rate").hasAnyRole("CLIENT","WORKER")
                        .requestMatchers(HttpMethod.POST, "/api/deals/*/rating").hasAnyRole("CLIENT","WORKER")


                        // If you had previously left these open, REMOVE those permitAll lines:
                        // .requestMatchers("/api/chat/**").permitAll()
                        // .requestMatchers("/api/proposals/**").permitAll()
                        // .requestMatchers("/api/deals/**").permitAll()
                        // and instead place them under the proper namespace:
                        //   /api/client/chat/**, /api/worker/chat/**, /api/client/deals/**, etc.

                        // Everything else must be authenticated
                        .anyRequest().authenticated()
                )
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(e -> e
                        .authenticationEntryPoint((req, res, ex) -> {
                            res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            res.setContentType("application/json");
                            res.setCharacterEncoding("UTF-8");
                            res.getWriter().write("{\"error\":\"UNAUTHORIZED\",\"message\":\"Invalid or missing token\"}");
                        })
                        .accessDeniedHandler((req, res, ex) -> {
                            res.setStatus(HttpServletResponse.SC_FORBIDDEN);
                            res.setContentType("application/json");
                            res.setCharacterEncoding("UTF-8");
                            res.getWriter().write("{\"error\":\"FORBIDDEN\",\"message\":\"Insufficient role for this resource\"}");
                        })
                )
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(Arrays.asList("*"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
