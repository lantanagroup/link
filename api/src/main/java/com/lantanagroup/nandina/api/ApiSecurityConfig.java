package com.lantanagroup.nandina.api;

import com.lantanagroup.nandina.api.auth.NandinaAuthManager;
import com.lantanagroup.nandina.api.auth.PreAuthTokenHeaderFilter;
import com.lantanagroup.nandina.api.config.ApiConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@EnableWebSecurity
@Order(1)
public class ApiSecurityConfig extends WebSecurityConfigurerAdapter {
  @Autowired
  private ApiConfig config;

  @Override
  protected void configure(HttpSecurity http) throws Exception {
    PreAuthTokenHeaderFilter authFilter = new PreAuthTokenHeaderFilter("Authorization");
    authFilter.setAuthenticationManager(new NandinaAuthManager(this.config));

    http
            .csrf().disable()
            .cors()
            .and()
            .authorizeRequests()
            .antMatchers(HttpMethod.OPTIONS, "/**")
            .permitAll()
            .antMatchers("/api/fhir/**", "/api/cda", "/config/**")
            .permitAll()
            .and()
            .antMatcher("/api/**")
            .addFilter(authFilter)
            .authorizeRequests()
            .anyRequest()
            .authenticated();
  }

  @Bean
  public WebMvcConfigurer corsConfigurer() {
    // TODO: Replace with configuration
    String allowsOrigins = "*";
    String[] allowedMethods = new String[] { "GET", "POST", "PUT", "OPTIONS" };
    String allowedHeaders = "*";
    Boolean allowCredentials = true;

    return new WebMvcConfigurer() {
      @Override
      public void addCorsMappings(CorsRegistry registry) {
        registry
                .addMapping("/**")
                .allowedOrigins(allowsOrigins)
                .allowedMethods(allowedMethods)
                .allowedHeaders(allowedHeaders)
                .allowCredentials(allowCredentials);
      }
    };
  }
}