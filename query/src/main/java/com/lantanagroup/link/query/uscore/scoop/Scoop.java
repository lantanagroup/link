package com.lantanagroup.link.query.uscore.scoop;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import com.lantanagroup.link.query.uscore.PatientData;
import lombok.Getter;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.r4.model.Bundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Getter
@Component
public abstract class Scoop {
	protected static final Logger logger = LoggerFactory.getLogger(Scoop.class);
	protected PatientData patientData;
	protected Date reportDate = null;

	public static List<Bundle> rawSearch(IGenericClient fhirClient, String query) {
		int interceptors = 0;

		if (fhirClient.getInterceptorService() != null) {
			interceptors = fhirClient.getInterceptorService().getAllRegisteredInterceptors().size();
		}

		try {
			logger.info("Executing query: " + query);

			if (fhirClient == null) {
				logger.error("Client is null");
			}

			List<Bundle> retBundles = new ArrayList<>();
			Bundle retBundle = fhirClient.search()
					.byUrl(query)
					.returnBundle(Bundle.class)
					.execute();
			retBundles.add(retBundle);
			while (retBundle.getLink(IBaseBundle.LINK_NEXT) != null) {
				retBundle = fhirClient.loadPage()
						.next(retBundle)
						.execute();
				retBundles.add(retBundle);
			}
			int count = retBundles.stream()
					.mapToInt(b -> b.getEntry().size())
					.sum();
			logger.info(query + " Found " + count + " resources");
			return retBundles;
		} catch (Exception ex) {
			logger.error("Could not retrieve \"" + query + "\" from FHIR server " + fhirClient.getServerBase() + " with " + interceptors + ": " + ex.getMessage(), ex);
		}

		return null;
	}
}
