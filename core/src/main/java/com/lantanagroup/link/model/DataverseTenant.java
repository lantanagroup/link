package com.lantanagroup.link.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class DataverseTenant {
  @JsonProperty("crcbb_name")
  private String name;

  @JsonProperty("crcbb_nameinbundle")
  private String nameInBundle;

  @JsonProperty("crcbb_description")
  private String description;

  @JsonProperty("lcg_nhsnorgid")
  private String nhsnOrgId;

  @JsonProperty("lcg_scheduledataretentioncheck")
  private String scheduledDataRetentionCheck;

  @JsonProperty("lcg_schedulequerystu3patientlist")
  private String scheduledQuerySTU3PatientList;

  @JsonProperty("lcg_normalizecodesystemcleanup")
  private Boolean normalizeCodeSystemCleanup;

  @JsonProperty("lcg_normalizeencounterstatustransformer")
  private Boolean normalizeEncounterStatusTransformer;

  @JsonProperty("lcg_normalizecontainedresourcecleanup")
  private Boolean normalizeContainedResourceCleanup;

  @JsonProperty("lcg_normalizecopylocationidentifiertotype")
  private Boolean normalizeCopyLocationIdentifierToType;

  @JsonProperty("lcg_normalizefixperioddates")
  private Boolean normalizeFixPeriodDates;

  @JsonProperty("lcg_normalizefixresourceids")
  private Boolean normalizeFixResourceIds;

  @JsonProperty("lcg_normalizepatientdataresourcefilter")
  private Boolean normalizePatientDataResourceFilter;

  @JsonProperty("lcg_authenticationmethod")
  private Integer authenticationMethod;

  @JsonProperty("lcg_fhirserverbase")
  private String fhirServerBase;

  @JsonProperty("lcg_parallelpatients")
  private Integer parallelPatients;

  @JsonProperty("lcg_authclientid")
  private String authClientId;

  @JsonProperty("lcg_authtokenurl")
  private String authTokenUrl;

  @JsonProperty("lcg_authaudience")
  private String authAudience;
}
