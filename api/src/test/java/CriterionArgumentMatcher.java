import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.gclient.ICriterion;
import ca.uhn.fhir.rest.gclient.ICriterionInternal;
import ca.uhn.fhir.rest.gclient.ReferenceClientParam;
import org.mockito.ArgumentMatcher;
import ca.uhn.fhir.rest.gclient.StringCriterion;

public class CriterionArgumentMatcher implements ArgumentMatcher<ICriterionInternal> {
  private ICriterionInternal expectedCriterion;
  private FhirContext fhirContext;

  public CriterionArgumentMatcher(ICriterionInternal expectedCriterion) {
    this.expectedCriterion = expectedCriterion;
  }

  @Override
  public boolean matches(ICriterionInternal referenceClientParamICriterion) {
    String expectedName = this.expectedCriterion.getParameterName();
    String expectedValue = this.expectedCriterion.getParameterValue(this.fhirContext);
    return false;
  }
}
