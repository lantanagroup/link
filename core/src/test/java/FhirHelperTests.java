import com.lantanagroup.link.FhirHelper;
import org.hl7.fhir.r4.model.*;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;

import static com.lantanagroup.link.FhirHelper.findReferences;

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
}
