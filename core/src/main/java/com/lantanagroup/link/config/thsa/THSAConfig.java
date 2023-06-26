package com.lantanagroup.link.config.thsa;

import com.lantanagroup.link.config.YamlPropertySourceFactory;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "thsa")
@PropertySource(value = "classpath:application.yml", factory = YamlPropertySourceFactory.class)
public class THSAConfig {
  /**
   * The id of the MeasureReport for bed and icu-bed inventory data
   */
  @Setter
  @Getter
  //private String dataMeasureReportId;
  private String bedInventoryReportId;

  /*
  The id of the Vent inventory MeasureReport
   */
  @Setter
  @Getter
  private String ventInventoryReportId;
}
