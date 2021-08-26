import com.lantanagroup.link.FhirHelper;
import org.hl7.fhir.r4.model.*;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class FhirHelperTests {
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

    List<Reference> references = FhirHelper.findReferences(bundle);
    Assert.assertEquals(2, references.size());
    Assert.assertEquals("Patient/123", references.get(0).getReference());
    Assert.assertEquals("Composition/xyz", references.get(1).getReference());
  }

  @Test
  public void changeIdsTest() {
    Bundle bundle = new Bundle();
    Patient patient = new Patient();
    patient.setId("123");
    bundle.addEntry(new Bundle.BundleEntryComponent().setResource(patient));
    Observation obs1 = new Observation();
    obs1.setSubject(new Reference("Patient/123"));
    bundle.addEntry(new Bundle.BundleEntryComponent().setResource(obs1));
    Composition comp1 = new Composition();
    comp1.getRelatesTo().add(new Composition.CompositionRelatesToComponent().setTarget(new Reference("Composition/xyz")));
    bundle.addEntry(new Bundle.BundleEntryComponent().setResource(comp1));
    Composition comp2 = new Composition();
    comp2.setId("xyz");
    bundle.addEntry(new Bundle.BundleEntryComponent().setResource(comp2));
    Observation obs2 = new Observation();
    obs2.setId("obs2");
    bundle.addEntry(new Bundle.BundleEntryComponent().setResource(obs2));

    FhirHelper.changeIds(bundle);

    Assert.assertNotEquals("123", patient.getId());
    Assert.assertEquals(36, patient.getId().length());
    Assert.assertEquals("Patient/" + patient.getId(), obs1.getSubject().getReference());
    Assert.assertNotEquals("xyz", comp2.getId());
    Assert.assertEquals(36, comp2.getId().length());
    Assert.assertEquals("Composition/" + comp2.getIdElement().getIdPart(), comp1.getRelatesToFirstRep().getTargetReference().getReference());
    Assert.assertNotEquals("obs2", obs2.getId());
    Assert.assertEquals(36, obs2.getId().length());
  }

  @Test
  public void fixResourceIdsTest() {
    Bundle bundle = new Bundle();

    Condition cond1 = new Condition();
    cond1.setId("lkajsdflkajsdflkajsdflkajsdflkajsdflkajsdflkajsdflkajsdflkajsdflkajsdflkajsdf");  // 78 chars
    bundle.addEntry(new Bundle.BundleEntryComponent().setResource(cond1));

    Encounter enc1 = new Encounter();
    enc1.setId("ThisIsATest");
    enc1.addDiagnosis().setCondition(new Reference("Condition/lkajsdflkajsdflkajsdflkajsdflkajsdflkajsdflkajsdflkajsdflkajsdflkajsdflkajsdf"));
    bundle.addEntry().setResource(enc1);

    FhirHelper.fixResourceIds(bundle);

    // Make sure the Condition's ID was changed
    Assert.assertEquals("HASH-530656147", cond1.getIdElement().getIdPart());

    // Make sure an extension was added to the resource to track the original ID
    Assert.assertNotNull(cond1.getExtensionByUrl(FhirHelper.ORIG_ID_EXT_URL));

    // Make sure the reference to the Condition in the encounter got updated

    // Make sure the reference to the Condition has an extension to track the original reference
    Assert.assertEquals("Condition/HASH-530656147", enc1.getDiagnosisFirstRep().getCondition().getReference());
    Assert.assertNotNull(enc1.getDiagnosisFirstRep().getCondition().getExtensionByUrl(FhirHelper.ORIG_ID_EXT_URL));
  }

  @Test
  public void getNameTest() {
    HumanName name1 = new HumanName().setFamily("Sombody").addGiven("Joe");
    HumanName name2 = new HumanName().addGiven("Joe Sombody");
    HumanName name3 = new HumanName().setFamily("Joe Sombody");
    HumanName name4 = new HumanName().setText("Joe Sombody");

    String actual1 = FhirHelper.getName(Arrays.asList(name1));
    String actual2 = FhirHelper.getName(Arrays.asList(name2));
    String actual3 = FhirHelper.getName(Arrays.asList(name3));
    String actual4 = FhirHelper.getName(Arrays.asList(name4));

    Assert.assertEquals(actual1, "Joe Sombody");
    Assert.assertEquals(actual2, "Joe Sombody");
    Assert.assertEquals(actual3, "Joe Sombody");
    Assert.assertEquals(actual4, "Joe Sombody");
  }
}
