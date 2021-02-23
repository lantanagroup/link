package com.lantanagroup.nandina;

import ca.uhn.fhir.context.FhirContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;
import org.springframework.util.ResourceUtils;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.Setter;
import lombok.Getter;

@Component
@PropertySource(value = "classpath:config.json", factory = JsonPropertySourceFactory.class)
@EnableConfigurationProperties
@ConfigurationProperties
@Getter @Setter
public class NandinaConfig {
    private static Logger logger = LoggerFactory.getLogger(NandinaConfig.class.getName());
    private static String terminologyCovidCodes;
    private static String terminologyVentilatorCodes;
    private static String terminologyIntubationProcedureCodes;

    public static final String COVID_CODES_VALUE_SET = "covidCodesValueSet";
    public static final String VENTILATOR_CODES_VALUESET = "ventilatorCodesValueSet";
    public static final String INTUBATION_PROCEDURE_CODES_VALUESET = "intubationProcedureCodesValueSet";
    public static final String DIRECT_URL = "url";
    public static final String DIRECT_USERNAME = "username";
    public static final String DIRECT_PASSWORD = "password";
    public static final String DIRECT_TO_ADDRESS = "toAddress";

    private String fhirServerQueryBase;
    private String fhirServerStoreBase;
    private Map<String, String> fhirServerQueryAuth;
    private String authJwksUrl;
    private String prepareQuery;
    private String formQuery;
    private String downloader;
    private String sender;
    private Map<String, String> terminology;
    private List<DefaultField> defaultField;
    private boolean requireHttps;
    private Map<String, String> direct;
    private Map<String, String> queryCriteria;
    private String sendUrl;
    private List<MeasureConfig> measureConfigs;
    private LinkedHashMap<String, String> measureLocation;

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

    public LocationConfig getMeasureLocationConfig() {
        if (this.getMeasureLocation() != null) {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.convertValue(this.getMeasureLocation(), LocationConfig.class);
        }
        return null;
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

}
