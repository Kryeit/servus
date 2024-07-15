package com.kryeit.servus.config;

import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.cors.CorsConfigurationSource;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static com.kryeit.servus.user.Permission.ADMIN_CREATE;
import static com.kryeit.servus.user.Permission.ADMIN_DELETE;
import static com.kryeit.servus.user.Permission.ADMIN_READ;
import static com.kryeit.servus.user.Permission.ADMIN_UPDATE;
import static com.kryeit.servus.user.Role.STAFF;
import static org.springframework.http.HttpMethod.DELETE;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpMethod.PUT;
import static org.springframework.security.config.http.SessionCreationPolicy.STATELESS;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
@EnableMethodSecurity
public class SecurityConfiguration
{

  private static final String[] WHITE_LIST_URL = {"/api/v1/auth/**",
                                                  "/api/v1/otp/**",
                                                  "/v2/api-docs",
                                                  "/v3/api-docs",
                                                  "/v3/api-docs/**",
                                                  "/swagger-resources",
                                                  "/swagger-resources/**",
                                                  "/configuration/ui",
                                                  "/configuration/security",
                                                  "/swagger-ui/**",
                                                  "/webjars/**",
                                                  "/swagger-ui.html"};

  @Value("${cors.allowed.origins}")
  private String corsAllowedOrigins;

  private final JwtAuthenticationFilter jwtAuthFilter;
  private final AuthenticationProvider authenticationProvider;
  private final LogoutHandler logoutHandler;

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception
  {
    http.csrf(AbstractHttpConfigurer::disable)
        .cors(cors -> cors.configurationSource(corsConfigurationSource()))
        .authorizeHttpRequests(req -> req.requestMatchers(WHITE_LIST_URL)
                                         .permitAll()
                                         .requestMatchers("/api/v1/management/**")
                                         .hasAnyRole(STAFF.name())
                                         .requestMatchers(GET, "/api/v1/management/**")
                                         .hasAnyAuthority(ADMIN_READ.name())
                                         .requestMatchers(POST, "/api/v1/management/**")
                                         .hasAnyAuthority(ADMIN_CREATE.name())
                                         .requestMatchers(PUT, "/api/v1/management/**")
                                         .hasAnyAuthority(ADMIN_UPDATE.name())
                                         .requestMatchers(DELETE, "/api/v1/management/**")
                                         .hasAnyAuthority(ADMIN_DELETE.name())
                                         .anyRequest()
                                         .authenticated())
        .sessionManagement(session -> session.sessionCreationPolicy(STATELESS))
        .authenticationProvider(authenticationProvider)
        .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
        .logout(logout -> logout.logoutUrl("/api/v1/auth/logout")
                                .addLogoutHandler(logoutHandler)
                                .logoutSuccessHandler((request, response, authentication) -> SecurityContextHolder.clearContext()));

    return http.build();
  }

  @Bean
  public CorsConfigurationSource corsConfigurationSource()
  {
    List<String> allowedOrigins = Arrays.stream(corsAllowedOrigins.split(","))
                                        .map(String::trim)
                                        .collect(Collectors.toList());

    CorsConfiguration configuration = new CorsConfiguration();
    configuration.setAllowedOrigins(allowedOrigins);
    configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
    configuration.setAllowedHeaders(Arrays.asList("Authorization", "Cache-Control", "Content-Type"));
    configuration.setAllowCredentials(true);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);
    return source;
  }
}
