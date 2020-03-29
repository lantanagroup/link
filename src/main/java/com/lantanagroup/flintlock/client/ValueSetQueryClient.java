package com.lantanagroup.flintlock.client;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Condition;
import org.hl7.fhir.r4.model.ValueSet;
import org.hl7.fhir.r4.model.ValueSet.ValueSetComposeComponent;
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
	int vsChunkSize = 2;
	
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
	
	public Bundle genericQuery (IGenericClient client, String searchUrl) {
		String base = client.getServerBase();
		String fullyResolvedSearchUrl;
		fullyResolvedSearchUrl = "http://" + base + "/" + searchUrl;
		logger.info("Issuing query: {}", fullyResolvedSearchUrl);
		Bundle response = targetClient.search()
			      .byUrl(searchUrl)
			      .returnBundle(Bundle.class)
			      .execute();
		return response;
	}
	
	public List<Condition> conditionCodeQuery(ValueSet vs) {
		List<Condition> conditions = new ArrayList<Condition>();
		// TODO issue query and return results. 
		return conditions;
	}
	
	public List<Bundle> chunkedQuery(ValueSet vs) {
		if (vs.hasExpansion() == false) {
			logger.info("Expanding value set {}", vs.getUrl());
			expand(vs);
		}
		List<Bundle> resultList = new ArrayList<Bundle>();
		List<List<ValueSet.ValueSetExpansionContainsComponent>> chunks = chunkValueSet(vs);
		for (List<ValueSet.ValueSetExpansionContainsComponent> chunk: chunks) {
			String searchCodes = chunkToString(chunk);
			String queryString = "Condition?code=" + searchCodes;
			Bundle result = genericQuery(targetClient,queryString);
			resultList.add(result);
		}
		return resultList;
	}
	
	private void expand(ValueSet vs) {
		// TODO Auto-generated method stub
		ValueSet.ValueSetExpansionComponent expansion = new ValueSet.ValueSetExpansionComponent();
		ValueSetComposeComponent compose = vs.getCompose();
		List<ValueSet.ConceptSetComponent> include = compose.getInclude();
		for (ValueSet.ConceptSetComponent csc : include) {
			String system = csc.getSystem();
			List<ValueSet.ConceptReferenceComponent> concepts = csc.getConcept();
			for (ValueSet.ConceptReferenceComponent concept : concepts) {
				String code = concept.getCode();
				ValueSet.ValueSetExpansionContainsComponent vsecc = expansion.addContains();
				vsecc.setSystem(system);
				vsecc.setCode(code);
			}
		}
		vs.setExpansion(expansion);
		
	}

	private String chunkToString(List<ValueSet.ValueSetExpansionContainsComponent> chunk) {
		StringBuffer searchCodes = new StringBuffer();
		boolean first = true;
		for (ValueSet.ValueSetExpansionContainsComponent comp : chunk) {
			String system = comp.getSystem();
			String code = comp.getCode();
			if (first) {
				first = false;
			} else {
				searchCodes.append(",");
			}
			searchCodes.append(system + "|" + code);
		}
		return searchCodes.toString();
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
