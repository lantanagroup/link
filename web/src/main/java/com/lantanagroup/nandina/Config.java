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
    private String terminologyIntubationProcedureCodes;

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
    
    @Value("${terminology.intubationProcedureCodesValueSet}")
	private String terminologyIntubationProcedureCodesValueSet;

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

    @Value("${query.hospitalBeds}")
    private String queryHospitalBeds;

    @Value("${query.hospitalInpatientBeds}")
    private String queryHospitalInpatientBeds;

    @Value("${query.hospitalInpatientBedOcc}")
    private String queryHospitalInpatientBedOcc;

    @Value("${query.hospitalIcuBeds}")
    private String queryHospitalIcuBeds;

    @Value("${query.hospitalIcuBedOcc}")
    private String queryHospitalIcuBedOcc;

    @Value("${query.mechanicalVentilators}")
    private String queryMechanicalVentilators;

    @Value("${query.mechanicalVentilatorsUsed}")
    private String queryMechanicalVentilatorsUsed;

    @Value("${field.default.facilityId}")
    private String fieldDefaultFacilityId;

    @Value("${field.default.summaryCensusId}")
    private String fieldDefaultSummaryCensusId;

    @Value("${remember.fields}")
    private String rememberFields;

    @Value("${requireHttps}")
    private boolean requireHttps;

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
        if (Helper.isNullOrEmpty(terminologyVentilatorCodes)) {
        	terminologyVentilatorCodes = loadValueSet(this.getTerminologyVentilatorCodesValueSet());
        	/*
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
            */
        }
        return terminologyVentilatorCodes;
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

    @Override
    public String getTerminologyCovidCodes() {
        if (Helper.isNullOrEmpty(this.terminologyCovidCodes)) {
        	terminologyCovidCodes = loadValueSet(this.getTerminologyCovidCodesValueSet());
        	/*
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
            */
        }
        return this.terminologyCovidCodes;
    }

    @Override
    public String getTerminologyCovidCodesValueSet() {
        return this.terminologyCovidCodesValueSet;
    }
    


	@Override
	public String getTerminologyIntubationProcedureCodes() {
        if (Helper.isNullOrEmpty(terminologyIntubationProcedureCodes)) {
        	terminologyIntubationProcedureCodes = loadValueSet(this.getTerminologyIntubationProcedureCodesValueSet());
        }
        return this.terminologyIntubationProcedureCodes;
	}

	@Override
	public String getTerminologyIntubationProcedureCodesValueSet() {
        return this.terminologyIntubationProcedureCodesValueSet;
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

    public String getQueryHospitalBeds() {
        return queryHospitalBeds;
    }

    public void setQueryHospitalBeds(String queryHospitalBeds) {
        this.queryHospitalBeds = queryHospitalBeds;
    }

    public String getQueryHospitalIcuBeds() {
        return queryHospitalIcuBeds;
    }

    public void setQueryHospitalIcuBeds(String queryHospitalIcuBeds) {
        this.queryHospitalIcuBeds = queryHospitalIcuBeds;
    }

    public String getQueryHospitalIcuBedOcc() {
        return queryHospitalIcuBedOcc;
    }

    public void setQueryHospitalIcuBedOcc(String queryHospitalIcuBedOcc) {
        this.queryHospitalIcuBedOcc = queryHospitalIcuBedOcc;
    }

    public String getQueryHospitalInpatientBeds() {
        return queryHospitalInpatientBeds;
    }

    public void setQueryHospitalInpatientBeds(String queryHospitalInpatientBeds) {
        this.queryHospitalInpatientBeds = queryHospitalInpatientBeds;
    }

    public String getQueryHospitalInpatientBedOcc() {
        return queryHospitalInpatientBedOcc;
    }

    public void setQueryHospitalInpatientBedOcc(String queryHospitalInpatientBedOcc) {
        this.queryHospitalInpatientBedOcc = queryHospitalInpatientBedOcc;
    }

    public String getQueryMechanicalVentilators() {
        return queryMechanicalVentilators;
    }

    public void setQueryMechanicalVentilators(String queryMechanicalVentilators) {
        this.queryMechanicalVentilators = queryMechanicalVentilators;
    }

    public String getQueryMechanicalVentilatorsUsed() {
        return queryMechanicalVentilatorsUsed;
    }

    public void setQueryMechanicalVentilatorsUsed(String queryMechanicalVentilatorsUsed) {
        this.queryMechanicalVentilatorsUsed = queryMechanicalVentilatorsUsed;
    }

    public boolean getRequireHttps() {
        return requireHttps;
    }

    public void setRequireHttps(boolean requireHttps) {
        this.requireHttps = requireHttps;
    }
}