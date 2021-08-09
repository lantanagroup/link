package com.lantanagroup.link.config.api;

import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.NumberFormat;

import java.math.BigDecimal;

@Getter @Setter
public class ApiMeasureLocationConfig {
  /**
   * The value set in <code>MeasureReport.subject.identifier.system</code>.
   */
  private String system;
  /**
   * The value set in <code>MeasureReport.subject.identifier.value</code>.
   */
  private String value;

  /**
   * The value set in <code>MeasureReport.subject.identifier.extension[url = .../GeoLocation].extension[url = latitude].valueDecimal</code>.
   */
  private BigDecimal latitude;

  /**
   * The value set in <code>MeasureReport.subject.identifier.extension[url = .../GeoLocation].extension[url = longitude].valueDecimal</code>.
   */
  private BigDecimal longitude;
}
