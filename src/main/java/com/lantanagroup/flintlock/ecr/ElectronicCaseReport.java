package com.lantanagroup.flintlock.ecr;

import java.util.List;

import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Composition;
import org.hl7.fhir.r4.model.Composition.SectionComponent;
import org.hl7.fhir.r4.model.Patient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;

public class ElectronicCaseReport {
	
	private static final Logger logger = LoggerFactory.getLogger(ElectronicCaseReport.class);
	public static final String LOINC_CODE_SYSTEM = "http://loinc.org";
	FhirContext ctx = FhirContext.forR4();
	IParser xmlParser = ctx.newXmlParser();
	IParser jsonParser = ctx.newJsonParser();
	Composition ecr = new Composition();
	SectionComponent problemSection;
	
	public ElectronicCaseReport() {
		problemSection = addSection("Problems Section", "11450-4");
		// TODO: add remaining required sections from http://hl7.org/fhir/uv/ecr/2018Jan/StructureDefinition-eicr-composition.html
	}
	
	private SectionComponent addSection(String title, String loincCode) {
		SectionComponent section = ecr.addSection();
		CodeableConcept cc = new CodeableConcept();
		Coding c = cc.addCoding();
		c.setCode(loincCode);
		c.setSystem(LOINC_CODE_SYSTEM);
		return section;
	}
	
	public void setSubject(Patient p) {
		// TODO: set Composition.subject to the Patient
	}
	
	public void setProblemSectionEntries(List Condition) {
		// TODO: add all Condition resources to the problemSection
	}
	
	// TODO: Add methods for populating entries for remaining sections. 

}
