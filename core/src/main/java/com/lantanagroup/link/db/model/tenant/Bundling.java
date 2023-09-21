package com.lantanagroup.link.db.model.tenant;

import lombok.Getter;
import lombok.Setter;
import org.hl7.fhir.r4.model.Bundle;

@Getter
@Setter
public class Bundling {
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
   * Whether to move contained line-level resources to the top level of the submission bundle.
   */
  private boolean promoteLineLevelResources = true;

  /**
   * The National Provider Identifier (NPI) of the organization/facility the system is bundling/submitting for, to be
   * used in the submission Bundle's Organization.identifier field
   */
  private String npi;

  /**
   * The name of the organization/facility the system is bundling/submitting for, to be used in the submission
   * Bundle's Organization.name field
   */
  private String name;

  /**
   * The phone of the organization/facility the system is bundling/submitting for, to be used in the submission
   * Bundle's Organization.name field
   */
  private String phone;

  /**
   * The email of the organization/facility the system is bundling/submitting for, to be used in the submission
   * Bundle's Organization.name field
   */
  private String email;

  /**
   * The address of the organization/facility the system is bundling/submitting for, to be used in the submission
   * Bundle's Organization.name field
   */
  private Address address;
}
