package com.lantanagroup.link.config;

import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.validation.annotation.Validated;

@Configuration
@ConfigurationProperties(prefix = "shared-cors")
@Validated
@PropertySource(value = "classpath:application.yml", factory = YamlPropertySourceFactory.class)
public class SharedCorsConfig {
  /**
   * <strong>datastore.public-address</strong><br/>The public address that the data store and API are exposed at.
   */
  @Getter
  private String publicAddress;
}
