package com.lantanagroup.nandina.auth;

import com.auth0.jwk.Jwk;
import com.auth0.jwk.JwkProvider;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.lantanagroup.nandina.Config;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.access.ExceptionTranslationFilter;
import org.springframework.security.web.authentication.Http403ForbiddenEntryPoint;

import java.security.interfaces.RSAPublicKey;

@Configuration
@EnableWebSecurity
@Order(1)
public class AuthTokenSecurityConfig extends WebSecurityConfigurerAdapter {

    @Override
    protected void configure(HttpSecurity httpSecurity) throws Exception
    {
        PreAuthTokenHeaderFilter filter = new PreAuthTokenHeaderFilter("Authorization");

        filter.setAuthenticationManager(new AuthenticationManager() {
            @Override
            public Authentication authenticate(Authentication authentication) throws AuthenticationException {
                String authHeader = (String) authentication.getPrincipal();

                if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                    throw new BadCredentialsException("This REST operation requires a Bearer Authorization header.");
                }

                // Validate that the token is issued by our configured authentication provider
                String token = authHeader.substring("Bearer ".length());
                DecodedJWT jwt = JWT.decode(token);
                JwkProvider provider = new CustomUrlJwkProvider(Config.getInstance().getAuthJwksUrl());

                try {
                    Jwk jwk = provider.get(jwt.getKeyId());
                    Algorithm algorithm = Algorithm.RSA256((RSAPublicKey) jwk.getPublicKey(), null);
                    algorithm.verify(jwt);

                    authentication.setAuthenticated(true);
                } catch (Exception e) {
                    authentication.setAuthenticated(false);
                    e.printStackTrace();
                }

                return authentication;
            }
        });

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