package com.lantanagroup.nandina.query;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;

import com.lantanagroup.nandina.NandinaConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.uhn.fhir.rest.client.api.IGenericClient;

public class QueryFactory {
	protected static final Logger logger = LoggerFactory.getLogger(QueryFactory.class);

	public static IPrepareQuery newPrepareQueryInstance(String className, NandinaConfig nandinaConfig, IGenericClient fhirClient, Map<String, String> criteria, Map<String, Object> contextData) throws IllegalAccessException, InvocationTargetException, InstantiationException, NoSuchMethodException, ClassNotFoundException {
		logger.debug("Creating new prepare-query object: " + className);

		IPrepareQuery prepareQuery;
		Class<?> prepareQueryClass = Class.forName(className);
		Constructor<?> queryConstructor = prepareQueryClass.getConstructor();
		prepareQuery = (IPrepareQuery) queryConstructor.newInstance();

		prepareQuery.setProperties(nandinaConfig);
		prepareQuery.setFhirClient(fhirClient);
		prepareQuery.setCriteria(criteria);
		prepareQuery.setContextData(contextData);

		return prepareQuery;
	}
}
