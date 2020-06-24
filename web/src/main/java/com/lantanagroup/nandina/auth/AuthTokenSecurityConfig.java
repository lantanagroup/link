package com.lantanagroup.nandina.auth;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.access.ExceptionTranslationFilter;
import org.springframework.security.web.authentication.Http403ForbiddenEntryPoint;

@Configuration
@EnableWebSecurity
@Order(1)
public class AuthTokenSecurityConfig extends WebSecurityConfigurerAdapter {

    @Override
    protected void configure(HttpSecurity httpSecurity) throws Exception
    {
        PreAuthTokenHeaderFilter filter = new PreAuthTokenHeaderFilter("Authorization");

        filter.setAuthenticationManager(new NandinaAuthManager());

        httpSecurity.
                antMatcher("/config/smart")
                .anonymous();
        
        httpSecurity.
                antMatcher("/api/**")
                .csrf()
                .disable()
                .sessionManagement()
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                .and()
                .addFilter(filter)
                .addFilterBefore(new ExceptionTranslationFilter(new Http403ForbiddenEntryPoint()), filter.getClass())
                .authorizeRequests()
                .anyRequest()
                .authenticated();
    }
}