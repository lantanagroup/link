package com.lantanagroup.nandina;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

import java.util.Map;


@Component
@PropertySource(
        value = "classpath:config-local.json",
        factory = JsonPropertySourceFactory.class)
@EnableConfigurationProperties
@ConfigurationProperties
public class JsonProperties {
    public static final String DEFAULT = "default";
    public static final String FACILITY_ID = "facilityId";
    public static final String SUMMARY_CENSUS_ID = "summaryCensusId";
    public static final String HOSPITALIZED = "hospitalized";
    public static final String HOSPITALIZED_AND_VENTILATED = "hospitalizedAndVentilated";
    public static final String HOSPITAL_ONSET = "hospitalOnset";
    public static final String ED_OVERFLOW = "edOverflow";
    public static final String ED_OVERFLOW_AND_VENTILATED = "edOverflowAndVentilated";
    public static final String DEATHS = "deaths";
    public static final String HOSPITAL_BEDS = "hospitalBeds";
    public static final String HOSPITAL_INPATIENT_BEDS = "hospitalInpatientBeds";
    public static final String HOSPITAL_INPATIENT_BED_OCC = "hospitalInpatientBedOcc";
    public static final String HOSPITAL_ICU_BEDS = "hospitalIcuBeds";
    public static final String HOSPITAL_ICU_BED_OCC = "hospitalIcuBedOcc";
    public static final String MECHANICAL_VENTILATORS = "mechanicalVentilators";
    public static final String MECHANICAL_VENTILATORS_USED = "mechanicalVentilatorsUsed";
    public static final String COVID_CODES_VALUE_SET = "covidCodesValueSet";
    public static final String VENTILATOR_CODES_VALUESET = "ventilatorCodesValueSet";
    public static final String INTUBATION_PROCEDURE_CODES_VALUESET = "intubationProcedureCodesValueSet";

    private String exportFormat;
    private String fhirServerBase;
    private String fhirServerQueryBase;
    private String fhirServerStoreBase;
    private String fhirServerUserName;
    private String fhirServerPassword;
    private String fhirServerBearerToken;
    private String authIssuer;
    private String authJwksUrl;
    private String authClientId;
    private String authScope;
    private Map<String, String> query;
    private Map<String, String> terminology;
    private Map<String, Map<String, String>> field;
    private String rememberFields;
    private boolean requireHttps;

    public String getExportFormat() {
        return exportFormat;
    }

    public void setExportFormat(String exportFormat) {
        this.exportFormat = exportFormat;
    }

    public String getFhirServerBase() {
        return fhirServerBase;
    }

    public void setFhirServerBase(String fhirServerBase) {
        this.fhirServerBase = fhirServerBase;
    }

    public String getFhirServerQueryBase() {
        return fhirServerQueryBase;
    }

    public void setFhirServerQueryBase(String fhirServerQueryBase) {
        this.fhirServerQueryBase = fhirServerQueryBase;
    }

    public String getFhirServerStoreBase() {
        return fhirServerStoreBase;
    }

    public void setFhirServerStoreBase(String fhirServerStoreBase) {
        this.fhirServerStoreBase = fhirServerStoreBase;
    }

    public String getFhirServerUserName() {
        return fhirServerUserName;
    }

    public void setFhirServerUserName(String fhirServerUserName) {
        this.fhirServerUserName = fhirServerUserName;
    }

    public String getFhirServerPassword() {
        return fhirServerPassword;
    }

    public void setFhirServerPassword(String fhirServerPassword) {
        this.fhirServerPassword = fhirServerPassword;
    }

    public String getFhirServerBearerToken() {
        return fhirServerBearerToken;
    }

    public void setFhirServerBearerToken(String fhirServerBearerToken) {
        this.fhirServerBearerToken = fhirServerBearerToken;
    }

    public String getAuthIssuer() {
        return authIssuer;
    }

    public void setAuthIssuer(String authIssuer) {
        this.authIssuer = authIssuer;
    }

    public String getAuthJwksUrl() {
        return authJwksUrl;
    }

    public void setAuthJwksUrl(String authJwksUrl) {
        this.authJwksUrl = authJwksUrl;
    }

    public String getAuthClientId() {
        return authClientId;
    }

    public void setAuthClientId(String authClientId) {
        this.authClientId = authClientId;
    }

    public String getAuthScope() {
        return authScope;
    }

    public void setAuthScope(String authScope) {
        this.authScope = authScope;
    }

    public Map<String, String> getQuery() {
        return query;
    }

    public void setQuery(Map<String, String> query) {
        this.query = query;
    }

    public Map<String, String> getTerminology() {
        return terminology;
    }

    public void setTerminology(Map<String, String> terminology) {
        this.terminology = terminology;
    }

    public Map<String, Map<String, String>> getField() {
        return field;
    }

    public void setField(Map<String, Map<String, String>> field) {
        this.field = field;
    }

    public String getRememberFields() {
        return rememberFields;
    }

    public void setRememberFields(String rememberFields) {
        this.rememberFields = rememberFields;
    }

    public boolean isRequireHttps() {
        return requireHttps;
    }

    public void setRequireHttps(boolean requireHttps) {
        this.requireHttps = requireHttps;
    }
}
