package com.lantanagroup.flintlock;

import java.util.HashSet;
import java.util.List;

import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Condition;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Resource;
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
	public String conformanceServerBase = "https://flintlock-fhir.lantanagroup.com/fhir";
	public String targetServerBase = "http://hapi.fhir.org/baseR4";
	FhirContext ctx = FhirContext.forR4();
	IParser xmlParser = ctx.newXmlParser();
	IParser jsonParser = ctx.newJsonParser();
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
	
	@GetMapping(value = "report", produces = "application/xml")
	public String report() {
		ValueSet symptomsVs = vsClient.getValueSet(symptomsValueSetUrl);
		logger.info("Retrieved value set", symptomsVs.getUrl());
		List<Condition> resultList = vsClient.conditionCodeQuery(symptomsVs);
		StringBuffer buffy = new StringBuffer();
		buffy.append("<Conditions>");
		for (Condition c : resultList) {
			buffy.append(xmlParser.encodeResourceToString(c));
		}

		buffy.append("</Conditions>");
		return buffy.toString();
	}
	
	private HashSet<Patient> getUniquePatients(List<Condition> c){
		return null;
	}
}
