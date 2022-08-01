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
    String authJwksUrl = this.config.getOauth() != null ? this.config.getOauth().getAuthJwksUrl() : null;

    authFilter.setAuthenticationManager(new LinkAuthManager(issuer, authJwksUrl, this.config.getBasicAuthUsers()));
    http
            .sessionManagement()
            .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            .and()
            .csrf().disable()
            .cors().and()
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
