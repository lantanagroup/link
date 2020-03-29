package com.lantanagroup.flintlock;

import java.util.List;

import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.ValueSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.lantanagroup.flintlock.client.ValueSetQueryClient;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;

@RestController
public class FlintlockController {
	
	private static final Logger logger = LoggerFactory.getLogger(FlintlockController.class);
	FhirContext ctx = FhirContext.forR4();
	IParser xmlParser = ctx.newXmlParser();
	IParser jsonParser = ctx.newJsonParser();
	String conformanceServerBase = "https://flintlock-fhir.lantanagroup.com/fhir";
	String targetServerBase = "http://hapi.fhir.org/baseR4";
	ValueSetQueryClient vsClient;
	String symptomsValueSetUrl = "http://flintlock-fhir.lantanagroup.com/fhir/ValueSet/symptoms";
	
	
	public FlintlockController() {
		vsClient = new ValueSetQueryClient(conformanceServerBase, targetServerBase);
	}
	
	@RequestMapping("fhir")
	public String transaction (@RequestBody String resourceStr) throws JsonProcessingException {
		IBaseResource resource; 
		boolean isJson = false;
		if (resourceStr.startsWith("{")) {
			resource = jsonParser.parseResource(resourceStr);
			isJson = true;
		} else {
			resource = xmlParser.parseResource(resourceStr);
		}
		String parsedResource = xmlParser.encodeResourceToString(resource);
		logger.info(parsedResource);
		return parsedResource;
	}
	
	@GetMapping("report")
	public String report() {
		ValueSet symptomsVs = vsClient.getValueSet(symptomsValueSetUrl);
		logger.info("Retrieved value set", symptomsVs.getUrl());
		List<Bundle> resultList = vsClient.chunkedQuery(symptomsVs);
		StringBuffer buffy = new StringBuffer();
		for (Bundle b : resultList) {
			buffy.append(xmlParser.encodeResourceToString(b));
		}
		return buffy.toString();
	}
}
