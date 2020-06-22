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

    @Override
    public String getFhirServerQueryBase() {
        return null;
    }

    @Override
    public String getFhirServerUserName() {
        return null;
    }

    @Override
    public String getFhirServerPassword() {
        return null;
    }

    @Override
    public String getFhirServerBearerToken() {
        return null;
    }

    @Override
    public String getTerminologyCovidCodesValueSet() {
        return null;
    }

    @Override
    public String getTerminologyVentilatorCodesValueSet() {
        return null;
    }

    @Override
    public String getTerminologyIntubationProcedureCodesValueSet() {
        return null;
    }

    @Override
    public String getTerminologyVentilatorCodes() {
        return null;
    }

    @Override
    public String getTerminologyCovidCodes() {
        return null;
    }

    @Override
    public String getTerminologyIntubationProcedureCodes() {
        return null;
    }

    @Override
    public String getQueryHospitalized() {
        return null;
    }

    @Override
    public String getQueryEDOverflow() {
        return null;
    }
}