package com.lantanagroup.link.config.bundler;

import com.google.common.base.Strings;
import com.lantanagroup.link.config.YamlPropertySourceFactory;
import lombok.Getter;
import lombok.Setter;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.StringType;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "bundler")
@Validated
@PropertySource(value = "classpath:application.yml", factory = YamlPropertySourceFactory.class)
public class BundlerConfig {
  /**
   * Indicates whether the bundle is being created for MHL or not.
   */
  private boolean MHL = false;

  /**
   * The type of submission bundle to create.
   */
  private Bundle.BundleType bundleType = Bundle.BundleType.COLLECTION;

  /**
   * Whether to include censuses in the submission bundle.
   */
  private boolean includeCensuses = true;

  /**
   * Whether to merge multiple censuses into a single list.
   */
  private boolean mergeCensuses = true;

  /**
   * Whether to include individual measure reports in the submission bundle.
   */
  private boolean includeIndividualMeasureReports = true;

  /**
   * Whether to copy non-contained, non-patient line-level resources from the patient data bundle to the submission bundle.
   */
  private boolean reifyLineLevelResources = false;

  /**
   * Whether to move contained line-level resources to the top level of the submission bundle.
   */
  private boolean promoteLineLevelResources = false;

  /**
   * The National Provider Identifier (NPI) of the organization/facility the system is bundling/submitting for, to be
   * used in the submission Bundle's Organization.identifier field
   */
  private String orgNpi;

  /**
   * The name of the organization/facility the system is bundling/submitting for, to be used in the submission
   * Bundle's Organization.name field
   */
  private String orgName;

  /**
   * The phone of the organization/facility the system is bundling/submitting for, to be used in the submission
   * Bundle's Organization.name field
   */
  private String orgPhone;

  /**
   * The email of the organization/facility the system is bundling/submitting for, to be used in the submission
   * Bundle's Organization.name field
   */
  private String orgEmail;

  /**
   * The address of the organization/facility the system is bundling/submitting for, to be used in the submission
   * Bundle's Organization.name field
   */
  private Address orgAddress;

  @Getter
  @Setter
  public class Address {
    private String orgAddressLine;
    private String orgCity;
    private String orgState;
    private String orgPostalCode;
    private String orgCountry;

    public org.hl7.fhir.r4.model.Address getFHIRAddress() {
      org.hl7.fhir.r4.model.Address ret = new org.hl7.fhir.r4.model.Address();

      if (!Strings.isNullOrEmpty(this.getOrgAddressLine())) {
        ret.getLine().add(new StringType(this.getOrgAddressLine()));
      }

      if (!Strings.isNullOrEmpty(this.getOrgCity())) {
        ret.setCity(this.getOrgCity());
      }

      if (!Strings.isNullOrEmpty(this.getOrgState())) {
        ret.setState(this.getOrgState());
      }

      if (!Strings.isNullOrEmpty(this.getOrgPostalCode())) {
        ret.setPostalCode(this.getOrgPostalCode());
      }

      if (!Strings.isNullOrEmpty(this.getOrgCountry())) {
        ret.setCountry(this.getOrgCountry());
      }

      return ret;
    }
  }
}
