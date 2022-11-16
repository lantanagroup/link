package com.lantanagroup.link.config.bundler;

import com.lantanagroup.link.config.YamlPropertySourceFactory;
import lombok.Getter;
import lombok.Setter;
import org.hl7.fhir.r4.model.Bundle;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "bundler")
@PropertySource(value = "classpath:application.yml", factory = YamlPropertySourceFactory.class)
public class BundlerConfig {
  /**
   * The type of submission bundle to create.
   */
  private Bundle.BundleType bundleType = Bundle.BundleType.COLLECTION;

  /**
   * Whether to include censuses in the submission bundle.
   */
  private boolean includeCensuses = true;

  /**
   * Whether to merge multiple censuses into a single list.
   */
  private boolean mergeCensuses = true;

  /**
   * Whether to include individual measure reports in the submission bundle.
   */
  private boolean includeIndividualMeasureReports = true;

  /**
   * Whether to move contained resources from the individual measure reports to the submission bundle.
   */
  private boolean promoteContainedResources = true;
}
