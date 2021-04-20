package com.lantanagroup.nandina.config;

import lombok.Getter;
import lombok.Setter;
import org.hibernate.validator.constraints.URL;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.*;

@Getter @Setter
@Configuration
@ConfigurationProperties(prefix = "query")
@Validated
@PropertySource(value = "classpath:application.yml", factory = YamlPropertySourceFactory.class)
public class QueryConfig {
  @NotBlank @URL
  private String fhirServerBase;

  @Size(min = 128)
  private String apiKey;

  private String[] allowedRemote;

  private String authClass;
}
