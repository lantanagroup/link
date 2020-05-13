package com.lantanagroup.nandina;

import ca.uhn.fhir.context.FhirContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.ResourceUtils;

import java.io.File;

@Component
public class Config implements IConfig {
    protected static final Logger logger = LoggerFactory.getLogger(Config.class);

    private static Config config;
    private String terminologyCovidCodes;
    private String terminologyVentilatorCodes;

    public static Config getInstance() {
        if (config == null) {
            config = new Config();
        }
        return config;
    }

    public static void setInstance(Config instance) {
        config = instance;
    }

    @Value("${export.format}")
    private String exportFormat;

    @Value("${auth.issuer}")
    private String authIssuer;

    @Value("${auth.jwksUrl}")
    private String authJwksUrl;

    @Value("${auth.clientId}")
    private String authClientId;

    @Value("${auth.scope}")
    private String authScope;

    @Value("${fhirServer.base}")
    private String fhirServerBase;

    @Value("${fhirServer.username}")
    private String fhirServerUserName;

    @Value("${fhirServer.password}")
    private String fhirServerPassword;

    @Value("${fhirServer.bearerToken}")
    private String fhirServerBearerToken;

    @Value("${terminology.covidCodesValueSet}")
    private String terminologyCovidCodesValueSet;

    @Value("${terminology.ventilatorCodesValueSet}")
    private String terminologyVentilatorCodesValueSet;

    @Value("${query.hospitalized}")
    private String queryHospitalized;

    @Value("${query.hospitalizedAndVentilated}")
    private String queryHospitalizedAndVentilated;

    @Value("${query.hospitalOnset}")
    private String queryHospitalOnset;

    @Value("${query.edOverflow}")
    private String queryEDOverflow;

    @Value("${query.edOverflowAndVentilated}")
    private String queryEDOverflowAndVentilated;

    @Value("${query.deaths}")
    private String queryDeaths;

    @Value("${field.default.facilityId}")
    private String fieldDefaultFacilityId;

    @Value("${field.default.summaryCensusId}")
    private String fieldDefaultSummaryCensusId;

    @Value("${remember.fields}")
    private String rememberFields;

    public String getQueryHospitalizedAndVentilated() {
        return this.queryHospitalizedAndVentilated;
    }

    @Override
    public String getQueryHospitalized() {
        return this.queryHospitalized;
    }

    @Override
    public String getTerminologyVentilatorCodesValueSet() {
        return this.terminologyVentilatorCodesValueSet;
    }

    @Override
    public String getTerminologyVentilatorCodes() {
        if (Helper.isNullOrEmpty(this.terminologyVentilatorCodes)) {
            this.logger.info("Extracting mechanical ventilator concepts from ValueSet");
            try {
                if (this.getTerminologyVentilatorCodesValueSet().startsWith("https://") || this.getTerminologyVentilatorCodesValueSet().startsWith("http://")) {
                    // TODO: pull the value set from the web
                    throw new Exception("Nandina does not yet support http/https value sets.");
                } else {
                    File mechanicalVentilatorsFile = ResourceUtils.getFile(config.getTerminologyVentilatorCodesValueSet());
                    this.terminologyVentilatorCodes = TerminologyHelper.extractCodes(FhirContext.forR4(), mechanicalVentilatorsFile);
                }
            } catch (Exception ex) {
                this.logger.error("Could not load/extract codes for concept of mechanical ventilators", ex);
            }

            this.logger.info(String.format("Found %s mechanical ventilator concepts in ValueSet", this.terminologyVentilatorCodes.split(",").length));
        }
        return this.terminologyVentilatorCodes;
    }

    @Override
    public String getTerminologyCovidCodes() {
        if (Helper.isNullOrEmpty(this.terminologyCovidCodes)) {
            this.logger.info("Extracting COVID-19 concepts from ValueSet");
            try {
                if (this.getTerminologyCovidCodesValueSet().startsWith("https://") || this.getTerminologyCovidCodesValueSet().startsWith("http://")) {
                    // TODO: pull the value set from the web
                    throw new Exception("Nandina does not yet support http/https value sets.");
                } else {
                    File covidCodesFile = ResourceUtils.getFile(config.getTerminologyCovidCodesValueSet());
                    this.terminologyCovidCodes = TerminologyHelper.extractCodes(FhirContext.forR4(), covidCodesFile);
                }
            } catch (Exception ex) {
                this.logger.error("Could not load/extract codes for concept of COVID-19", ex);
            }

            this.logger.info(String.format("Found %s COVID-19 concepts in ValueSet", this.terminologyCovidCodes.split(",").length));
        }
        return this.terminologyCovidCodes;
    }

    @Override
    public String getTerminologyCovidCodesValueSet() {
        return this.terminologyCovidCodesValueSet;
    }

    @Override
    public String getFhirServerBearerToken() {
        return this.fhirServerBearerToken;
    }

    @Override
    public String getFhirServerPassword() {
        return this.fhirServerPassword;
    }

    @Override
    public String getFhirServerUserName() {
        return this.fhirServerUserName;
    }

    @Override
    public String getFhirServerBase() {
        return this.fhirServerBase;
    }

    public String getQueryHospitalOnset() {
        return this.queryHospitalOnset;
    }

    public String getQueryEDOverflow() {
        return this.queryEDOverflow;
    }

    public String getRememberFields() {
        return this.rememberFields;
    }

    public String getQueryEDOverflowAndVentilated() {
        return this.queryEDOverflowAndVentilated;
    }

    public String getQueryDeaths() {
        return this.queryDeaths;
    }

    public String getAuthScope() {
        return this.authScope;
    }

    public String getAuthClientId() {
        return this.authClientId;
    }

    public String getAuthJwksUrl() {
        return this.authJwksUrl;
    }

    public String getAuthIssuer() {
        return this.authIssuer;
    }

    public String getExportFormat() {
        return this.exportFormat;
    }

    public String getFieldDefaultFacilityId() {
        return this.fieldDefaultFacilityId;
    }

    public String getFieldDefaultSummaryCensusId() {
        return this.fieldDefaultSummaryCensusId;
    }
}