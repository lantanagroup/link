package com.lantanagroup.link.config.bundler;

import com.lantanagroup.link.config.YamlPropertySourceFactory;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "bundler")
@Validated
@PropertySource(value = "classpath:application.yml", factory = YamlPropertySourceFactory.class)
public class BundlerConfig {
  /**
   * <strong>api.send-whole-bundle</strong><br>Boolean used to determine if the full Bundle is sent or just the MeasureReport. True to send full bundle and false to send just the MeasureReport
   */
  private Boolean sendWholeBundle;

  /**
   * <strong>api.remove-generated-observations</strong><br>Whether to remove contained evaluated resources from patient measure reports
   */
  private boolean removeGeneratedObservations = true;
}
