package com.lantanagroup.link.config.query;

import com.lantanagroup.link.config.YamlPropertySourceFactory;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.validation.annotation.Validated;

import java.util.List;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "query.uscore")
@Validated
@PropertySource(value = "classpath:application.yml", factory = YamlPropertySourceFactory.class)
public class USCoreConfig {
  private List<String> queries;
  private List<String> additionalResources;
}
