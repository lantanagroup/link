package com.lantanagroup.link.validation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lantanagroup.link.db.model.tenant.ValidationResult;
import com.lantanagroup.link.db.model.tenant.ValidationResultCategory;
import com.lantanagroup.link.model.ValidationCategory;
import com.lantanagroup.link.model.ValidationCategorySeverities;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

public class CategorizerTests {
  private static ValidationResult createValidationResult(String severity, String code, String details, String expression) {
    ValidationResult result = new ValidationResult();
    result.setSeverity(severity);
    result.setCode(code);
    result.setDetails(details);
    result.setSeverity(severity);
    result.setExpression(expression);
    return result;
  }

  private List<ValidationResult> getResults() throws IOException, URISyntaxException {
    String validationCategoriesJson = Files.readString(Path.of(this.getClass().getClassLoader().getResource("validation-results.json").toURI()));
    List<ValidationResult> results = List.of(new ObjectMapper().readValue(validationCategoriesJson, ValidationResult[].class));
    return results;
  }

  @Test
  public void categoriesTest() throws IOException, URISyntaxException {
    List<ValidationResult> results = this.getResults();
    ValidationCategorizer categorizer = new ValidationCategorizer();
    categorizer.loadFromResources();
    List<ValidationResultCategory> categorizedResults = categorizer.categorize(results);
    Assert.assertNotEquals(0, categorizedResults.size());

    List<ValidationResult> notMatched = results.stream()
            .filter(r -> categorizedResults.stream().noneMatch(c -> c.getValidationResultId().equals(r.getId())))
            .collect(Collectors.toList());
    System.out.printf("Matched %s of %s results, %s remaining%n", categorizedResults.size(), results.size(), notMatched.size());
    notMatched.forEach(r -> System.out.println("No category found for: " + r.getDetails()));
  }

  @Test
  public void categoryTest1() {
    List<ValidationResult> results = List.of(
            createValidationResult(
                    "error",
                    "code-invalid",
                    "The value provided ('text/yml') is not in the value set 'MimeType' (http://hl7.org/fhir/ValueSet/mimetypes|4.0.1), and a code is required from this value set) (error message = Failed to expand ValueSet 'http://hl7.org/fhir/ValueSet/mimetypes' (in-memory). Could not validate code null#text/yml. Error was: HAPI-0702: Unable to expand ValueSet because CodeSystem could not be found: urn:ietf:bcp:13)",
                    "Bundle.entry[3].resource.ofType(Library).content[0].contentType"
            )
    );
    ValidationCategorizer categorizer = new ValidationCategorizer();
    ValidationCategorizer.loadAndRetrieveCategories().clear();
    categorizer.addCategory("Invalid Code in required ValueSet", ValidationCategorySeverities.ERROR, false,
                    "The code is not part of the required ValueSet. This may cause issues with measure calculation.")
            .addRuleSet()
            .addRule(ValidationCategoryRule.Field.SEVERITY, "^error$")
            .addRule(ValidationCategoryRule.Field.CODE, "^code-invalid$")
            .addRule(ValidationCategoryRule.Field.DETAILS_TEXT, "^The value provided \\(.*\\) is not in the value set '.*' \\(.*\\), and a code is required from this value set");
    List<ValidationResultCategory> categorizedResults = categorizer.categorize(results);
    Assert.assertEquals(1, categorizedResults.size());
  }

  /**
   * Checks that the validation categories defined in the core/resources are unique,
   * and don't duplicate ids.
   */
  @Test
  public void uniqueValidationCategories() {
    ValidationCategorizer categorizer = new ValidationCategorizer();
    categorizer.loadFromResources();
    categorizer.getCategories().stream()
            .map(ValidationCategory::getId)
            .collect(Collectors.groupingBy(id -> id, Collectors.counting()))
            .forEach((id, count) -> {
              Assert.assertEquals("Duplicate id: " + id, 1, count.longValue());
            });
  }
}
