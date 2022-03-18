package com.lantanagroup.link;

import org.hl7.fhir.r4.model.*;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;

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
    cond2.setId("urn:uuid:987654321");  // 78 chars
    bundle.addEntry(new Bundle.BundleEntryComponent().setResource(cond2));

    Condition cond3 = new Condition();
    cond3.setId("urn:uuid:12341234123412341234123412341234123412341234123412341234123412341234123412341");  // 78 chars
    bundle.addEntry(new Bundle.BundleEntryComponent().setResource(cond3));

    Condition cond4 = new Condition();
    cond4.setId("correctOne");  // 78 chars
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
    Assert.assertEquals("HASH-530656147", cond1.getIdElement().getIdPart());

    // Make sure the Condition's ID was changed
    Assert.assertEquals("987654321", cond2.getIdElement().getIdPart());

    // Make sure the Condition's ID was changed
    Assert.assertEquals("HASH460725003", cond3.getIdElement().getIdPart());

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
    Assert.assertEquals("Condition/HASH-530656147", enc1.getDiagnosisFirstRep().getCondition().getReference());
    Assert.assertNotNull(enc1.getDiagnosisFirstRep().getCondition().getExtensionByUrl(ResourceIdChanger.ORIG_ID_EXT_URL));

    // Make sure the reference to a condition that isn't in the bundle is updated
    Assert.assertEquals("Condition/HASH460725003", enc1.getDiagnosis().get(1).getCondition().getReference());
    Assert.assertNotNull(enc1.getDiagnosis().get(1).getCondition().getExtensionByUrl(ResourceIdChanger.ORIG_ID_EXT_URL));

    // Make sure the reference to a condition that isn't in the bundle is updated
    Assert.assertEquals("Condition/123412341", enc1.getDiagnosis().get(2).getCondition().getReference());
    Assert.assertNotNull(enc1.getDiagnosis().get(2).getCondition().getExtensionByUrl(ResourceIdChanger.ORIG_ID_EXT_URL));

    Assert.assertEquals("Condition/correctOne", enc1.getDiagnosis().get(3).getCondition().getReference());
    Assert.assertNull(enc1.getDiagnosis().get(3).getCondition().getExtensionByUrl(ResourceIdChanger.ORIG_ID_EXT_URL));
  }
}
