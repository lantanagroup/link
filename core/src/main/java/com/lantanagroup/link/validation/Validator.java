package com.lantanagroup.link.validation;

import ca.uhn.fhir.context.support.DefaultProfileValidationSupport;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.validation.IValidationContext;
import ca.uhn.fhir.validation.ValidationContext;
import ca.uhn.fhir.validation.ValidationOptions;
import com.lantanagroup.link.FhirContextProvider;
import com.lantanagroup.link.db.SharedService;
import lombok.Setter;
import org.apache.commons.codec.Charsets;
import org.apache.commons.io.input.ReaderInputStream;
import org.hl7.fhir.common.hapi.validation.support.*;
import org.hl7.fhir.common.hapi.validation.validator.FhirInstanceValidator;
import org.hl7.fhir.common.hapi.validation.validator.VersionSpecificWorkerContextWrapper;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.*;
import org.hl7.fhir.r5.elementmodel.Manager;
import org.hl7.fhir.r5.utils.FHIRPathEngine;
import org.hl7.fhir.r5.utils.XVerExtensionManager;
import org.hl7.fhir.utilities.validation.ValidationMessage;
import org.hl7.fhir.validation.instance.InstanceValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class Validator {
  protected static final Logger logger = LoggerFactory.getLogger(Validator.class);

  private InstanceValidator instanceValidator;
  private VersionSpecificWorkerContextWrapper workerContext;
  private DefaultProfileValidationSupport defaultProfileValidationSupport;
  private CommonCodeSystemsTerminologyService commonCodeSystemsTerminologyService;
  private InMemoryTerminologyServerValidationSupport inMemoryTerminologyServerValidationSupport;
  private PrePopulatedValidationSupport prePopulatedValidationSupport;

  private final IParser jsonParser = FhirContextProvider.getFhirContext().newJsonParser().setPrettyPrint(true);
  private final IParser xmlParser = FhirContextProvider.getFhirContext().newXmlParser();
  private final ResourceFetcher resourceFetcher;

  @Setter
  private SharedService sharedService;

  public Validator(SharedService sharedService) {
    this.sharedService = sharedService;
    this.prePopulatedValidationSupport = new PrePopulatedValidationSupport(FhirContextProvider.getFhirContext());
    this.resourceFetcher = new ResourceFetcher(this.prePopulatedValidationSupport);

    try {
      this.defaultProfileValidationSupport = new DefaultProfileValidationSupport(FhirContextProvider.getFhirContext());
      this.commonCodeSystemsTerminologyService = new CommonCodeSystemsTerminologyService(FhirContextProvider.getFhirContext());
      this.inMemoryTerminologyServerValidationSupport = new InMemoryTerminologyServerValidationSupport(FhirContextProvider.getFhirContext());

      this.loadFiles();
      this.resourceFetcher.refreshCanonicalUrls();
      this.updateMeasures();

      CachingValidationSupport validationSupport = this.getCachingValidationSupport();

      FHIRPathEngine.IEvaluationContext evaluationCtx = new FhirInstanceValidator.NullEvaluationContext();
      this.workerContext = VersionSpecificWorkerContextWrapper.newVersionSpecificWorkerContextWrapper(validationSupport);
      XVerExtensionManager xverManager = new XVerExtensionManager(this.workerContext);

      this.instanceValidator = new InstanceValidator(this.workerContext, evaluationCtx, xverManager);
      this.instanceValidator.setFetcher(this.resourceFetcher);
      //this.instanceValidator.setDebug(true);
      this.instanceValidator.setAnyExtensionsAllowed(true);

      org.hl7.fhir.utilities.validation.ValidationOptions opts =
              new org.hl7.fhir.utilities.validation.ValidationOptions()
                      .noCheckValueSetMembership();
      this.instanceValidator.setBaseOptions(opts);
    } catch (IOException ex) {
      logger.error("Error initializing validator", ex);
    }
  }

  public void updateMeasures() {
    // Add each measure canonical url to the resource fetcher
    this.sharedService.getMeasureDefinitions().forEach(md -> {
      md.getBundle().getEntry()
              .stream()
              .filter(e -> e.getResource().getResourceType() == ResourceType.Measure)
              .map(e -> ((Measure) e.getResource()).getUrl())
              .forEach(this.resourceFetcher::addCanonicalUrl);
    });
  }

  private void loadClassResources(org.springframework.core.io.Resource[] classResources) throws IOException {
    logger.debug("Loading {} class resources for validation", classResources.length);

    for (org.springframework.core.io.Resource classResource : classResources) {
      IBaseResource resource = null;

      if (!classResource.isFile()) {
        continue;
      }

      if (classResource.getFilename() != null && classResource.getFilename().endsWith(".json")) {
        try (InputStream is = new FileInputStream(classResource.getFile())) {
          resource = this.jsonParser.parseResource(is);
        }
      } else if (classResource.getFilename() != null && classResource.getFilename().endsWith(".xml")) {
        try (InputStream is = new FileInputStream(classResource.getFile())) {
          resource = this.xmlParser.parseResource(is);
        }
      }

      if (resource != null) {
        this.prePopulatedValidationSupport.addResource(resource);
      }
    }
  }

  private void loadFiles() {
    try {
      PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
      this.loadClassResources(resolver.getResources("terminology/**"));
      this.loadClassResources(resolver.getResources("profiles/**"));
    } catch (IOException ex) {
      logger.error("Error loading resources for validation", ex);
    }
  }

  private CachingValidationSupport getCachingValidationSupport() throws IOException {
    ValidationSupportChain validationSupportChain = new ValidationSupportChain(
            this.defaultProfileValidationSupport,
            this.inMemoryTerminologyServerValidationSupport,
            this.prePopulatedValidationSupport
    );
    return new CachingValidationSupport(validationSupportChain);
  }

  private static OperationOutcome.IssueSeverity getIssueSeverity(ValidationMessage.IssueSeverity severity) {
    switch (severity) {
      case ERROR:
        return OperationOutcome.IssueSeverity.ERROR;
      case WARNING:
        return OperationOutcome.IssueSeverity.WARNING;
      case INFORMATION:
        return OperationOutcome.IssueSeverity.INFORMATION;
      case FATAL:
        return OperationOutcome.IssueSeverity.FATAL;
      case NULL:
        return OperationOutcome.IssueSeverity.NULL;
      default:
        throw new RuntimeException("Unexpected severity " + severity);
    }
  }

  private static OperationOutcome.IssueType getIssueCode(ValidationMessage.IssueType issueType) {
    return OperationOutcome.IssueType.fromCode(issueType.toCode());
  }

  private void validateResource(Resource resource, OperationOutcome outcome, OperationOutcome.IssueSeverity severity, Integer entryIndex) {
    ValidationOptions opts = new ValidationOptions();

    String resourceJson = this.jsonParser.encodeResourceToString(resource);
    IValidationContext<IBaseResource> validationContext = ValidationContext.forText(FhirContextProvider.getFhirContext(), resourceJson, opts);

    // Have to encode to string here with pretty printing turned on, otherwise the validation messages don't have line/col info
    String resourceString = validationContext.getResourceAsString();
    InputStream inputStream = new ReaderInputStream(new StringReader(resourceString), Charsets.UTF_8);
    Manager.FhirFormat format = Manager.FhirFormat.JSON;

    List<ValidationMessage> messages = new ArrayList<>();
    this.instanceValidator.validate(null, messages, inputStream, format);

    for (ValidationMessage message : messages) {
      OperationOutcome.IssueSeverity messageSeverity = getIssueSeverity(message.getLevel());

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

      String location = message.getLocation();

      if (entryIndex != null) {
        String ofTypeString = ".ofType(" + resource.getResourceType().toString() + ")";

        if (location.indexOf(".") > 0) {
          location = "Bundle.entry[" + entryIndex + "].resource" + ofTypeString + "." + location.substring(location.indexOf(".") + 1);
        } else {
          location = "Bundle.entry[" + entryIndex + "].resource" + ofTypeString;
        }
      }

      outcome.addIssue()
              .setSeverity(messageSeverity)
              .setCode(getIssueCode(message.getType()))
              .setDetails(new CodeableConcept().setText(message.getMessage()))
              .setExpression(List.of(
                      new StringType(location),
                      new StringType(message.getLine() + ":" + message.getCol())));
    }
  }

  public OperationOutcome validate(Resource resource, OperationOutcome.IssueSeverity severity) {
    logger.debug("Validating {}", resource.getResourceType().toString().toLowerCase());

    this.resourceFetcher.logCanonicalUrls();

    OperationOutcome outcome = new OperationOutcome();
    Date start = new Date();

    //noinspection unused
    outcome.setId(UUID.randomUUID().toString());

    if (resource instanceof Bundle) {
      Bundle bundle = (Bundle) resource;
      for (int i = 0; i < bundle.getEntry().size(); i++) {
        Bundle.BundleEntryComponent entry = bundle.getEntry().get(i);
        this.validateResource(entry.getResource(), outcome, severity, i);
      }
    }

    this.validateResource(resource, outcome, severity, null);

    Date end = new Date();
    logger.debug("Validation took {} seconds", TimeUnit.MILLISECONDS.toSeconds(end.getTime() - start.getTime()));
    logger.debug("Validation found {} issues", outcome.getIssue().size());

    // Add extensions (which don't formally exist) that show the total issue count and severity threshold
    outcome.addExtension("http://nhsnlink.org/oo-total", new IntegerType(outcome.getIssue().size()));
    outcome.addExtension("http://nhsnlink.org/oo-severity", new CodeType(severity.toCode()));

    return outcome;
  }
}
