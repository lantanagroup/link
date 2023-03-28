package com.lantanagroup.link;

import com.lantanagroup.link.config.api.ApiConfig;
import com.lantanagroup.link.config.api.ApiReportDefsConfig;
import com.lantanagroup.link.config.api.ApiReportDefsUrlConfig;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.HumanName;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class FhirHelperTests {

  @Test
  public void getNameTest() {
    HumanName name1 = new HumanName().setFamily("Sombody").addGiven("Joe");
    HumanName name2 = new HumanName().addGiven("Joe Sombody");
    HumanName name3 = new HumanName().setFamily("Joe Sombody");
    HumanName name4 = new HumanName().setText("Joe Sombody");
    HumanName name5 = new HumanName();

    String actual1 = FhirHelper.getName(Arrays.asList(name1));
    String actual2 = FhirHelper.getName(Arrays.asList(name2));
    String actual3 = FhirHelper.getName(Arrays.asList(name3));
    String actual4 = FhirHelper.getName(Arrays.asList(name4));
    String actual5 = FhirHelper.getName(Arrays.asList(name5));
    String actual6 = FhirHelper.getName(Arrays.asList());

    Assert.assertEquals(actual1, "Joe Sombody");
    Assert.assertEquals(actual2, "Joe Sombody");
    Assert.assertEquals(actual3, "Joe Sombody");
    Assert.assertEquals(actual4, "Joe Sombody");
    Assert.assertEquals(actual5, "Unknown");
    Assert.assertEquals(actual6, "Unknown");
  }

  @Test
  public void getReportAggregatorClassTest() {
    ApiConfig apiConfig = new ApiConfig();
    apiConfig.setReportAggregator("");
    ApiReportDefsConfig apiReportDefsConfig = new ApiReportDefsConfig();
    List<ApiReportDefsUrlConfig> apiReportDefsConfigList = new ArrayList<>();
    apiReportDefsConfig.setUrls(apiReportDefsConfigList);
    ApiReportDefsUrlConfig apiReportDefsUrlConfig = new ApiReportDefsUrlConfig();
    apiReportDefsUrlConfig.setUrl("https://ehr-test.nhsnlink.org/fhir/Bundle/NHSNGlycemicControlHypoglycemicInitialPopulation");
    apiReportDefsUrlConfig.setBundleId("NHSNGlycemicControlHypoglycemicInitialPopulation");
    apiReportDefsUrlConfig.setReportAggregator("com.lantanagroup.link.nhsn.ReportAggregator");
    apiConfig.setReportDefs(apiReportDefsConfig);
    apiReportDefsConfigList.add(apiReportDefsUrlConfig);
    Bundle bundle = new Bundle();
    bundle.setId("NHSNGlycemicControlHypoglycemicInitialPopulation");
    String reportAggregatorClassName = FhirHelper.getReportAggregatorClassName(apiConfig, bundle);
    Assert.assertEquals("com.lantanagroup.link.nhsn.ReportAggregator", reportAggregatorClassName);
  }

  @Test
  public void getDefaultAggregatorClassTest() {
    ApiConfig apiConfig = new ApiConfig();
    apiConfig.setReportAggregator("com.lantanagroup.link.nhsn.ReportAggregator");
    ApiReportDefsConfig apiReportDefsConfig = new ApiReportDefsConfig();
    List<ApiReportDefsUrlConfig> apiReportDefsConfigList = new ArrayList<>();
    apiReportDefsConfig.setUrls(apiReportDefsConfigList);
    ApiReportDefsUrlConfig apiReportDefsUrlConfig = new ApiReportDefsUrlConfig();
    apiReportDefsUrlConfig.setUrl("https://ehr-test.nhsnlink.org/fhir/Bundle/NHSNGlycemicControlHypoglycemicInitialPopulation");
    apiReportDefsUrlConfig.setBundleId("NHSNGlycemicControlHypoglycemicInitialPopulation");
    apiReportDefsUrlConfig.setReportAggregator("");
    apiConfig.setReportDefs(apiReportDefsConfig);
    apiReportDefsConfigList.add(apiReportDefsUrlConfig);
    Bundle bundle = new Bundle();
    bundle.setId("NHSNGlycemicControlHypoglycemicInitialPopulation");
    String reportAggregatorClassName = FhirHelper.getReportAggregatorClassName(apiConfig, bundle);
    Assert.assertEquals("com.lantanagroup.link.nhsn.ReportAggregator", reportAggregatorClassName);
  }

  @Test
  public void getTHSAMeasureAggregatorClassTest() {
    ApiConfig apiConfig = new ApiConfig();
    apiConfig.setReportAggregator("");
    ApiReportDefsConfig apiReportDefsConfig = new ApiReportDefsConfig();
    List<ApiReportDefsUrlConfig> apiReportDefsConfigList = new ArrayList<>();
    apiReportDefsConfig.setUrls(apiReportDefsConfigList);
    ApiReportDefsUrlConfig apiReportDefsUrlConfig = new ApiReportDefsUrlConfig();
    apiReportDefsUrlConfig.setUrl("https://ehr-test.nhsnlink.org/fhir/Bundle/THSAMeasure");
    apiReportDefsUrlConfig.setBundleId("THSAMeasure");
    apiReportDefsUrlConfig.setReportAggregator("com.lantanagroup.link.thsa.THSAAggregator");
    apiConfig.setReportDefs(apiReportDefsConfig);
    apiReportDefsConfigList.add(apiReportDefsUrlConfig);
    Bundle bundle = new Bundle();
    bundle.setId("THSAMeasure");
    String reportAggregatorClassName = FhirHelper.getReportAggregatorClassName(apiConfig, bundle);
    Assert.assertEquals("com.lantanagroup.link.thsa.THSAAggregator", reportAggregatorClassName);
  }

}
