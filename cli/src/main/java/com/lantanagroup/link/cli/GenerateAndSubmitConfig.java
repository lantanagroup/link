package com.lantanagroup.link.cli;

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
@ConfigurationProperties(prefix = "cli.generate-and-submit")
@Validated
@PropertySource(value = "classpath:application.yml", factory = YamlPropertySourceFactory.class)
public class GenerateAndSubmitConfig {

  private String apiUrl;
  private GenerateAndSubmitPeriodStart periodStart;
  private GenerateAndSubmitPeriodEnd periodEnd;
  private AuthConfig auth;
  private String reportTypeId;
}

@Getter
@Setter
class GenerateAndSubmitPeriodStart {

  private int adjustDay;
  private int adjustMonth;
  private boolean startOfDay;
}

@Getter
@Setter
class GenerateAndSubmitPeriodEnd {

  private int adjustDay;
  private boolean endOfDay;
}
