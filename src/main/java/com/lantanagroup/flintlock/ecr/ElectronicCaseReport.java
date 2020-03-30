package com.lantanagroup.flintlock.ecr;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r4.model.*;
import org.hl7.fhir.r4.model.Composition.CompositionStatus;
import org.hl7.fhir.r4.model.Composition.SectionComponent;
import org.hl7.fhir.r4.model.Device.DeviceNameType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.HashMap;
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
    HashMap<String, DomainResource> resources = new HashMap<String, DomainResource>();
    IGenericClient client;
    String subjectId;

    public ElectronicCaseReport(IGenericClient client, Patient subject, Encounter encounter, PractitionerRole author) {
        this.client = client;
        this.subjectId = subject.getIdElement().getIdPart();

        CodeableConcept type = new CodeableConcept();
        Coding typeCoding = type.addCoding();
        typeCoding.setCode("55751-2");
        typeCoding.setSystem(LOINC_CODE_SYSTEM);

        this.ecr.setId(UUID.randomUUID().toString());
        this.ecr.setIdentifier(new Identifier());
        this.ecr.getIdentifier().setValue(UUID.randomUUID().toString());
        this.ecr.setDate(new Date());
        this.ecr.setStatus(CompositionStatus.PRELIMINARY);
        this.ecr.setType(type);
        this.ecr.setSubject(new Reference(this.addResource(subject)));
        this.ecr.setTitle("Initial Public Health Case Report" + subject.getId());

        reasonForVisit = addSection("Reason for visit Narrative", "29299-5");
        historyOfPresentIllness = addSection("History of Present illness Narrative", "10164-2");
        problems = addSection("Problems Section", "11450-4");
        medicationsAdministered = addSection("Medications administered Narrative", "29549-3");
        results = addSection("Relevant diagnostic tests/laboratory data Narrative", "30954-2");
        planOfTreatment = addSection("Plan of Treatment Section", "18876-5");
        immunizations = addSection("History of Immunization Narrative", "11369-6");
        vitalSigns = addSection("Vital Signs Section", "8716-3");
        socialHistory = addSection("Social history Narrative", "29762-2");

        // Decided to remove the following for now, since I believe the eCR spec is wrong,
        // and PHC2237 does not actually meet the definition of Encounter. Will talk to John and Sarah about it.
		
		/*
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
		*/

        if (author != null) {
            String authorRef = this.addResource(author);
            this.ecr.addAuthor(new Reference(authorRef));
        } else {
            Device device = new Device();
            device.addDeviceName().setName("Lantana Flintlock Case Reporting Service").setType(DeviceNameType.USERFRIENDLYNAME);
            String deviceRef = this.addResource(device);
            this.ecr.addAuthor(new Reference(deviceRef));
        }

        this.findProblems();
        this.findResults();
        this.findVitalSigns();
        this.findMedicationsAdministered();
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

    private void findResults() {
        if (this.client == null) return;

        Bundle bundle = (Bundle) this.client.search()
                .forResource(Observation.class)
                .and(Observation.SUBJECT.hasId(this.subjectId))
                .and(Observation.CATEGORY.exactly().code("laboratory"))
                .execute();

        for (Bundle.BundleEntryComponent entry : bundle.getEntry()) {
            String ref = this.addResource((DomainResource) entry.getResource());
            this.results.addEntry(new Reference(ref));
        }
    }

    private void findVitalSigns() {
        if (this.client == null) return;

        Bundle bundle = (Bundle) this.client.search()
                .forResource(Observation.class)
                .and(Observation.SUBJECT.hasId(this.subjectId))
                .and(Observation.CATEGORY.exactly().code("vital-signs"))
                .execute();

        for (Bundle.BundleEntryComponent entry : bundle.getEntry()) {
            String ref = this.addResource((DomainResource) entry.getResource());
            this.vitalSigns.addEntry(new Reference(ref));
        }
    }

    private void findMedicationsAdministered() {
        if (this.client == null) return;

        Bundle bundle = (Bundle) this.client.search()
                .forResource(MedicationAdministration.class)
                .and(Observation.SUBJECT.hasId(this.subjectId))
                .execute();

        for (Bundle.BundleEntryComponent entry : bundle.getEntry()) {
            String ref = this.addResource((DomainResource) entry.getResource());
            this.medicationsAdministered.addEntry(new Reference(ref));
        }
    }

    private String addResource(DomainResource resource) {
        // TODO: Find a way to preserve the original resource id and server url, maybe as an extension
        //	resource.setId(UUID.randomUUID().toString());
        if (resource.hasId() == false) {
            String uuid = UUID.randomUUID().toString();
            resource.setId("urn:uuid:" + UUID.randomUUID().toString());
        }
        this.resources.put(resource.getId(), resource);
        //	return "urn:uuid:" + resource.getIdElement().getIdPart();
        return resource.getId();
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

    public void setSocialHistoryEntries(List<Observation> observations) {
        for (Observation observation : observations) {
            String ref = this.addResource(observation);
            this.socialHistory.addEntry(new Reference(ref));
        }
    }

    public Bundle compile() {
        Bundle doc = new Bundle();
        String uuid = UUID.randomUUID().toString();
        doc.setId("urn:uuid:" + uuid);
        doc.setTimestamp(new Date());
        Identifier identifier = new Identifier();
        identifier.setSystem("urn:ietf:rfc:3986");
        identifier.setValue(uuid);
        doc.setIdentifier(identifier);
        doc.setType(Bundle.BundleType.DOCUMENT);

        // Composition first
        doc.addEntry(createBundleEntry(this.ecr));

        for (String key : this.resources.keySet()) {
            doc.addEntry(createBundleEntry(resources.get(key)));
        }

        // Set the total resources in the bundle
        // RG: actually don't: invariant bdl-1: total only when a search or history
        // doc.setTotal(doc.getEntry().size());
        return doc;
    }

    private static Bundle.BundleEntryComponent createBundleEntry(DomainResource resource) {
        Bundle.BundleEntryComponent entry = new Bundle.BundleEntryComponent();
        //	entry.setFullUrl("urn:uuid:" + resource.getIdElement().getIdPart());
        String fullUrl;
        if (resource.getId().contains("/_history")) {
            fullUrl = StringUtils.substringBefore(resource.getId(), "/_history");
        } else {
            fullUrl = resource.getId();
        }
        entry.setFullUrl(fullUrl);
        entry.setResource(resource);
        return entry;
    }
}
