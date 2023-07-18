package com.lantanagroup.link.cli;

import com.lantanagroup.link.config.auth.LinkOAuthConfig;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "cli.expunge-data")
public class ExpungeDataConfig {
    private String apiUrl;
    private LinkOAuthConfig auth;
}
