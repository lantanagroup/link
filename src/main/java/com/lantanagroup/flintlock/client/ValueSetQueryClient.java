package com.lantanagroup.flintlock.client;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Condition;
import org.hl7.fhir.r4.model.ValueSet;
import org.hl7.fhir.r4.model.ValueSet.ValueSetExpansionComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.client.api.IGenericClient;

public class ValueSetQueryClient {
	
	private static final Logger logger = LoggerFactory.getLogger(ValueSetQueryClient.class);
	FhirContext ctx = FhirContext.forR4();
	IParser xmlParser = ctx.newXmlParser();
	IParser jsonParser = ctx.newJsonParser();
	IGenericClient targetClient;
	IGenericClient vsClient;
	int vsChunkSize = 10;
	
	public ValueSetQueryClient(String conformanceServerBase, String targetServerBase) {
		vsClient = ctx.newRestfulGenericClient(conformanceServerBase);
		targetClient = ctx.newRestfulGenericClient(targetServerBase);
		logger.info("Created clients for {}, {}", conformanceServerBase, targetServerBase);
	}
	
	public ValueSet getValueSet(String vsUrl) {
		// This should be fixed to search ValueSet?url=vsUrl, but shortcutting for hackathon since the phyisical URL is the same as the cannonical
		ValueSet vs = vsClient.read().resource(ValueSet.class).withUrl(vsUrl).execute();
		return vs;
	}
	
	public Bundle genericQuery (String searchUrl) {
		Bundle response = targetClient.search()
			      .byUrl(searchUrl)
			      .returnBundle(Bundle.class)
			      .execute();
		return response;
	}
	
	public Bundle conditionCodeQuery(ValueSet vs) {
		Bundle response = null;
		List<List<ValueSet.ValueSetExpansionContainsComponent>> chunks = chunkValueSet(vs);
		
		return response;
	}

	private List<List<ValueSet.ValueSetExpansionContainsComponent>> chunkValueSet(ValueSet vs) {
		List<List<ValueSet.ValueSetExpansionContainsComponent>> codeChunks = new ArrayList<List<ValueSet.ValueSetExpansionContainsComponent>>();
		ValueSetExpansionComponent expansion = vs.getExpansion();
		List<ValueSet.ValueSetExpansionContainsComponent> containsList = expansion.getContains();
		List<ValueSet.ValueSetExpansionContainsComponent> codeChunk = null;
		boolean remainingChunks = false;
		for (int i = 0 ; i < containsList.size() ; i++) {
			// break into chunks 
			if (i%vsChunkSize == 0) {
				remainingChunks = false;
				if (codeChunk != null) {
					codeChunks.add(codeChunk);
				} else {
					codeChunk = new ArrayList<ValueSet.ValueSetExpansionContainsComponent>();
				}
			} else {
				remainingChunks = true;
			}
			ValueSet.ValueSetExpansionContainsComponent comp = containsList.get(i);
			codeChunk.add(comp);
		}
		if (remainingChunks = true) {
			codeChunks.add(codeChunk);
		}
		return codeChunks;
	}
	

	

}
