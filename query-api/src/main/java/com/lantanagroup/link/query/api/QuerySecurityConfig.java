package com.lantanagroup.link.query.api;

import com.lantanagroup.link.query.api.auth.QueryApiAuthFilter;
import com.lantanagroup.link.config.query.QueryConfig;
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
    QueryApiAuthFilter authFilter = new QueryApiAuthFilter(this.config.getApiKey(), this.config.getAllowedRemote(), this.config.getProxyAddress());

    http
            .csrf().disable()
            .antMatcher("/api/**")
            .addFilter(authFilter)
            .authorizeRequests()
            .anyRequest()
            .authenticated();
  }
}
