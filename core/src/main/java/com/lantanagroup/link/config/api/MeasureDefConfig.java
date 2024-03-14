package com.lantanagroup.link.config.api;

import lombok.Getter;
import lombok.Setter;

/**
 * Configuration for a supported measure definition within the installation
 */
@Getter
@Setter
public class MeasureDefConfig {
  /**
   * The unique identifier for the measure definition
   */
  private String id;

  /**
   * The short name for the measure definition (i.e. "Hypo")
   */
  private String shortName;

  /**
   * The long name for the measure definition (i.e. "Hypoglycemia")
   */
  private String longName;

  /**
   * The URL where the latest version of the measure definition lives
   */
  private String definitionUrl;
}
