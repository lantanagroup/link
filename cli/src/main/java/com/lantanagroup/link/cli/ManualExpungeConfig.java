package com.lantanagroup.link.cli;

import com.lantanagroup.link.config.api.ApiDataStoreConfig;
import com.lantanagroup.link.config.auth.LinkOAuthConfig;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.NotNull;

@Getter
@Setter
@Validated
@Configuration
@ConfigurationProperties(prefix = "cli.manual-expunge")
public class ManualExpungeConfig {
    @NotNull
    private String apiUrl;
    @NotNull
    private LinkOAuthConfig auth;
}
