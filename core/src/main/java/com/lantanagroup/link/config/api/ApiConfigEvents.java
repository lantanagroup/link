package com.lantanagroup.link.config.api;

import com.lantanagroup.link.config.YamlPropertySourceFactory;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.validation.annotation.Validated;

import java.util.List;

@Data
@Configuration
@ConfigurationProperties(prefix = "api.events")
@Validated
@PropertySource(value = "classpath:application.yml", factory = YamlPropertySourceFactory.class)
public class ApiConfigEvents {

  List<String> BeforeMeasureResolution;

  List<String> AfterMeasureResolution;

  List<String> OnRegeneration;

  List<String> BeforePatientOfInterestLookup;

  List<String> AfterPatientOfInterestLookup;

  List<String> BeforePatientDataQuery;

  List<String> AfterPatientResourceQuery;

  List<String> AfterPatientDataQuery;

  List<String> BeforePatientDataStore;

  List<String> AfterPatientDataStore;

  List<String> BeforeMeasureEval;

  List<String> AfterMeasureEval;

  List<String> BeforeReportStore;

  List<String> AfterReportStore;

  List<String> BeforeBundling;

  List<String> AfterBundling;
}
