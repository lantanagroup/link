package com.lantanagroup.flintlock;

import org.hl7.fhir.instance.model.api.IBaseResource;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;

@RestController
public class FlintlockController {

	FhirContext ctx = FhirContext.forR4();
	IParser xmlParser = ctx.newXmlParser();
	IParser jsonParser = ctx.newJsonParser();
	
	@RequestMapping("/fhir")
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
		System.out.println(parsedResource);
		return parsedResource;
	}
	

	
	@RequestMapping("/test")
	public String test (@RequestBody String resourceStr) throws JsonProcessingException {
		return "Hello";
	}
	
}
