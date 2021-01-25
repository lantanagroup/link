package com.lantanagroup.nandina.query.scoop;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.server.exceptions.AuthenticationException;
import com.lantanagroup.nandina.query.PatientData;
import lombok.Getter;
import org.hl7.fhir.r4.model.Bundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.List;

@Getter
public abstract class Scoop {
	protected static final Logger logger = LoggerFactory.getLogger(Scoop.class);
	protected List<PatientData> patientData;
	protected Date reportDate = null;

	public Bundle rawSearch(IGenericClient fhirClient, String query) {
		int interceptors = 0;

		if (fhirClient.getInterceptorService() != null) {
			interceptors = fhirClient.getInterceptorService().getAllRegisteredInterceptors().size();
		}

		try {
			logger.info("Executing query: " + query);

			if (fhirClient == null) {
				logger.error("Client is null");
			}

			Bundle retBundle = fhirClient.search()
					.byUrl(query)
					.returnBundle(Bundle.class)
					.execute();
			logger.info(query + " Found " + retBundle.getEntry().size() + " resources");
			return retBundle;
		} catch (AuthenticationException ae) {
			logger.error("Unable to perform rawSearch() with query " + query + " response body: " + ae.getResponseBody());
			ae.printStackTrace();
		} catch (Exception ex) {
			this.logger.error("Could not retrieve data from FHIR server " + fhirClient.getServerBase() + " with " + interceptors + " interceptors for " + this.getClass().getName() + ": " + ex.getMessage(), ex);
		}

		return null;
	}
}
