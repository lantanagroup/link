package com.lantanagroup.link.api;

import com.lantanagroup.link.api.auth.PreAuthTokenHeaderFilter;
import com.lantanagroup.link.auth.LinkCredentials;
import com.lantanagroup.link.config.api.ApiConfig;
import com.lantanagroup.link.db.SharedService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;

import java.util.List;

/**
 * Sets the security for the API component, requiring authentication for most methods using `PreAuthTokenHeaderFilter`
 */
@Configuration
@EnableWebSecurity
@Order(1)
public class ApiSecurityConfig {
  @Autowired
  private ApiConfig config;

  @Autowired
  private SharedService sharedService;

//  @Autowired
//  private CsrfTokenRepository customCsrfTokenRepository; //custom csrfToken repository

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    PreAuthTokenHeaderFilter authFilter = new PreAuthTokenHeaderFilter("Authorization", config, this.sharedService);
    authFilter.setAuthenticationManager(authentication -> {
      LinkCredentials credentials = (LinkCredentials) authentication.getPrincipal();
      if (credentials.getUser() != null) {
        authentication.setAuthenticated(true);
      }
      return authentication;
    });
    return http.sessionManagement(configurer -> configurer.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .csrf(AbstractHttpConfigurer::disable)
            .cors(configurer -> configurer.configurationSource(request -> {
              var cors = new CorsConfiguration();
              cors.setAllowedOrigins(List.of(config.getCors().getAllowedOrigins()));
              cors.setAllowedMethods(List.of(config.getCors().getAllowedMethods()));
              cors.setAllowedHeaders(List.of(config.getCors().getAllowedHeaders()));
              cors.setAllowCredentials(config.getCors().getAllowedCredentials());
              return cors;
            }))
            .addFilter(authFilter)
            .authorizeHttpRequests(registry -> {
              registry.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll();
              registry.requestMatchers("/config/**", "/api", "/api/docs").permitAll();
              registry.requestMatchers("/api/**").authenticated();
            })
            .headers(configurer -> configurer.contentSecurityPolicy(csp -> {
              csp.policyDirectives("script-src 'self'");
            }))
            .build();
  }
}
