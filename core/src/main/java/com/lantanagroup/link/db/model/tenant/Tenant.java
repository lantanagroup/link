package com.lantanagroup.link.db.model.tenant;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Tenant {
  /**
   * A unique ID of the tenant. Cannot contain special characters other than dashes and underscores.
   */
  private String id;

  /**
   * The name of the tenant, such as the facility's name.
   */
  private String name;

  /**
   * Organization ID for the tenant according to CDC. Used by the Measure Reporting Plan functionality, and
   * when bundling to submit to NHSN; an Organization resource is created uses this as an identifier of the facility.
   */
  private String cdcOrgId;

  /**
   * The vendor that the tenant is using. This is used to determine how to query the EHR's FHIR API.
   */
  private TenantVendors vendor;

  /**
   * A free-text string representing the vendor if the vendor is not in the list of known vendors.
   */
  private String otherVendor;

  /**
   * A description of the tenant that is more meaningful/useful than the id.
   */
  private String description;

  /**
   * The Time Zone string for the Tenant
   */
  private String timeZoneId;
  /**
   * The connection string to use for tenant-specific data.
   */
  private String connectionString;

  /**
   * Configuration properties for how to bundle the final submission, including contact information that is used
   * when creating an Organization resource that represents the sending facility (the tenant).
   */
  private Bundling bundling = new Bundling();

  /**
   * Schedule configuration for the tenant that can be used to automatically begin processes such as querying for
   * patient lists, data retention, and automatic generation and submission of reports.
   * See https://productresources.collibra.com/docs/collibra/latest/Content/Cron/co_spring-cron.htm for more info.
   * See https://crontab.guru/ for more info (note: seconds aren't accounted for in this tool).
   */
  private Schedule scheduling;

  /**
   * Custom classes/functionality that can be turned on at various points during the report generation pipeline.
   */
  private Events events;

  /**
   * Configuration that should be used for /api/{tenantId}/poi/$query-list to query the EHR's FHIR List
   * for patients of interest
   */
  private QueryList queryList;

  /**
   * Configuration to use when querying patients of interest for their clinical data, such as Encounter, Condition, etc.
   * Includes authentication strategy that is needed to successfully query the EHR's FHIR API.
   */
  private FhirQuery fhirQuery;


  /**
   * ISO 8601 formatted duration in which to keep data for each tenant. Defaulted to 3 months.
   */
  private String retentionPeriod = "P3M";

  /**
   *
   */
  private String bulkGroupId;

  /**
   * ex /R4/Group/{groupId}/$export?_type=patient,medicationrequest,medication
   */
  private String relativeBulkUrl;

  /**
   * Header name to retrieve polling url
   */
  private String bulkInitiateResponseUrlHeader;

  /**
   * Header name for progress status. e.g. X-Progress
   */
  private String progressHeaderName;

  /**
   * value that indicates bulk export is ready to retrieve
   */
  private String progressHeaderCompleteValue;

  /**
   * value that indicates the sleep time when looping retries for progress completion
   */
  private int bulkWaitTimeInMilliseconds;
}
