package com.lantanagroup.nandina.query;

import com.lantanagroup.nandina.Helper;
import com.lantanagroup.nandina.IConfig;

import ca.uhn.fhir.rest.client.api.IGenericClient;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractQuery {

    protected static final Logger logger = LoggerFactory.getLogger(AbstractQuery.class);
	private static final String NO_COVID_CODES_ERROR = "Covid codes have not been specified in configuration. Cannot execute queries.";
	
	protected IConfig config;
	protected IGenericClient fhirClient;
	
	public AbstractQuery(IConfig config, IGenericClient fhirClient) {
		this.config = config;
		this.fhirClient = fhirClient;
        if (Helper.isNullOrEmpty(config.getTerminologyCovidCodes())) {
            this.logger.error(NO_COVID_CODES_ERROR);
            throw new RuntimeException(NO_COVID_CODES_ERROR);
        }
	}

}
