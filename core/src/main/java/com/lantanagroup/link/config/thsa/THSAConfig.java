package com.lantanagroup.link.config.thsa;

import lombok.Getter;
import lombok.Setter;
import org.springframework.context.annotation.Configuration;


@Configuration
public class THSAConfig {
  /**
   * The id of the MeasureReport for inventory data
   */
  @Setter
  @Getter
  private String dataMeasureReportId;
}
