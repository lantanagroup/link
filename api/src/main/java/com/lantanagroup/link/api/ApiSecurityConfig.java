package com.lantanagroup.link.api;

import com.lantanagroup.link.api.auth.LinkAuthenticationSuccessHandler;
import com.lantanagroup.link.api.auth.PreAuthTokenHeaderFilter;
import com.lantanagroup.link.auth.LinkAuthManager;
import com.lantanagroup.link.auth.LinkCredentials;
import com.lantanagroup.link.config.api.ApiConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.web.cors.CorsConfiguration;

import java.util.List;

/**
 * Sets the security for the API component, requiring authentication for most methods using `PreAuthTokenHeaderFilter`
 */
@Configuration
@EnableWebSecurity
@Order(1)
public class ApiSecurityConfig extends WebSecurityConfigurerAdapter {
  @Autowired
  private ApiConfig config;

  @Autowired
  private LinkCredentials linkCredentials;

//  @Autowired
//  private CsrfTokenRepository customCsrfTokenRepository; //custom csrfToken repository

  @Autowired
  private Environment env;

  @Override
  protected void configure(HttpSecurity http) throws Exception {
    PreAuthTokenHeaderFilter authFilter = new PreAuthTokenHeaderFilter("Authorization", linkCredentials, config);
    authFilter.setAuthenticationManager(
            new LinkAuthManager(
                    config.getLinkAuthManager().getIssuer(),
                    config.getLinkAuthManager().getAlgorithm(),
                    config.getLinkAuthManager().getAuthJwksUrl(),
                    config.getLinkAuthManager().getTokenVerificationClass(),
                    null,
                    config.getLinkAuthManager().getTokenValidationEndpoint()
            )
    );
    authFilter.setAuthenticationSuccessHandler(new LinkAuthenticationSuccessHandler(this.config));
    http
            .sessionManagement()
            .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            .and()
            .csrf().disable()
//            .csrf().csrfTokenRepository(customCsrfTokenRepository) //.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
//            .and()
            //.cors()
            .cors().configurationSource(request -> {
                      var cors = new CorsConfiguration();
                      cors.setAllowedOrigins(List.of(config.getCors().getAllowedOrigins()));
                      cors.setAllowedMethods(List.of(config.getCors().getAllowedMethods()));
                      cors.setAllowedHeaders(List.of(config.getCors().getAllowedHeaders()));
                      cors.setAllowCredentials(config.getCors().getAllowedCredentials());
                      return cors;
                    })
            .and()
            .authorizeRequests()
            .antMatchers(HttpMethod.OPTIONS, "/**")
            .permitAll()
            .antMatchers("/config/**", "/api", "/api/docs")
            .permitAll()
            .and()
            .antMatcher("/api/**")
            .addFilter(authFilter)
            .authorizeRequests()
            .anyRequest()
            .authenticated();

    //set content security policy
    String csp = "script-src 'self'";
    http.headers().contentSecurityPolicy(csp);

  }

}


