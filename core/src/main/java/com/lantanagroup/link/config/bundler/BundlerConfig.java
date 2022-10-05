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
   * <strong>bundler.send-whole-bundle</strong><br>Whether to include patient data in the submission bundle
   */
  private boolean sendWholeBundle = true;

  /**
   * <strong>bundler.remove-contained-resources</strong><br>Whether to remove contained resources from patient measure reports
   */
  private boolean removeContainedResources = true;
}
