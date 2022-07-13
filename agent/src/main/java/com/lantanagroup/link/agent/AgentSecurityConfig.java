package com.lantanagroup.link.agent;

import com.lantanagroup.link.agent.auth.AgentAuthFilter;
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
public class AgentSecurityConfig extends WebSecurityConfigurerAdapter {
  @Autowired
  private QueryConfig config;

  @Override
  protected void configure(HttpSecurity http) throws Exception {
    AgentAuthFilter authFilter = new AgentAuthFilter(this.config.getApiKey(), this.config.getAllowedRemote(), this.config.getProxyAddress());

    http
            .csrf().disable()
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
