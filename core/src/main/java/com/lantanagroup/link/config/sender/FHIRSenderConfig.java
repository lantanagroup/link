package com.lantanagroup.link.config.sender;

import com.lantanagroup.link.config.YamlPropertySourceFactory;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import java.util.List;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "sender.fhir")
@PropertySource(value = "classpath:application.yml", factory = YamlPropertySourceFactory.class)
public class FHIRSenderConfig {

    /**
     * <strong>sender.fhir.auth-config</strong>
     */
    FHIRSenderOAuthConfig authConfig;

    /**
     * <strong>sender.fhir.url</strong>
     */
    private String url;

    /**
     * <strong>oauth.compress</strong><br>Whether to compress reports during submission
     */
    private boolean compress;

}
