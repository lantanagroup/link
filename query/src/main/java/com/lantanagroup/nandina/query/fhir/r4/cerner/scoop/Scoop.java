package com.lantanagroup.nandina.query.fhir.r4.cerner.scoop;

import com.lantanagroup.nandina.query.fhir.r4.cerner.PatientData;

import ca.uhn.fhir.rest.client.api.IGenericClient;

import java.util.Date;
import java.util.List;

import org.hl7.fhir.r4.model.Bundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class Scoop {

	protected static final Logger logger = LoggerFactory.getLogger(Scoop.class);

	protected List<PatientData> patientData;
	protected Date reportDate = null;

	public List<PatientData> getPatientData() {
		return patientData;
	}

	public Date getReportDate() {
		return reportDate;
	}

	public Bundle rawSearch(IGenericClient fhirServer, String query) {
		try {
			logger.info("Executing query: " + query);

			if (fhirServer == null) {
				logger.error("Client is null");
			}

			return fhirServer.search()
							.byUrl(query)
							.returnBundle(Bundle.class)
							.execute();
		} catch (Exception ex) {
			this.logger.error("Could not retrieve data for " + this.getClass().getName() + " with query " + query + ": " + ex.getMessage(), ex);
		}

		return null;
	}

}
