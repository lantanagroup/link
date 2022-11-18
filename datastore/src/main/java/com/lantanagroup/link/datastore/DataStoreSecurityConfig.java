package com.lantanagroup.link.datastore;

import com.lantanagroup.link.auth.LinkAuthManager;
import com.lantanagroup.link.config.datastore.DataStoreConfig;
import com.lantanagroup.link.datastore.auth.PreAuthTokenHeaderFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.web.cors.CorsConfiguration;

import java.util.List;

/**
 * Sets the security for the datastore component, requiring authentication for most methods using `PreAuthTokenHeaderFilter`
 */
@Configuration
@EnableWebSecurity
@Order(1)
public class DataStoreSecurityConfig extends WebSecurityConfigurerAdapter {

  @Autowired
  private DataStoreConfig config;

  @Autowired
  private Environment env;

  @Override
  protected void configure(HttpSecurity http) throws Exception {
    PreAuthTokenHeaderFilter authFilter = new PreAuthTokenHeaderFilter("Authorization");

    String issuer = this.config.getOauth() != null ? this.config.getOauth().getIssuer() : null;
    String algorithm = this.config.getOauth() != null ? this.config.getOauth().getAlgorithm() : null;
    String authJwksUrl = this.config.getOauth() != null ? this.config.getOauth().getAuthJwksUrl() : null;
    String tokenVerificationClass = this.config.getOauth() != null ? this.config.getOauth().getTokenVerificationClass() : null;
    String tokenValidationEndpoint = this.config.getOauth() != null ? this.config.getOauth().getTokenValidationEndpoint() : null;

    authFilter.setAuthenticationManager(new LinkAuthManager(issuer, algorithm, authJwksUrl, tokenVerificationClass, this.config.getBasicAuthUsers(), tokenValidationEndpoint));
    http
            .sessionManagement()
            .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            .and()
            .csrf().disable()
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
            .and()
            .antMatcher("/**")
            .addFilter(authFilter)
            .authorizeRequests()
            .anyRequest()
            .authenticated();

    //set content security policy
    String csp = "script-src 'self'";
    http.headers().contentSecurityPolicy(csp);
  }
}
