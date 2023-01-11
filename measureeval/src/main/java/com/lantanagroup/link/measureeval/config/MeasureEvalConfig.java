package com.lantanagroup.link.measureeval.config;

import com.lantanagroup.link.config.YamlPropertySourceFactory;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.validation.annotation.Validated;

@Getter @Setter
@Configuration
@ConfigurationProperties(prefix = "measure-eval")
@Validated
@PropertySource(value = "classpath:application.yml", factory = YamlPropertySourceFactory.class)
public class MeasureEvalConfig {
  /**
   * The file path location where measure definitions should be stored
   */
  @Getter
  private String measuresPath;
}
