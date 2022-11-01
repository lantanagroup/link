package com.lantanagroup.link.cli;

import com.lantanagroup.link.config.YamlPropertySourceFactory;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.List;

@Getter
@Setter
@Validated
@Configuration
@ConfigurationProperties(prefix = "cli.refresh-patient-list")
@PropertySource(value = "classpath:application.yml", factory = YamlPropertySourceFactory.class)
public class RefreshPatientListConfig {
  @NotBlank
  private String apiUrl;
  private AuthConfig auth;
  @NotNull
  @Size(min = 1)
  private List<PatientList> patientList;
  private CensusReportingPeriods censusReportingPeriod;

  @Getter
  @Setter
  public static class PatientList {
    @NotBlank
    private String patientListPath;
    @NotNull
    @Size(min = 1)
    private List<@NotBlank String> censusIdentifier;
  }
}
