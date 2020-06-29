package com.lantanagroup.nandina.query;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;

import com.lantanagroup.nandina.Helper;
import com.lantanagroup.nandina.JsonProperties;
import com.lantanagroup.nandina.query.fhir.r4.AbstractQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.uhn.fhir.rest.client.api.IGenericClient;

public class QueryFactory {
	protected static final Logger logger = LoggerFactory.getLogger(QueryFactory.class);
	private static final String NO_DEVICE_CODES_ERROR = "Device-type codes have not been specified in configuration.";
	private static final String NO_COVID_CODES_ERROR = "Covid codes have not been specified in configuration.";

	public static IPrepareQuery newPrepareQueryInstance(String className, JsonProperties jsonProperties, IGenericClient fhirClient, Map<String, String> criteria, Map<String, Object> contextData) throws IllegalAccessException, InvocationTargetException, InstantiationException, NoSuchMethodException, ClassNotFoundException {
		logger.trace("Creating new prepare-query object: " + className);

		IPrepareQuery prepareQuery;
		Class<?> prepareQueryClass = Class.forName(className);
		Constructor<?> queryConstructor = prepareQueryClass.getConstructor();
		prepareQuery = (IPrepareQuery) queryConstructor.newInstance();

		prepareQuery.setProperties(jsonProperties);
		prepareQuery.setFhirClient(fhirClient);
		prepareQuery.setCriteria(criteria);
		prepareQuery.setContextData(contextData);

		return prepareQuery;
	}
	
	public static IQueryCountExecutor newQueryCountInstance(String className, JsonProperties jsonProperties, IGenericClient fhirClient, Map<String, String> criteria, Map<String, Object> contextData) throws ClassNotFoundException, NoSuchMethodException, SecurityException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		logger.trace("Creating new query object: " + className);

		if (Helper.isNullOrEmpty(jsonProperties.getTerminologyCovidCodes())) {
			logger.error(NO_COVID_CODES_ERROR);
			throw new RuntimeException(NO_COVID_CODES_ERROR);
		}

		if (Helper.isNullOrEmpty(jsonProperties.getTerminologyVentilatorCodes())) {
			logger.error(NO_DEVICE_CODES_ERROR);
			throw new RuntimeException(NO_DEVICE_CODES_ERROR);
		}

		IQueryCountExecutor query;
		Class<?> queryClass = Class.forName(className);
		Constructor<?> queryConstructor = queryClass.getConstructor();
		query = (AbstractQuery) queryConstructor.newInstance();

		query.setProperties(jsonProperties);
		query.setFhirClient(fhirClient);
		query.setCriteria(criteria);
		query.setContextData(contextData);

		return query;
	}
}
