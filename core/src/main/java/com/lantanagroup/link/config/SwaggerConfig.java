package com.lantanagroup.link.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "swagger")
@PropertySource(value = {"classpath:application.yml", "classpath:application-site.yml"}, factory = YamlPropertySourceFactory.class)
public class SwaggerConfig {
    @Getter
    private String authUrl;

    @Getter
    private String tokenUrl;

    @Getter
    private String[] scope;
}
