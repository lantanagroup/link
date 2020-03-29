package com.lantanagroup.flintlock.ecr;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import org.hl7.fhir.r4.model.*;
import org.hl7.fhir.r4.model.Composition.SectionComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

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
	IGenericClient client;
	String subjectId;
	
	public ElectronicCaseReport(IGenericClient client, Patient subject, Encounter encounter, Practitioner author) {
		this.client = client;
		this.subjectId = subject.getIdElement().getIdPart();

		CodeableConcept type = new CodeableConcept();
		Coding typeCoding = type.addCoding();
		typeCoding.setCode("55751-2");

		this.ecr.setId(UUID.randomUUID().toString());
		this.ecr.setIdentifier(new Identifier());
		this.ecr.getIdentifier().setValue(UUID.randomUUID().toString());
		this.ecr.setDate(new Date());
		this.ecr.setType(type);
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

		if (encounter == null) {
			encounter = new Encounter();
			encounter.setStatus(Encounter.EncounterStatus.UNKNOWN);
			CodeableConcept cc = new CodeableConcept();
			Coding encounterType = cc.addCoding();
			encounterType.setCode("PHC2237");
			encounterType.setSystem("urn:oid:2.16.840.1.114222.4.5.274");
			encounterType.setDisplay("External Encounter");
			List<CodeableConcept> ccList = new ArrayList<CodeableConcept>();
			ccList.add(cc);
			encounter.setType(ccList);
		}

		String encounterRef = this.addResource(encounter);
		this.ecr.setEncounter(new Reference(encounterRef));

		if (author != null) {
			String authorRef = this.addResource(author);
			this.ecr.addAuthor(new Reference(authorRef));
		}

		this.findProblems();
	}

	private void findProblems() {
		if (this.client == null) return;

		Bundle bundle = (Bundle) this.client.search()
				.forResource(Condition.class)
				.and(Condition.SUBJECT.hasId(this.subjectId))
				.execute();

		for (Bundle.BundleEntryComponent entry : bundle.getEntry()) {
			String ref = this.addResource((DomainResource) entry.getResource());
			this.problems.addEntry(new Reference(ref));
		}
	}

	private String addResource(DomainResource resource) {
		resource.setId(UUID.randomUUID().toString());
		this.resources.add(resource);
		return "urn:uuid:" + resource.getIdElement().getIdPart();
	}
	
	private SectionComponent addSection(String title, String code) {
		SectionComponent section = ecr.addSection();
		section.setTitle(title);
		CodeableConcept cc = new CodeableConcept();
		Coding c = cc.addCoding();
		c.setCode(code);
		c.setSystem(LOINC_CODE_SYSTEM);
		section.setCode(cc);
		return section;
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
		entry.setFullUrl("urn:uuid:" + resource.getIdElement().getIdPart());
		entry.setResource(resource);
		return entry;
	}
}
