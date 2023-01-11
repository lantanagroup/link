package com.lantanagroup.link.cli;

import com.lantanagroup.link.config.YamlPropertySourceFactory;
import com.lantanagroup.link.config.api.ApiDataStoreConfig;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.NotNull;

@Getter
@Setter
@Validated
@Configuration
@ConfigurationProperties(prefix = "cli.query")
@PropertySource(value = "classpath:application.yml", factory = YamlPropertySourceFactory.class)
public class QueryCliConfig {
  @NotNull
  private ApiDataStoreConfig dataStore;
  private String fhirServerBase;
}

