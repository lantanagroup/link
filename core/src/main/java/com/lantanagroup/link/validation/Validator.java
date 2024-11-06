package com.lantanagroup.link.validation;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.support.DefaultProfileValidationSupport;
import ca.uhn.fhir.context.support.IValidationSupport;
import ca.uhn.fhir.validation.*;
import com.lantanagroup.link.Constants;
import com.lantanagroup.link.FhirContextProvider;
import org.hl7.fhir.common.hapi.validation.support.*;
import org.hl7.fhir.common.hapi.validation.validator.FhirInstanceValidator;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Validator {
  protected static final Logger logger = LoggerFactory.getLogger(Validator.class);

  private volatile FhirValidator validator;

  private static OperationOutcome.IssueSeverity getIssueSeverity(ResultSeverityEnum severity) {
    switch (severity) {
      case ERROR:
        return OperationOutcome.IssueSeverity.ERROR;
      case WARNING:
        return OperationOutcome.IssueSeverity.WARNING;
      case INFORMATION:
        return OperationOutcome.IssueSeverity.INFORMATION;
      case FATAL:
        return OperationOutcome.IssueSeverity.FATAL;
      default:
        throw new RuntimeException("Unexpected severity " + severity);
    }
  }

  private static OperationOutcome.IssueType getIssueCode(String messageId) {
    if (messageId == null) {
      return OperationOutcome.IssueType.NULL;
    } else if (messageId.startsWith("Rule ")) {
      return OperationOutcome.IssueType.INVARIANT;
    }

    switch (messageId) {
      case "TERMINOLOGY_TX_SYSTEM_NO_CODE":
        return OperationOutcome.IssueType.INFORMATIONAL;
      case "Terminology_TX_NoValid_2_CC":
      case "Terminology_PassThrough_TX_Message":
      case "Terminology_TX_Code_ValueSet_Ext":
      case "Terminology_TX_NoValid_17":
      case "Terminology_TX_NoValid_16":
      case "Terminology_TX_NoValid_3_CC":
        return OperationOutcome.IssueType.CODEINVALID;
      case "Extension_EXT_Unknown":
        return OperationOutcome.IssueType.EXTENSION;
      case "Measure_MR_M_NotFound":
        return OperationOutcome.IssueType.NOTFOUND;
      case "Validation_VAL_Profile_Minimum":
      case "Validation_VAL_Profile_Maximum":
      case "Extension_EXT_Type":
      case "Validation_VAL_Profile_Unknown":
      case "Reference_REF_NoDisplay":
        return OperationOutcome.IssueType.STRUCTURE;
      case "Type_Specific_Checks_DT_String_WS":
        return OperationOutcome.IssueType.VALUE;
      case "Terminology_TX_System_Unknown":
        return OperationOutcome.IssueType.UNKNOWN;
      case "Type_Specific_Checks_DT_Code_WS":
        return OperationOutcome.IssueType.INVALID;
      case "MEASURE_MR_GRP_POP_COUNT_MISMATCH":
      case "MEASURE_MR_SCORE_PROHIBITED_MS":
        return OperationOutcome.IssueType.BUSINESSRULE;
      default:
        return OperationOutcome.IssueType.NULL;
    }
  }

  /**
   * Initializes a FhirValidator based on the following validation support:
   * <ul>
   *   <li>Default (base R4) profiles</li>
   *   <li>IGs/resources found on the classpath</li>
   *   <li>Specified measure definition bundles</li>
   *   <li>Snapshot-generating support</li>
   *   <li>In-memory terminology service</li>
   *   <li>Common code systems terminology service</li>
   *   <li>Unknown code system warning support</li>
   * </ul>
   */
  private static FhirValidator initialize(List<Bundle> support) {
    FhirContext fhirContext = FhirContextProvider.getFhirContext();
    FhirValidator validator = fhirContext.newValidator();

    MeasureDefinitionBasedValidationSupport measureDefinitionBasedValidationSupport =
            new MeasureDefinitionBasedValidationSupport(fhirContext);
    support.stream()
            .flatMap(bundle -> bundle.getEntry().stream())
            .map(Bundle.BundleEntryComponent::getResource)
            .filter(Objects::nonNull)
            .forEachOrdered(measureDefinitionBasedValidationSupport::addResource);
    UnknownCodeSystemWarningValidationSupport unknownCodeSystemWarningValidationSupport =
            new UnknownCodeSystemWarningValidationSupport(fhirContext);
    unknownCodeSystemWarningValidationSupport.setNonExistentCodeSystemSeverity(IValidationSupport.IssueSeverity.WARNING);
    ValidationSupportChain validationSupportChain = new ValidationSupportChain(
            new DefaultProfileValidationSupport(fhirContext),
            ClasspathBasedValidationSupport.getInstance(),
            measureDefinitionBasedValidationSupport,
            new SnapshotGeneratingValidationSupport(fhirContext),
            new InMemoryTerminologyServerValidationSupport(fhirContext),
            new CommonCodeSystemsTerminologyService(fhirContext),
            unknownCodeSystemWarningValidationSupport);
    CachingValidationSupport cachingValidationSupport = new CachingValidationSupport(validationSupportChain);
    IValidatorModule validatorModule = new FhirInstanceValidator(cachingValidationSupport);
    validator.registerValidatorModule(validatorModule);

    validator.setExecutorService(ForkJoinPool.commonPool());
    validator.setConcurrentBundleValidation(true);

    return validator;
  }

  public OperationOutcome validateRaw(IBaseResource resource) {
    FhirValidator validator = initialize(List.of());
    OperationOutcome outcome = new OperationOutcome();
    ValidationResult result = validator.validateWithResult(resource);
    result.populateOperationOutcome(outcome);
    return outcome;
  }

  private void validateResource(FhirValidator validator, Resource resource, OperationOutcome outcome, OperationOutcome.IssueSeverity severity) {
    ValidationOptions opts = new ValidationOptions();

    ValidationResult result = validator.validateWithResult(resource, opts);

    for (SingleValidationMessage message : result.getMessages()) {
      OperationOutcome.IssueSeverity messageSeverity = getIssueSeverity(message.getSeverity());

      // Skip the message depending on the severity filter/arg
      if (severity != null) {
        if (severity == OperationOutcome.IssueSeverity.ERROR) {
          if (messageSeverity == OperationOutcome.IssueSeverity.INFORMATION || messageSeverity == OperationOutcome.IssueSeverity.WARNING) {
            continue;
          }
        } else if (severity == OperationOutcome.IssueSeverity.WARNING) {
          if (messageSeverity == OperationOutcome.IssueSeverity.INFORMATION) {
            continue;
          }
        }
      }

      OperationOutcome.IssueType issueCode = getIssueCode(message.getMessageId());

      if (issueCode == OperationOutcome.IssueType.NULL && message.getMessageId() != null) {
        logger.warn("Unknown issue code {} for message {}", message.getMessageId(), message.getMessage());
      }

      outcome.addIssue()
              .setSeverity(messageSeverity)
              .setCode(issueCode)
              .setDetails(new CodeableConcept().setText(message.getMessage()))
              .setExpression(List.of(
                      new StringType(message.getLocationString()),
                      new StringType(message.getLocationLine() + ":" + message.getLocationCol())));
    }
  }

  /**
   * Update the issue locations/expressions to use resource IDs instead of "ofType(MedicationRequest)" (for example).
   * Only works on Bundle resources and looks for patterns such as "Bundle.entry[X].resource.ofType(MedicationRequest)" to replace with "Bundle.entry[X].resource.where(id = '123')"
   *
   * @param resource The resource to amend
   * @param outcome  The outcome to amend
   */
  private void improveIssueExpressions(Resource resource, OperationOutcome outcome) {
    final String regex = "^Bundle.entry\\[(\\d+)\\]\\.resource\\.ofType\\(.+?\\)(.*)$";
    final Pattern pattern = Pattern.compile(regex, Pattern.MULTILINE);

    if (resource.getResourceType() == ResourceType.Bundle) {
      Bundle bundle = (Bundle) resource;
      for (OperationOutcome.OperationOutcomeIssueComponent issue : outcome.getIssue()) {
        if (!issue.getExpression().isEmpty()) {
          String location = issue.getExpression().get(0).asStringValue();
          final Matcher matcher = pattern.matcher(location);

          if (matcher.matches()) {
            int entryIndex = Integer.parseInt(matcher.group(1));
            if (entryIndex < bundle.getEntry().size()) {
              String resourceId = bundle.getEntry().get(entryIndex).getResource().getIdElement().getIdPart();
              String resourceType = bundle.getEntry().get(entryIndex).getResource().getResourceType().toString();
              ArrayList newExpressions = new ArrayList<>(issue.getExpression());
              newExpressions.set(0, new StringType("Bundle.entry[" + entryIndex + "].resource.ofType(" + resourceType + ").where(id = '" + resourceId + "')" + matcher.group(2)));
              issue.setExpression(newExpressions);
            }
          }
        }
      }
    }
  }

  /**
   * Validates a resource at the specified severity threshold.
   * Uses the specified measure definition bundles as additional validation support.
   * Optionally skips reinitialization of the underlying FhirValidator.
   * This is only appropriate when validating multiple resources with the same support.
   * E.g., validating individual MeasureReport bundles all generated as part of the same report.
   * In such a case, the Validator instance should not be shared across requests.
   */
  public OperationOutcome validate(Resource resource, OperationOutcome.IssueSeverity severity, List<Bundle> support, boolean reinitialize) {
    FhirValidator validator;
    if (reinitialize || this.validator == null) {
      validator = initialize(support);
      this.validator = validator;
    } else {
      validator = this.validator;
    }

    logger.debug("Validating {}", resource.getResourceType().toString().toLowerCase());

    OperationOutcome outcome = new OperationOutcome();
    Date start = new Date();

    //noinspection unused
    outcome.setId(UUID.randomUUID().toString());

    this.validateResource(validator, resource, outcome, severity);
    this.improveIssueExpressions(resource, outcome);

    Date end = new Date();
    logger.debug("Validation took {} seconds", TimeUnit.MILLISECONDS.toSeconds(end.getTime() - start.getTime()));
    logger.debug("Validation found {} issues", outcome.getIssue().size());

    // Add extensions (which don't formally exist) that show the total issue count and severity threshold
    outcome.addExtension(Constants.OperationOutcomeTotalExtensionUrl, new IntegerType(outcome.getIssue().size()));
    outcome.addExtension(Constants.OperationOutcomeSeverityExtensionUrl, new CodeType(severity.toCode()));

    return outcome;
  }

  /**
   * Validates a resource at the specified severity threshold.
   * Uses the specified measure definition bundles as additional validation support.
   */
  public OperationOutcome validate(Resource resource, OperationOutcome.IssueSeverity severity, List<Bundle> support) {
    return validate(resource, severity, support, true);
  }

  /**
   * Validates a resource at the specified severity threshold.
   */
  public OperationOutcome validate(Resource resource, OperationOutcome.IssueSeverity severity) {
    return validate(resource, severity, List.of(), true);
  }
}
