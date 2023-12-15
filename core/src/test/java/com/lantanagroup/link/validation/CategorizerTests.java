package com.lantanagroup.link.validation;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lantanagroup.link.ValidationCategorizer;
import com.lantanagroup.link.db.model.tenant.ValidationResult;
import com.lantanagroup.link.db.model.tenant.ValidationResultCategory;
import com.lantanagroup.link.model.ValidationCategory;
import com.lantanagroup.link.model.ValidationCategorySeverities;
import com.lantanagroup.link.model.ValidationCategoryTypes;
import org.junit.Assert;
import org.junit.Ignore;
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
    ValidationCategorizer categorizer = new ValidationCategorizer(results);
    categorizer.loadFromResources();
    List<ValidationResultCategory> categorizedResults = categorizer.categorize();
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
    ValidationCategorizer categorizer = new ValidationCategorizer(results);
    ValidationCategorizer.loadAndRetrieveCategories().clear();
    categorizer.addCategory("Invalid Code in required ValueSet", ValidationCategorySeverities.ERROR, false, ValidationCategoryTypes.CRITICAL,
                    "The code is not part of the required ValueSet. This may cause issues with measure calculation.")
            .addRuleSet()
            .addRule(ValidationCategoryRule.Field.SEVERITY, "^error$")
            .addRule(ValidationCategoryRule.Field.CODE, "^code-invalid$")
            .addRule(ValidationCategoryRule.Field.DETAILS_TEXT, "^The value provided \\(.*\\) is not in the value set '.*' \\(.*\\), and a code is required from this value set");
    List<ValidationResultCategory> categorizedResults = categorizer.categorize();
    Assert.assertEquals(1, categorizedResults.size());
  }

  @Test
  @Ignore
  public void serializeCategories() throws JsonProcessingException {
    ValidationCategorizer categorizer = new ValidationCategorizer(null);
    ValidationCategorizer.loadAndRetrieveCategories().clear();

    categorizer.addCategory("Can't validate code", ValidationCategorySeverities.WARNING, true, ValidationCategoryTypes.POTENTIAL_CONCERN, "There is an issue with the way the CodeSystem is set up on the terminology server. The full code set for the system does not appear to be on the server. The terminology server should be updated.")
            .addRuleSet()
            .addRule(ValidationCategoryRule.Field.DETAILS_TEXT, "^The system .* http:\\/\\/hl7\\.org/fhir\\/sid\\/icd-9-cm was found but did not contain enough information to properly validate the code \\(mode = fragment\\) \\(from Tx-Server\\)");
    categorizer.addCategory("Unresolved Epic Code System URI", ValidationCategorySeverities.INFORMATION, true, ValidationCategoryTypes.NOT_IMPORTANT, "This is an Epic proprietary Code System and is only a concern if there is not another coding that provides a standard recognized coding Code System (which is handled under another logged issue)")
            .addRuleSet(false)
            .addRule(ValidationCategoryRule.Field.DETAILS_TEXT, "^Code System URI ''urn:oid:1\\.2\\.840\\.114350.*'' is unknown so the code cannot be validated")
            .addRule(ValidationCategoryRule.Field.DETAILS_TEXT, "^Code System URI ''http://.*epic\\.com/.*'' is unknown so the code cannot be validated");
    categorizer.addCategory("Unresolved Medispan Code System URI", ValidationCategorySeverities.INFORMATION, true, ValidationCategoryTypes.NOT_IMPORTANT, "This is an Medispan proprietary Code System and is only a concern if there is not another coding that provides a standard recognized coding Code System (which is handled under another logged issue)")
            .addRuleSet()
            .addRule(ValidationCategoryRule.Field.DETAILS_TEXT, "^Code System URI ''urn:oid:2\\.16\\.840\\.1\\.113883\\.6\\.68'' is unknown so the code cannot be validated");
    categorizer.addCategory("Unresolved and Unrecognized Code System URI", ValidationCategorySeverities.INFORMATION, true, ValidationCategoryTypes.NOT_IMPORTANT, "This is an unrecognized Code System and is only a concern if there is not another coding that provides a standard recognized coding Code System (which is handled under another logged issue)")
            .addRuleSet()
            .addRule(ValidationCategoryRule.Field.DETAILS_TEXT, "^Code System URI ''.*'' is unknown so the code cannot be validated")
            .addRule(ValidationCategoryRule.Field.DETAILS_TEXT, "%urn:oid:1.2.840.114350%", true)
            .addRule(ValidationCategoryRule.Field.DETAILS_TEXT, "%http://%epic.com%", true)
            .addRule(ValidationCategoryRule.Field.DETAILS_TEXT, "%urn:oid:2.16.840.1.113883.6.68%", true);
    categorizer.addCategory("Unknown Extension", ValidationCategorySeverities.INFORMATION, true, ValidationCategoryTypes.NOT_IMPORTANT, "Systems are allowed to include extensions (additional data). Extensions that do not modify the meaning of the data (modifierExtensions) can be safely ignored. This is not a modifierExtension.")
            .addRuleSet()
            .addRule(ValidationCategoryRule.Field.DETAILS_TEXT, "^The extension .* is unknown, and not allowed here$");
    categorizer.addCategory("Does not match a slice", ValidationCategorySeverities.INFORMATION, true, ValidationCategoryTypes.POTENTIAL_CONCERN, "This could indicate an underlying issue in the resource (the resource is not validating). FHIR SME may need to review.")
            .addRuleSet()
            .addRule(ValidationCategoryRule.Field.DETAILS_TEXT, "^This element does not match any known slice defined in the profile");
    categorizer.addCategory("Unable to match profile", ValidationCategorySeverities.ERROR, false, ValidationCategoryTypes.POTENTIAL_CONCERN, "This could indicate an underlying issue in the resource (the resource is not validating). FHIR SME may need to review.")
            .addRuleSet()
            .addRule(ValidationCategoryRule.Field.DETAILS_TEXT, "^Unable to find a match for profile .* among choices:");
    categorizer.addCategory("Does not match preferred ValueSet", ValidationCategorySeverities.INFORMATION, true, ValidationCategoryTypes.POTENTIAL_CONCERN, "This could be indicative of a problem if the data element is part of the measure and would not enable the resource to be included in the measure calculation appropriately.")
            .addRuleSet()
            .addRule(ValidationCategoryRule.Field.DETAILS_TEXT, "^None of the codings provided are in the value set ''.*'' .*, and a coding is recommended to come from this value set");
    categorizer.addCategory("Does not match extensible ValueSet", ValidationCategorySeverities.WARNING, false, ValidationCategoryTypes.POTENTIAL_CONCERN, "This could be indicative of a problem if the data element is part of the measure and would not enable the resource to be included in the measure calculation appropriately.")
            .addRuleSet()
            .addRule(ValidationCategoryRule.Field.DETAILS_TEXT, "^None of the codings provided are in the value set ''.*'' .*, and a coding should come from this value set unless it has no suitable code \\(note that the validator cannot judge what is suitable\\)");
    categorizer.addCategory("Possible matching profile", ValidationCategorySeverities.INFORMATION, true, ValidationCategoryTypes.POTENTIAL_CONCERN, "This could indicate an underlying issue in the resource (the resource is not validating). FHIR SME may need to review.")
            .addRuleSet()
            .addRule(ValidationCategoryRule.Field.DETAILS_TEXT, "^Details for .* matching against profile");
    categorizer.addCategory("Identifier.type code not provided from IdentifyType VS", ValidationCategorySeverities.WARNING, true, ValidationCategoryTypes.NOT_IMPORTANT, "This is only for business identifiers. Not important, unless a business identifier, such as MRN is required to be identified.")
            .addRuleSet()
            .addRule(ValidationCategoryRule.Field.DETAILS_TEXT, "^No code provided, and a code should be provided from the value set ''.*'' \\(http:\\/\\/hl7\\.org\\/fhir\\/ValueSet\\/identifier-type\\|4\\.0\\.1\\)");
    categorizer.addCategory("MedicationRequest.requestor does not have a proper reference", ValidationCategorySeverities.WARNING, true, ValidationCategoryTypes.NOT_IMPORTANT, "No identity or reference for requester (Provider). Safe to ignore when ordering provider is not needed.")
            .addRuleSet()
            .addRule(ValidationCategoryRule.Field.DETAILS_TEXT, "A Reference without an actual reference or identifier should have a display")
            .addRule(ValidationCategoryRule.Field.EXPRESSION, "Bundle.entry[\\[[0-9]+\\]\\.resource\\/.*\\*\\/\\.requester");
    categorizer.addCategory("Identifier value starts with whitespace", ValidationCategorySeverities.WARNING, true, ValidationCategoryTypes.NOT_IMPORTANT, "This is a business identifier with whitespace at the front or back. Not important if business identifiers are not used. May want to have the whitespace trimmed.")
            .addRuleSet()
            .addRule(ValidationCategoryRule.Field.DETAILS_TEXT, "^value should not start or finish with whitespace ''.*''")
            .addRule(ValidationCategoryRule.Field.EXPRESSION, "resource.*\\.identifier\\[[0-9]+\\]\\.value");
    categorizer.addCategory("No measure score allowed with cohort", ValidationCategorySeverities.ERROR, false, ValidationCategoryTypes.IMPORTANT, "The MeasureReport violates a business rule regarding MeasureScore. This issue needs to be resolved in the source data, the validator, or explicitely deemed an invalid error.")
            .addRuleSet()
            .addRule(ValidationCategoryRule.Field.DETAILS_TEXT, "^No measureScore when the scoring of the message is ''''cohort''''");
    categorizer.addCategory("Invalid whitespace (non-identifier)", ValidationCategorySeverities.WARNING, true, ValidationCategoryTypes.NOT_IMPORTANT, "This is a non-identifier string element with whitespace at the front or back. Not generally important. May want to have the whitespace trimmed.")
            .addRuleSet()
            .addRule(ValidationCategoryRule.Field.DETAILS_TEXT, "^value should not start or finish with whitespace ''.*''")
            .addRule(ValidationCategoryRule.Field.EXPRESSION, "resource.*\\.\\w+\\[[0-9]+\\]\\.value", true);
    categorizer.addCategory("Minimum slice occurrence not met", ValidationCategorySeverities.ERROR, true, ValidationCategoryTypes.POTENTIAL_CONCERN, "This is likely a secondary issue. If the resource can't validate then it can't be counted as meeting the slicing requirements. Likely addressing the underlying issue will make this issue go away. ")
            .addRuleSet()
            .addRule(ValidationCategoryRule.Field.DETAILS_TEXT, "^Bundle.entry:.*: minimum required = .*, but only found .* \\(.*\\)");
    categorizer.addCategory("Link Error using old URL", ValidationCategorySeverities.WARNING, false, ValidationCategoryTypes.IMPORTANT, "This is an issue with NHSNLink (using an old url) and should be reported in Jira.")
            .addRuleSet()
            .addRule(ValidationCategoryRule.Field.DETAILS_TEXT, "'http:\\/\\/lantanagroup\\.com\\/fhir\\/nhsn-measures.*'");
    categorizer.addCategory("No codes from an extensible binding ValueSet", ValidationCategorySeverities.WARNING, false, ValidationCategoryTypes.IMPORTANT, "The code provided is not part of the extensible ValueSet, which if it is a concept that is part of the measure, is a problem that needs to be resolved.")
            .addRuleSet()
            .addRule(ValidationCategoryRule.Field.DETAILS_TEXT, "None of the codings provided are in the value set ''.*'' \\(.*\\), and a coding should come from this value set unless it has no suitable code \\(note that the validator cannot judge what is suitable\\)");
    categorizer.addCategory("No code provided", ValidationCategorySeverities.WARNING, false, ValidationCategoryTypes.IMPORTANT, "No code was provided, which if it is a concept that is part of the measure, is a problem that needs to be resolved.")
            .addRuleSet()
            .addRule(ValidationCategoryRule.Field.DETAILS_TEXT, "No code provided, and a code should be provided from the value set ''.*'' \\(.*\\)");
    categorizer.addCategory("Unable to validate measure (Measure not found)", ValidationCategorySeverities.WARNING, false, ValidationCategoryTypes.IMPORTANT, "This appears to be an issue in the validation process and should be resolved as it may be hiding other issues.")
            .addRuleSet(false)
            .addRule(ValidationCategoryRule.Field.DETAILS_TEXT, "Canonical URL ''.*'' does not resolve")
            .addRule(ValidationCategoryRule.Field.DETAILS_TEXT, "The Measure ''.*'' could not be resolved, so no validation can be performed against the Measure");

    categorizer.getCategories().forEach(c -> c.setId(c.getTitle().replaceAll("[^a-zA-Z0-9]", "_")));

    System.out.println("Validation Categories:");
    System.out.println(new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(categorizer.getCategories()));
    System.out.println(String.join("\n", categorizer.getCategories().stream().map(ValidationCategory::getId).toArray(String[]::new)));
  }
}
