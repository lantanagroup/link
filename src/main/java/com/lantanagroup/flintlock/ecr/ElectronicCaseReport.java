package com.lantanagroup.flintlock.ecr;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.hl7.fhir.r4.model.*;
import org.hl7.fhir.r4.model.Composition.SectionComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ElectronicCaseReport {
	
	private static final Logger logger = LoggerFactory.getLogger(ElectronicCaseReport.class);
	public static final String LOINC_CODE_SYSTEM = "http://loinc.org";
	Composition ecr = new Composition();
	SectionComponent reasonForVisit;
	SectionComponent historyOfPresentIllness;
	SectionComponent problems;
	SectionComponent medicationsAdministered;
	SectionComponent results;
	SectionComponent planOfTreatment;
	SectionComponent immunizations;
	SectionComponent vitalSigns;
	SectionComponent socialHistory;
	List<DomainResource> resources = new ArrayList();
	
	public ElectronicCaseReport(Patient subject) {
		this.ecr.setId(UUID.randomUUID().toString());
		this.ecr.setSubject(new Reference(this.addResource(subject)));

		reasonForVisit = addSection("Reason for visit Narrative", "29299-5");
		historyOfPresentIllness = addSection("History of Present illness Narrative", "10164-2");
		problems = addSection("Problems Section", "11450-4");
		medicationsAdministered = addSection("Medications administered Narrative", "29549-3");
		results = addSection("Relevant diagnostic tests/laboratory data Narrative", "30954-2");
		planOfTreatment = addSection("Plan of Treatment Section", "18876-5");
		immunizations = addSection("History of Immunization Narrative", "11369-6");
		vitalSigns = addSection("Vital Signs Section", "8716-3");
		socialHistory = addSection("Social history Narrative", "29762-2");
	}

	private String addResource(DomainResource resource) {
		resource.setId(UUID.randomUUID().toString());
		this.resources.add(resource);
		return "urn:uuid:" + resource.getId();
	}
	
	private SectionComponent addSection(String title, String code) {
		SectionComponent section = ecr.addSection();
		CodeableConcept cc = new CodeableConcept();
		Coding c = cc.addCoding();
		c.setCode(code);
		c.setSystem(LOINC_CODE_SYSTEM);
		this.ecr.addSection(section);
		return section;
	}

	public void setProblemSectionEntries(List<Condition> conditions) {
		for (Condition condition : conditions) {
			String ref = this.addResource(condition);
			this.problems.addEntry(new Reference(ref));
		}
	}

	public void setMedicationsAdministeredEntries(List<MedicationStatement> medicationStatements) {
		for (MedicationStatement medicationStatement : medicationStatements) {
			String ref = this.addResource(medicationStatement);
			this.medicationsAdministered.addEntry(new Reference(ref));
		}
	}

	public void setResultsEntries(List<DiagnosticReport> diagnosticReports) {
		for (DiagnosticReport diagnosticReport : diagnosticReports) {
			String ref = this.addResource(diagnosticReport);
			this.results.addEntry(new Reference(ref));
		}
	}

	public void setPlanOfTreatmentServiceRequestEntries(List<ServiceRequest> serviceRequests) {
		for (ServiceRequest serviceRequest : serviceRequests) {
			String ref = this.addResource(serviceRequest);
			this.planOfTreatment.addEntry(new Reference(ref));
		}
	}

	public void setPlanOfTreatmentMedicationRequestEntries(List<MedicationRequest> medicationRequests) {
		for (MedicationRequest medicationRequest : medicationRequests) {
			String ref = this.addResource(medicationRequest);
			this.planOfTreatment.addEntry(new Reference(ref));
		}
	}

	public void setImmunizationsEntries(List<Immunization> immunizations) {
		for (Immunization immunization : immunizations) {
			String ref = this.addResource(immunization);
			this.immunizations.addEntry(new Reference(ref));
		}
	}

	public void setVitalSignsEntries(List<Observation> observations) {
		for (Observation observation : observations) {
			String ref = this.addResource(observation);
			this.vitalSigns.addEntry(new Reference(ref));
		}
	}

	public void setSocialHistoryEntries(List<Observation> observations) {
		for (Observation observation : observations) {
			String ref = this.addResource(observation);
			this.socialHistory.addEntry(new Reference(ref));
		}
	}
	
	// TODO: Add methods for populating entries for remaining sections.

	public Bundle compile() {
		Bundle doc = new Bundle();
		doc.setType(Bundle.BundleType.DOCUMENT);

		// Composition first
		doc.addEntry(createBundleEntry(this.ecr));

		for (DomainResource resource : this.resources) {
			doc.addEntry(createBundleEntry(resource));
		}

		// Set the total resources in the bundle
		doc.setTotal(doc.getEntry().size());

		return doc;
	}

	private static Bundle.BundleEntryComponent createBundleEntry(DomainResource resource) {
		Bundle.BundleEntryComponent entry = new Bundle.BundleEntryComponent();
		entry.setResource(resource);
		return entry;
	}
}
