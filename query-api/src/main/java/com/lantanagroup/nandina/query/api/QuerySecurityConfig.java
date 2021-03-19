package com.lantanagroup.nandina.query.api;

import com.lantanagroup.nandina.query.api.auth.QueryAuthFilter;
import com.lantanagroup.nandina.query.api.config.QueryConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;

@Configuration
@EnableWebSecurity
@Order(1)
public class QuerySecurityConfig extends WebSecurityConfigurerAdapter {
  @Autowired
  private QueryConfig config;

  @Override
  protected void configure(HttpSecurity http) throws Exception {
    QueryAuthFilter authFilter = new QueryAuthFilter(this.config.getApiKey(), this.config.getAllowedRemote());

    http
            .csrf().disable()
            .antMatcher("/api/**")
            .addFilter(authFilter)
            .authorizeRequests()
            .anyRequest()
            .authenticated();
  }
}
