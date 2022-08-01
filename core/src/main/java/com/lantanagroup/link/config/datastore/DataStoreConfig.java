package com.lantanagroup.link.config.datastore;

import com.lantanagroup.link.config.DataSourceConfig;
import com.lantanagroup.link.config.YamlPropertySourceFactory;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.validation.annotation.Validated;

import java.util.HashMap;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "datastore")
@Validated
@PropertySource(value = "classpath:application.yml", factory = YamlPropertySourceFactory.class)
public class DataStoreConfig {
  @Getter
  private DataSourceConfig dataSource;

  @Getter
  private DataStoreOAuthConfig oauth;

  @Getter
  private HashMap<String, String> basicAuthUsers;
}
