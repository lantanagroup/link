package com.lantanagroup.nandina.query;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;

import com.lantanagroup.nandina.JsonProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.uhn.fhir.rest.client.api.IGenericClient;

public class QueryFactory {
	protected static final Logger logger = LoggerFactory.getLogger(QueryFactory.class);

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

	public static IFormQuery newFormQueryInstance(String className, JsonProperties jsonProperties, IGenericClient fhirClient, Map<String, String> criteria, Map<String, Object> contextData) throws IllegalAccessException, InvocationTargetException, InstantiationException, NoSuchMethodException, ClassNotFoundException {
		logger.trace("Creating new form query object: " + className);

		IFormQuery formQuery;
		Class<?> formQueryClass = Class.forName(className);
		Constructor<?> queryConstructor = formQueryClass.getConstructor();
		formQuery = (IFormQuery) queryConstructor.newInstance();

		formQuery.setProperties(jsonProperties);
		formQuery.setFhirClient(fhirClient);
		formQuery.setCriteria(criteria);
		formQuery.setContextData(contextData);

		return formQuery;
	}
}
