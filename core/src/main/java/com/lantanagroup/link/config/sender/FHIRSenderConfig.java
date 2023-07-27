package com.lantanagroup.link.config.sender;

import com.lantanagroup.link.config.YamlPropertySourceFactory;
import com.lantanagroup.link.config.auth.LinkOAuthConfig;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "sender.fhir")
@PropertySource(value = "classpath:application.yml", factory = YamlPropertySourceFactory.class)
public class FHIRSenderConfig {

    /**
     * <strong>sender.fhir.auth-config</strong>
     */
    LinkOAuthConfig authConfig;

    /**
     * <strong>sender.fhir.url</strong>
     */
    private String url;

}
