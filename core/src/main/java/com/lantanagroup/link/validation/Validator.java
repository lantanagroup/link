package com.lantanagroup.link.validation;

import ca.uhn.fhir.context.support.DefaultProfileValidationSupport;
import ca.uhn.fhir.parser.DataFormatException;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.validation.*;
import com.lantanagroup.link.Constants;
import com.lantanagroup.link.FhirContextProvider;
import com.lantanagroup.link.config.api.ApiConfig;
import com.lantanagroup.link.db.SharedService;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.common.hapi.validation.support.CachingValidationSupport;
import org.hl7.fhir.common.hapi.validation.support.InMemoryTerminologyServerValidationSupport;
import org.hl7.fhir.common.hapi.validation.support.PrePopulatedValidationSupport;
import org.hl7.fhir.common.hapi.validation.support.ValidationSupportChain;
import org.hl7.fhir.common.hapi.validation.validator.FhirInstanceValidator;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.*;
import org.hl7.fhir.utilities.npm.NpmPackage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Validator {
  protected static final Logger logger = LoggerFactory.getLogger(Validator.class);

  private final PrePopulatedValidationSupport prePopulatedValidationSupport;

  private FhirValidator validator;

  private final IParser jsonParser = FhirContextProvider.getFhirContext().newJsonParser().setPrettyPrint(true);
  private final IParser xmlParser = FhirContextProvider.getFhirContext().newXmlParser();

  @Setter
  private SharedService sharedService;

  private final List<String> allowsResourceTypes = List.of("StructureDefinition", "ValueSet", "CodeSystem", "ImplementationGuide", "Measure", "Library");

  @Setter
  private ApiConfig apiConfig;

  private volatile boolean initialized;

  public Validator(SharedService sharedService, ApiConfig apiConfig) {
    this.sharedService = sharedService;
    this.apiConfig = apiConfig;
    this.prePopulatedValidationSupport = new PrePopulatedValidationSupport(FhirContextProvider.getFhirContext());
  }

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
        return OperationOutcome.IssueType.CODEINVALID;
      case "Extension_EXT_Unknown":
        return OperationOutcome.IssueType.EXTENSION;
      case "Measure_MR_M_NotFound":
        return OperationOutcome.IssueType.NOTFOUND;
      case "Validation_VAL_Profile_Minimum":
      case "Validation_VAL_Profile_Maximum":
      case "Extension_EXT_Type":
        return OperationOutcome.IssueType.STRUCTURE;
      case "Type_Specific_Checks_DT_String_WS":
        return OperationOutcome.IssueType.VALUE;
      case "Terminology_TX_System_Unknown":
        return OperationOutcome.IssueType.UNKNOWN;
      default:
        return OperationOutcome.IssueType.NULL;
    }
  }

  /**
   * Writes all conformance resources to a file for debugging purposes
   */
  private void writeConformanceResourcesToFile() {
    String resources = this.prePopulatedValidationSupport.fetchAllConformanceResources().stream()
            .map(Object::toString)
            .collect(Collectors.joining("\n"));
    try {
      Files.writeString(Path.of("conformance-resources.txt"), resources);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public void init() {
    if (this.initialized) {
      return;
    }

    synchronized (this) {
      if (this.initialized) {
        return;
      }

      this.loadPackages();
      this.loadTerminology();

      // When needed for debugging
      //this.writeConformanceResourcesToFile();

      this.validator = FhirContextProvider.getFhirContext().newValidator();
      this.validator.setExecutorService(Executors.newWorkStealingPool());
      IValidatorModule module = new FhirInstanceValidator(this.getValidationSupportChain());
      this.validator.registerValidatorModule(module);
      this.validator.setConcurrentBundleValidation(true);

      this.initialized = true;
    }
  }

  /**
   * Load allowed resources for all packages from the configured path into the pre-populated validation support
   */
  private void loadPackages() {
    if (StringUtils.isEmpty(this.apiConfig.getValidationPackagesPath())) {
      logger.info("No validation packages path configured");
      return;
    }

    try {
      PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
      org.springframework.core.io.Resource[] resources = resolver.getResources(this.apiConfig.getValidationPackagesPath());
      for (org.springframework.core.io.Resource packageResource : resources) {
        if (packageResource.getFile().isDirectory()) {
          continue;
        } else if (!packageResource.getFile().getName().endsWith(".tgz")) {
          logger.warn("Unexpected package file name {}", packageResource.getFilename());
          continue;
        }

        logger.info("Loading package {}", packageResource.getFilename());
        NpmPackage npmPackage = NpmPackage.fromPackage(packageResource.getInputStream());

        npmPackage.listResources(allowsResourceTypes).forEach(resource -> {
          try {
            String resourceJson = new String(npmPackage.getFolders().get("package").getContent().get(resource), StandardCharsets.UTF_8);
            IBaseResource baseResource = this.jsonParser.parseResource(resourceJson);
            this.prePopulatedValidationSupport.addResource(baseResource);
          } catch (DataFormatException ex) {
            logger.error("Error parsing package {} resource {}", packageResource, resource, ex);
          }
        });
      }
    } catch (IOException e) {
      logger.error("Error loading packages for validation: {}", e.getMessage());
    }
  }

  /**
   * Load resources from the terminology classpath directory into the pre-populated validation support
   */
  private void loadTerminology() {
    try {
      PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
      org.springframework.core.io.Resource[] resources = resolver.getResources("terminology/**");
      this.loadClassResources(resources);
    } catch (IOException e) {
      logger.error("Error loading class resources for validation: {}", e.getMessage());
    }

  }

  private void loadClassResources(org.springframework.core.io.Resource[] classResources) {
    logger.debug("Loading {} class resources for validation", classResources.length);

    List<IBaseResource> resources = new ArrayList<>();
    for (org.springframework.core.io.Resource classResource : classResources) {
      IBaseResource resource = null;

      if (StringUtils.isEmpty(classResource.getFilename()) || !classResource.isReadable()) {
        continue;
      }

      if (classResource.getFilename() != null && classResource.getFilename().endsWith(".json")) {
        try (InputStream is = classResource.getInputStream()) {
          resource = this.jsonParser.parseResource(is);
        } catch (IOException ex) {
          logger.error("Error parsing resource {}", classResource.getFilename(), ex);
        }
      } else if (classResource.getFilename() != null && classResource.getFilename().endsWith(".xml")) {
        try (InputStream is = classResource.getInputStream()) {
          resource = this.xmlParser.parseResource(is);
        } catch (IOException ex) {
          logger.error("Error parsing resource {}", classResource.getFilename(), ex);
        }
      } else {
        logger.warn("Unexpected file name {}", classResource.getFilename());
      }

      if (resource != null) {
        resources.add(resource);
      } else {
        logger.warn("Unable to parse resource {}", classResource.getFilename());
      }
    }

    logger.debug("Adding {} resources to pre-populated validation support", resources.size());
    for (IBaseResource resource : resources) {
      this.prePopulatedValidationSupport.addResource(resource);
    }
  }

  private CachingValidationSupport getValidationSupportChain() {
    ValidationSupportChain validationSupportChain = new ValidationSupportChain(
            new DefaultProfileValidationSupport(FhirContextProvider.getFhirContext()),
            new InMemoryTerminologyServerValidationSupport(FhirContextProvider.getFhirContext()),
            this.prePopulatedValidationSupport
    );
    return new CachingValidationSupport(validationSupportChain);
  }

  private void validateResource(Resource resource, OperationOutcome outcome, OperationOutcome.IssueSeverity severity) {
    ValidationOptions opts = new ValidationOptions();

    ValidationResult result = this.validator.validateWithResult(resource, opts);

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

  public OperationOutcome validate(Resource resource, OperationOutcome.IssueSeverity severity) {
    this.init();

    logger.debug("Validating {}", resource.getResourceType().toString().toLowerCase());

    OperationOutcome outcome = new OperationOutcome();
    Date start = new Date();

    //noinspection unused
    outcome.setId(UUID.randomUUID().toString());

    this.validateResource(resource, outcome, severity);
    this.improveIssueExpressions(resource, outcome);

    Date end = new Date();
    logger.debug("Validation took {} seconds", TimeUnit.MILLISECONDS.toSeconds(end.getTime() - start.getTime()));
    logger.debug("Validation found {} issues", outcome.getIssue().size());

    // Add extensions (which don't formally exist) that show the total issue count and severity threshold
    outcome.addExtension(Constants.OperationOutcomeTotalExtensionUrl, new IntegerType(outcome.getIssue().size()));
    outcome.addExtension(Constants.OperationOutcomeSeverityExtensionUrl, new CodeType(severity.toCode()));

    return outcome;
  }
}
