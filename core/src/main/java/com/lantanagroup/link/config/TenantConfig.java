package com.lantanagroup.link.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.lantanagroup.link.config.bundler.BundlerConfig;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TenantConfig {
  private String id;

  private String name;

  private String database;

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
  private BundlerConfig.Address orgAddress;
}
