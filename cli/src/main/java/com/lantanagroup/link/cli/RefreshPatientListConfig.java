package com.lantanagroup.link.cli;

import com.lantanagroup.link.config.YamlPropertySourceFactory;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "cli.refresh-patient-list")
@PropertySource(value = "classpath:application.yml", factory = YamlPropertySourceFactory.class)
public class RefreshPatientListConfig {
  private String apiUrl;
  private AuthConfig auth;
  private String patientListId;
}
