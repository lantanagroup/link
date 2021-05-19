package com.lantanagroup.link.config.api;

import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.NumberFormat;

import java.math.BigDecimal;

@Getter @Setter
public class ApiMeasureLocationConfig {
  private String system;
  private String value;

  private BigDecimal latitude;
  private BigDecimal longitude;
}
