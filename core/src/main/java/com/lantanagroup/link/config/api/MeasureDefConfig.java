package com.lantanagroup.link.config.api;

import lombok.Getter;
import lombok.Setter;

/**
 * Configuration for a supported measure definition within the installation
 */
@Getter
@Setter
public class MeasureDefConfig {
  private String id;
  private String shortName;
  private String longName;
  private String definitionUrl;
}
