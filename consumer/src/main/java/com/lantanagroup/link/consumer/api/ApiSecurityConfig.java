package com.lantanagroup.link.consumer.api;

import com.lantanagroup.link.api.auth.LinkAuthManager;
import com.lantanagroup.link.config.api.ApiConfig;
import com.lantanagroup.link.config.consumer.ConsumerConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;

/**
 * Sets the security for the API component, requiring authentication for most methods using `PreAuthTokenHeaderFilter`
 */
@Configuration
@EnableWebSecurity
@Order(1)
public class ApiSecurityConfig extends WebSecurityConfigurerAdapter {

  @Autowired
  private ConsumerConfig config;

  @Autowired
  private Environment env;

  @Override
  protected void configure(HttpSecurity http) throws Exception {
    PreAuthTokenHeaderFilter authFilter = new PreAuthTokenHeaderFilter("Authorization");
    authFilter.setAuthenticationManager(new LinkAuthManager(this.config.getIssuer(), this.config.getAuthJwksUrl()));
    http
            .csrf().disable()
            .cors()
            .and()
            .authorizeRequests()
            .antMatchers(HttpMethod.OPTIONS, "/**")
            .permitAll()
            .and()
            .antMatcher("/csv/**")
            .addFilter(authFilter)
            .authorizeRequests()
            .anyRequest()
            .authenticated();

    //set content security policy
    String csp = "script-src 'self'";
    http.headers().contentSecurityPolicy(csp);
  }
}
