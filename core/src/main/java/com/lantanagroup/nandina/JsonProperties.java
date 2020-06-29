package com.lantanagroup.nandina;

import ca.uhn.fhir.context.FhirContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;
import org.springframework.util.ResourceUtils;

import java.io.File;
import java.util.Map;

@Component
@PropertySource(value = "classpath:config.json", factory = JsonPropertySourceFactory.class)
@EnableConfigurationProperties
@ConfigurationProperties
public class JsonProperties {
    private static Logger logger = LoggerFactory.getLogger(JsonProperties.class.getName());
    private static String terminologyCovidCodes;
    private static String terminologyVentilatorCodes;
    private static String terminologyIntubationProcedureCodes;

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
    private String authJwksUrl;
    private String prepareQuery;
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

    public String getAuthJwksUrl() {
        return authJwksUrl;
    }

    public void setAuthJwksUrl(String authJwksUrl) {
        this.authJwksUrl = authJwksUrl;
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

    public String loadValueSet(String valueSetUri) {
        String valueSetCodes = null;
        logger.info("Extracting concepts from ValueSet " + valueSetUri);
        try {
            if (valueSetUri.startsWith("https://") || valueSetUri.startsWith("http://")) {
                // TODO: pull the value set from the web
                // or look up cannonical http/https value set URIs from a FHIR server.
                // Just because ValueSet.uri starts with http does not mean it is actually accessible at that URL
                throw new Exception("Nandina does not yet support http/https value sets.");
            } else {
                File valueSetFile = ResourceUtils.getFile(valueSetUri);
                valueSetCodes = TerminologyHelper.extractCodes(FhirContext.forR4(), valueSetFile);
            }
        } catch (Exception ex) {
            logger.error("Could not load/extract codes for " + valueSetUri, ex);
        }

        logger.info(String.format("Found %s concepts in ValueSet %s", valueSetCodes.split(",").length, valueSetUri));
        return valueSetCodes;
    }

    public String getTerminologyCovidCodes() {
        if (Helper.isNullOrEmpty(this.terminologyCovidCodes)) {
            this.terminologyCovidCodes = loadValueSet(this.getTerminology().get(COVID_CODES_VALUE_SET));
        }
        return this.terminologyCovidCodes;
    }

    public String getTerminologyVentilatorCodes() {
        if (Helper.isNullOrEmpty(this.terminologyVentilatorCodes)) {
            this.terminologyVentilatorCodes = loadValueSet(this.getTerminology().get(VENTILATOR_CODES_VALUESET));
        }
        return terminologyVentilatorCodes;
    }

    public String getTerminologyIntubationProcedureCodes() {
        if (Helper.isNullOrEmpty(this.terminologyIntubationProcedureCodes)) {
            this.terminologyIntubationProcedureCodes = loadValueSet(this.getTerminology().get(INTUBATION_PROCEDURE_CODES_VALUESET));
        }
        return this.terminologyIntubationProcedureCodes;
    }

    public String getPrepareQuery() {
        return prepareQuery;
    }

    public void setPrepareQuery(String prepareQuery) {
        this.prepareQuery = prepareQuery;
    }
}
