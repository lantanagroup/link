package com.lantanagroup.link.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.List;

@Getter
@Setter
@Validated
@Configuration
@ConfigurationProperties(prefix = "query-list")
@PropertySource(value = "classpath:application.yml", factory = YamlPropertySourceFactory.class)
public class QueryListConfig {
  private String fhirServerBase;

  @Size(min = 1)
  private List<PatientList> lists;

  @Getter
  @Setter
  public static class PatientList {
    @NotBlank
    private String listId;

    @NotNull
    @Size(min = 1)
    private List<@NotBlank String> measureId;
  }
}
