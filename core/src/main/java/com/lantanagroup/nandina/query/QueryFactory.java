package com.lantanagroup.nandina.query;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.lantanagroup.nandina.IConfig;

import ca.uhn.fhir.rest.client.api.IGenericClient;

public class QueryFactory {
	

	protected static final Logger logger = LoggerFactory.getLogger(QueryFactory.class);
	
	public static AbstractQuery newInstance(String className, IConfig config, IGenericClient fhirClient) throws ClassNotFoundException, NoSuchMethodException, SecurityException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		logger.info("Loading query class: " + className);
	      Class<?> queryClass = Class.forName(className);
	      Constructor<?> queryConstructor = queryClass.getConstructor(IConfig.class, IGenericClient.class);
	      AbstractQuery query = (AbstractQuery) queryConstructor.newInstance(config, fhirClient);
	      return query;
	}

}
