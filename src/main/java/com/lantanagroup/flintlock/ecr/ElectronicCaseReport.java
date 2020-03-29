package com.lantanagroup.flintlock.ecr;

import java.util.List;

import org.hl7.fhir.r4.model.*;
import org.hl7.fhir.r4.model.Composition.SectionComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;

public class ElectronicCaseReport {
	
	private static final Logger logger = LoggerFactory.getLogger(ElectronicCaseReport.class);
	public static final String LOINC_CODE_SYSTEM = "http://loinc.org";
	FhirContext ctx = FhirContext.forR4();
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
		this.ecr.addSection(section);
		return section;
	}
	
	public void setSubject(Patient p) {
		// TODO: set Composition.subject to the Patient
	}
	
	public void setProblemSectionEntries(List Condition) {
		// TODO: add all Condition resources to the problemSection
	}
	
	// TODO: Add methods for populating entries for remaining sections.


	public Bundle compile() {
		Bundle doc = new Bundle();
		doc.setType(Bundle.BundleType.DOCUMENT);

		// Composition first
		doc.addEntry(createBundleEntry(this.ecr));

		doc.setTotal(doc.getEntry().size());

		return doc;
	}

	private static Bundle.BundleEntryComponent createBundleEntry(DomainResource resource) {
		Bundle.BundleEntryComponent entry = new Bundle.BundleEntryComponent();
		entry.setResource(resource);
		return entry;
	}
}
