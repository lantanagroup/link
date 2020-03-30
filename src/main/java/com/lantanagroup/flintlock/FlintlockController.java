package com.lantanagroup.flintlock;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.validation.*;
import ca.uhn.fhir.validation.schematron.SchematronBaseValidator;
import com.lantanagroup.flintlock.client.ValueSetQueryClient;
import com.lantanagroup.flintlock.ecr.ElectronicCaseReport;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.Bundle.BundleType;
import org.hl7.fhir.r4.model.Bundle.HTTPVerb;
import org.hl7.fhir.r4.model.Condition;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.ValueSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
public class FlintlockController {

    private static final Logger logger = LoggerFactory.getLogger(FlintlockController.class);
    public String conformanceServerBase = "https://flintlock-fhir.lantanagroup.com/fhir";
    public String clinicalDataServerBase = "http://hapi.fhir.org/baseR4";
    FhirContext ctx = FhirContext.forR4();
    IParser xmlParser = ctx.newXmlParser().setPrettyPrint(true);
    IParser jsonParser = ctx.newJsonParser();
    ValueSetQueryClient vsClient;
    IGenericClient clinicalDataClient;
    String symptomsValueSetUrl = "http://flintlock-fhir.lantanagroup.com/fhir/ValueSet/symptoms";
    String dxtcSnomedValueSetUrl = "https://flintlock-fhir.lantanagroup.com/fhir/ValueSet/dxtc-snomed";
    String dxtcCoronavirusValueSetUrl = "https://flintlock-fhir.lantanagroup.com/fhir/ValueSet/dxtc-coronavirus";

    public FlintlockController() {
        this.vsClient = new ValueSetQueryClient(conformanceServerBase, clinicalDataServerBase);
        this.clinicalDataClient = this.ctx.newRestfulGenericClient(clinicalDataServerBase);
        //ctx.setNarrativeGenerator(new DefaultThymeleafNarrativeGenerator());
    }

    public ValidationResult validate(IBaseResource resource) {
        ValidationResult result = null;
        FhirValidator validator = ctx.newValidator();
        IValidatorModule module1 = new SchemaBaseValidator(ctx);
        validator.registerValidatorModule(module1);
        IValidatorModule module2 = new SchematronBaseValidator(ctx);
        validator.registerValidatorModule(module2);
        result = validator.validateWithResult(resource);
        for (SingleValidationMessage next : result.getMessages()) {
            System.out.println(next.getLocationString() + " " + next.getMessage());
        }
        return result;
    }


    @GetMapping(value = "patients", produces = "application/fhir+xml")
    public String patients() {
        ValueSet vs = vsClient.getValueSet(dxtcSnomedValueSetUrl);
        logger.info("Retrieved value set", vs.getUrl());
        List<Condition> resultList = vsClient.conditionCodeQuery(vs);
        Map<String, Patient> patientRefs = getUniquePatientReferences(resultList);
        Bundle b = new Bundle();
        b.setType(BundleType.COLLECTION);
        for (String key : patientRefs.keySet()) {
            Patient p = patientRefs.get(key);
            BundleEntryComponent entry = b.addEntry();
            entry.setFullUrl(clinicalDataServerBase + "/" + key);
            entry.setResource(p);
        }
        String parsedResource = xmlParser.encodeResourceToString(b);
        logger.info(parsedResource);
        return parsedResource;
    }

    @GetMapping(value = "report", produces = "application/fhir+xml")
    public String report() {
        ValueSet vs = vsClient.getValueSet(dxtcCoronavirusValueSetUrl);
        logger.info("Retrieved value set", vs.getUrl());
        List<Condition> resultList = vsClient.conditionCodeQuery(vs);
        Map<String, Patient> patientRefs = getUniquePatientReferences(resultList);
        Bundle b = new Bundle();
        //	b.setType(BundleType.TRANSACTION);
        b.setType(BundleType.COLLECTION);
        for (String key : patientRefs.keySet()) {
            logger.info("Building report for {}", key);
            Patient p = patientRefs.get(key);
            ElectronicCaseReport ecr = new ElectronicCaseReport(this.clinicalDataClient, p, null, null);
            Bundle ecrDoc = ecr.compile();
            logger.info("Created report {}", ecrDoc.getId());
            BundleEntryComponent entry = b.addEntry();
            entry.setFullUrl(ecrDoc.getId());
            entry.setResource(ecrDoc);
            if (b.getType().equals(BundleType.TRANSACTION)) {
                entry.getRequest().setMethod(HTTPVerb.PUT).setUrl("Bundle/");
            }
        }

        // TODO Maven dependency for HAPI FHIR Validation not working, figure out why
		/*
		ValidationResult result = validate(b);
        if (result.isSuccessful()) {
        	logger.info("Bundle is valid");
        } else {
        	logger.info("Bundle is not valid. Output may be incomplete.");
        }
		 */

        logger.info("Finished creating reports");
        String parsedResource = xmlParser.encodeResourceToString(b);
        return parsedResource;
    }

    @GetMapping(value = "test/{patientId}", produces = "application/xml")
    public String test(@PathVariable("patientId") String patientId) {
        Patient subject = (Patient) this.clinicalDataClient
                .read()
                .resource(Patient.class)
                .withId(patientId)
                .execute();
        ElectronicCaseReport ecr = new ElectronicCaseReport(this.clinicalDataClient, subject, null, null);
        Bundle ecrDoc = ecr.compile();
        IParser xmlParser = this.ctx.newXmlParser();
        return xmlParser.encodeResourceToString(ecrDoc);
    }

    private Map<String, Patient> getUniquePatientReferences(List<Condition> conditions) {
        HashMap<String, Patient> patients = new HashMap<String, Patient>();
        for (Condition c : conditions) {
            String key = c.getSubject().getReference();
            Patient p = clinicalDataClient.read().resource(Patient.class).withUrl(key).execute();
            patients.put(key, p);
        }
        return patients;
    }
}
