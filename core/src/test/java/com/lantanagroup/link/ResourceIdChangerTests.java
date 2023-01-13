package com.lantanagroup.link;

import org.hl7.fhir.r4.model.*;
import org.junit.Assert;
import org.junit.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ResourceIdChangerTests {
  @Test
  public void findReferencesBundleTest() {
    Bundle bundle = new Bundle();
    Patient patient = new Patient();
    patient.setId("123");
    bundle.addEntry(new Bundle.BundleEntryComponent().setResource(patient));
    Observation obs = new Observation();
    obs.setSubject(new Reference("Patient/123"));
    bundle.addEntry(new Bundle.BundleEntryComponent().setResource(obs));
    Composition comp = new Composition();
    comp.getRelatesTo().add(new Composition.CompositionRelatesToComponent().setTarget(new Reference("Composition/xyz")));
    bundle.addEntry(new Bundle.BundleEntryComponent().setResource(comp));

    List<Reference> references = ResourceIdChanger.findReferences(bundle);
    Assert.assertEquals(2, references.size());
    Assert.assertEquals("Patient/123", references.get(0).getReference());
    Assert.assertEquals("Composition/xyz", references.get(1).getReference());
  }

  @Test
  public void changeIdsTest() {
    Bundle bundle = new Bundle();

    Condition cond1 = new Condition();
    cond1.setId("lkajsdflkajsdflkajsdflkajsdflkajsdflkajsdflkajsdflkajsdflkajsdflkajsdflkajsdf");  // 78 chars
    bundle.addEntry(new Bundle.BundleEntryComponent().setResource(cond1));

    Condition cond2 = new Condition();
    cond2.setId("urn:uuid:987654321");
    bundle.addEntry(new Bundle.BundleEntryComponent().setResource(cond2));

    Condition cond3 = new Condition();
    cond3.setId("urn:uuid:12341234123412341234123412341234123412341234123412341234123412341234123412341");  // 78 chars
    bundle.addEntry(new Bundle.BundleEntryComponent().setResource(cond3));

    Condition cond4 = new Condition();
    cond4.setId("correctOne");
    bundle.addEntry(new Bundle.BundleEntryComponent().setResource(cond4));


    Encounter enc1 = new Encounter();
    enc1.setId("ThisIsATest");
    enc1.addDiagnosis().setCondition(new Reference("Condition/lkajsdflkajsdflkajsdflkajsdflkajsdflkajsdflkajsdflkajsdflkajsdflkajsdflkajsdf"));
    enc1.addDiagnosis().setCondition(new Reference("Condition/urn:uuid:12341234123412341234123412341234123412341234123412341234123412341234123412341"));
    enc1.addDiagnosis().setCondition(new Reference("Condition/urn:uuid:123412341"));
    enc1.addDiagnosis().setCondition(new Reference("Condition/correctOne"));
    bundle.addEntry().setResource(enc1);

    ResourceIdChanger.changeIds(bundle);

    // Make sure the Condition's ID was changed
    Assert.assertEquals("hash-e05ed46d", cond1.getIdElement().getIdPart());

    // Make sure the Condition's ID was changed
    Assert.assertEquals("987654321", cond2.getIdElement().getIdPart());

    // Make sure the Condition's ID was changed
    Assert.assertEquals("hash-1b761b0b", cond3.getIdElement().getIdPart());

    // Make sure the Condition's ID was not changed
    Assert.assertEquals("correctOne", cond4.getIdElement().getIdPart());


    // Make sure an extension was added to the resource to track the original ID
    Assert.assertNotNull(cond1.getExtensionByUrl(ResourceIdChanger.ORIG_ID_EXT_URL));

    // Make sure an extension was added to the resource to track the original ID
    Assert.assertNotNull(cond2.getExtensionByUrl(ResourceIdChanger.ORIG_ID_EXT_URL));

    // Make sure an extension was added to the resource to track the original ID
    Assert.assertNotNull(cond3.getExtensionByUrl(ResourceIdChanger.ORIG_ID_EXT_URL));

    // Make sure an extension was added to the resource to track the original ID
    Assert.assertNull(cond4.getExtensionByUrl(ResourceIdChanger.ORIG_ID_EXT_URL));


    // Make sure the reference to the Condition has an extension to track the original reference
    Assert.assertEquals("Condition/hash-e05ed46d", enc1.getDiagnosisFirstRep().getCondition().getReference());
    Assert.assertNotNull(enc1.getDiagnosisFirstRep().getCondition().getExtensionByUrl(ResourceIdChanger.ORIG_ID_EXT_URL));

    // Make sure the reference to a condition that isn't in the bundle is updated
    Assert.assertEquals("Condition/hash-1b761b0b", enc1.getDiagnosis().get(1).getCondition().getReference());
    Assert.assertNotNull(enc1.getDiagnosis().get(1).getCondition().getExtensionByUrl(ResourceIdChanger.ORIG_ID_EXT_URL));

    // Make sure the reference to a condition that isn't in the bundle is updated
    Assert.assertEquals("Condition/123412341", enc1.getDiagnosis().get(2).getCondition().getReference());
    Assert.assertNotNull(enc1.getDiagnosis().get(2).getCondition().getExtensionByUrl(ResourceIdChanger.ORIG_ID_EXT_URL));

    Assert.assertEquals("Condition/correctOne", enc1.getDiagnosis().get(3).getCondition().getReference());
    Assert.assertNull(enc1.getDiagnosis().get(3).getCondition().getExtensionByUrl(ResourceIdChanger.ORIG_ID_EXT_URL));
  }

  @Test
  public void findCodingsBundleTest() {
    Bundle bundle = new Bundle();
    Patient patient = new Patient();
    patient.setId("123");
    bundle.addEntry(new Bundle.BundleEntryComponent().setResource(patient));
    Observation obs = new Observation();
    obs.setSubject(new Reference("Patient/123"));
    bundle.addEntry(new Bundle.BundleEntryComponent().setResource(obs));
    Composition comp = new Composition();
    comp.getRelatesTo().add(new Composition.CompositionRelatesToComponent().setTarget(new Reference("Composition/xyz")));
    bundle.addEntry(new Bundle.BundleEntryComponent().setResource(comp));
    Location location = new Location();
    location.setId("p0yPvH-JAN21");
    List<CodeableConcept> codeableConceptList = new ArrayList<>();
    CodeableConcept codeableConcept = new CodeableConcept();
    Coding coding = new Coding();
    coding.setSystem("http://some-system.com");
    coding.setCode("some-type1");

    Coding coding1 = new Coding();
    coding1.setSystem("http://some-system2.com");
    coding1.setCode("some-type2");

    codeableConcept.addCoding(coding);
    codeableConcept.addCoding(coding1);
    codeableConceptList.add(codeableConcept);
    location.setType(codeableConceptList);
    location.setName("Some Location");
    bundle.addEntry(new Bundle.BundleEntryComponent().setResource(location));
    List<Coding> codings = ResourceIdChanger.findCodings(bundle);
    Assert.assertEquals(2, codings.size());
    Coding codingTarget = codings.get(0);
    Assert.assertEquals("some-type1", codingTarget.getCode());
    Assert.assertEquals("http://some-system.com", codingTarget.getSystem());
    Coding codingTarget2 = codings.get(1);
    Assert.assertEquals("some-type2", codingTarget2.getCode());
    Assert.assertEquals("http://some-system2.com", codingTarget2.getSystem());
  }

  @Test
  public void stressTest() {
    Random random = new Random();
    String characters = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    List<String> ids = Stream.generate(() -> {
              StringBuilder builder = new StringBuilder();
              while (builder.length() < 100) {
                builder.append(characters.charAt(random.nextInt(characters.length())));
              }
              return builder.toString();
            })
            .limit(100000)
            .collect(Collectors.toList());
    Bundle bundle = new Bundle();
    for (String id : ids) {
      Basic resource = new Basic();
      resource.setId(id);
      String subjectId = ids.get(random.nextInt(ids.size()));
      resource.setSubject(new Reference(String.format("Basic/%s", subjectId)));
      bundle.addEntry().setResource(resource);
    }
    Instant start = Instant.now();
    ResourceIdChanger.changeIds(bundle);
    Instant end = Instant.now();
    // Should take less than ten seconds
    Assert.assertTrue(Duration.between(start, end).compareTo(Duration.ofSeconds(10L)) < 0);
  }
}
