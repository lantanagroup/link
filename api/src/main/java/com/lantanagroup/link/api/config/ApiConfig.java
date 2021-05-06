package com.lantanagroup.link.api.config;

import com.lantanagroup.link.config.IDirectConfig;
import com.lantanagroup.link.config.YamlPropertySourceFactory;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.NotNull;
import java.util.List;

@Getter @Setter
@Configuration
@ConfigurationProperties(prefix = "api")
@Validated
@PropertySource(value = "classpath:application.yml", factory = YamlPropertySourceFactory.class)
public class ApiConfig {
    @NotNull
    private String fhirServerStore;

    private String authJwksUrl;
    private String downloader;
    private String sender;
    private String sendUrl;

    @Getter
    private ApiCorsConfig cors;

    @Getter
    private ApiTerminologyConfig terminology;

    @Getter
    private IDirectConfig direct;

    @Getter
    private List<ApiMeasureConfig> measures;

    @Getter @NotNull
    private ApiQueryConfig query;

    @Getter ApiMeasureLocationConfig measureLocation;
}
