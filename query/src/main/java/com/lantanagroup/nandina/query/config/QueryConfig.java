package com.lantanagroup.nandina.query.config;

import com.lantanagroup.nandina.config.IQueryConfig;
import com.lantanagroup.nandina.config.QueryAuthConfig;
import com.lantanagroup.nandina.config.YamlPropertySourceFactory;
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
@PropertySource(value = "classpath:query.yml", factory = YamlPropertySourceFactory.class)
public class QueryConfig implements IQueryConfig {
  @NotBlank @URL
  private String fhirServerBase;

  @Size(min = 128)
  private String apiKey;

  @NotEmpty
  private String[] allowedRemote;

  @Getter @NotNull
  private QueryAuthConfig queryAuth;
}
