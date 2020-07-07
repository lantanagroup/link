package com.lantanagroup.nandina.query;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;

import com.lantanagroup.nandina.Helper;
import com.lantanagroup.nandina.JsonProperties;
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
	
	public static void initializeQueryCount(IQueryCountExecutor instance, JsonProperties jsonProperties, IGenericClient fhirClient, Map<String, String> criteria, Map<String, Object> contextData) throws ClassNotFoundException, NoSuchMethodException, SecurityException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		if (Helper.isNullOrEmpty(jsonProperties.getTerminologyCovidCodes())) {
			logger.error(NO_COVID_CODES_ERROR);
			throw new RuntimeException(NO_COVID_CODES_ERROR);
		}

		if (Helper.isNullOrEmpty(jsonProperties.getTerminologyVentilatorCodes())) {
			logger.error(NO_DEVICE_CODES_ERROR);
			throw new RuntimeException(NO_DEVICE_CODES_ERROR);
		}

		instance.setProperties(jsonProperties);
		instance.setFhirClient(fhirClient);
		instance.setCriteria(criteria);
		instance.setContextData(contextData);
	}

	public static Object executeQuery(String className, JsonProperties jsonProperties, IGenericClient fhirClient, Map<String, String> criteria, Map<String, Object> contextData) throws ClassNotFoundException, InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
		Class theClass = Class.forName(className);

		if (theClass == null) return null;

		Object instance = theClass.getConstructor().newInstance();

		if (instance instanceof IQueryCountExecutor) {
			IQueryCountExecutor queryCountExecutor = (IQueryCountExecutor) instance;
			initializeQueryCount(queryCountExecutor, jsonProperties, fhirClient, criteria, contextData);
			return queryCountExecutor.execute();
		}

		// Add additional "else if" interface checks for other types of questions

		return null;
	}
}
