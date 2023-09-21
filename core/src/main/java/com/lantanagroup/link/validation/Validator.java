package com.lantanagroup.link.validation;

import ca.uhn.fhir.context.support.DefaultProfileValidationSupport;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.validation.*;
import com.lantanagroup.link.Constants;
import com.lantanagroup.link.FhirContextProvider;
import com.lantanagroup.link.db.model.tenant.Validation;
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
import java.util.stream.Collectors;

public class Validator {
  protected static final Logger logger = LoggerFactory.getLogger(Validator.class);

  private InstanceValidator instanceValidator;
  private final Validation config;
  private VersionSpecificWorkerContextWrapper workerContext;
  private DefaultProfileValidationSupport defaultProfileValidationSupport;
  private CommonCodeSystemsTerminologyService commonCodeSystemsTerminologyService;
  private InMemoryTerminologyServerValidationSupport inMemoryTerminologyServerValidationSupport;
  private PrePopulatedValidationSupport prePopulatedValidationSupport;

  private IParser jsonParser = FhirContextProvider.getFhirContext().newJsonParser();
  private IParser xmlParser = FhirContextProvider.getFhirContext().newXmlParser();

  public Validator(Validation config) {
    this.config = config;

    try {
      this.defaultProfileValidationSupport = new DefaultProfileValidationSupport(FhirContextProvider.getFhirContext());
      this.commonCodeSystemsTerminologyService = new CommonCodeSystemsTerminologyService(FhirContextProvider.getFhirContext());
      this.inMemoryTerminologyServerValidationSupport = new InMemoryTerminologyServerValidationSupport(FhirContextProvider.getFhirContext());
      this.prePopulatedValidationSupport = new PrePopulatedValidationSupport(FhirContextProvider.getFhirContext());

      this.loadFiles();

      CachingValidationSupport validationSupport = this.getCachingValidationSupport();

      FHIRPathEngine.IEvaluationContext evaluationCtx = new FhirInstanceValidator.NullEvaluationContext();
      this.workerContext = VersionSpecificWorkerContextWrapper.newVersionSpecificWorkerContextWrapper(validationSupport);
      XVerExtensionManager xverManager = new XVerExtensionManager(this.workerContext);

      this.instanceValidator = new InstanceValidator(this.workerContext, evaluationCtx, xverManager);
      this.instanceValidator.setPolicyAdvisor(new PolicyAdvisor());
      this.instanceValidator.setFetcher(new ResourceFetcher(this.prePopulatedValidationSupport));
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

  private Device findDevice(Resource resource) {
    if (resource.getResourceType() != ResourceType.Bundle) {
      return null;
    }

    return ((Bundle) resource).getEntry().stream()
            .filter(e -> e.getResource() != null && e.getResource().getResourceType() == ResourceType.Device)
            .map(e -> (Device) e.getResource())
            .filter(d -> d.getMeta() != null && d.getMeta().hasProfile(Constants.SubmittingDeviceProfile))
            .findFirst()
            .orElse(null);
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

  private static OperationOutcome getOperationOutcome(List<ValidationMessage> messages, OperationOutcome.IssueSeverity severity) {
    OperationOutcome outcome = new OperationOutcome();

    List<ValidationMessage> filtered = messages.stream().filter(m -> {
      boolean isError = m.getLevel() == ValidationMessage.IssueSeverity.ERROR;
      boolean isWarning = m.getLevel() == ValidationMessage.IssueSeverity.WARNING;
      boolean isInfo = m.getLevel() == ValidationMessage.IssueSeverity.INFORMATION;

      if (severity == OperationOutcome.IssueSeverity.ERROR && !isError) {
        return false;
      } else if (severity == OperationOutcome.IssueSeverity.WARNING && !isError && !isWarning) {
        return false;
      } else if (severity == OperationOutcome.IssueSeverity.INFORMATION && !isError && !isWarning && !isInfo) {
        return false;
      }
      return true;
    }).collect(Collectors.toList());

    outcome.setIssue(filtered.stream().map(f -> {
      OperationOutcome.OperationOutcomeIssueComponent issue = new OperationOutcome.OperationOutcomeIssueComponent();
      issue.setDiagnostics(f.getMessage());
      issue.setSeverity(getIssueSeverity(f.getLevel()));
      issue.getLocation().add(new StringType(f.getLocation()));
      issue.getLocation().add(new StringType(f.getLine() + ":" + f.getCol()));
      return issue;
    }).collect(Collectors.toList()));

    return outcome;
  }

  private void validateResource(Resource resource, OperationOutcome outcome, OperationOutcome.IssueSeverity severity, Integer entryIndex) {
    ValidationOptions opts = new ValidationOptions();
    resource.getMeta().getProfile().forEach(p -> opts.addProfile(p.getValue()));

    IValidationContext<IBaseResource> validationContext = ValidationContext.forResource(FhirContextProvider.getFhirContext(), resource, opts);
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
        if (location.indexOf(".") > 0) {
          location = "Bundle.entry[" + entryIndex + "].resource." + location.substring(location.indexOf(".") + 1);
        } else {
          location = "Bundle.entry[" + entryIndex + "].resource";
        }
      }

      outcome.addIssue()
              .setSeverity(messageSeverity)
              .setDiagnostics(message.getMessage())
              .setLocation(List.of(new StringType(location)));
    }
  }

  public OperationOutcome validate(Resource resource, OperationOutcome.IssueSeverity severity) {
    logger.debug("Validating {}", resource.getResourceType().toString().toLowerCase());

    OperationOutcome outcome = new OperationOutcome();
    Date start = new Date();

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

    Device device = this.findDevice(resource);

    if (device != null) {
      logger.debug("Found submitting device in validation input, attaching to OperationOutcome as contained resource");
      outcome.addContained(device);

      if (!device.hasId()) {
        device.setId(UUID.randomUUID().toString());
      }

      outcome.addExtension("http://test.com/submitting-device", new Reference().setReference("Device/" + device.getIdElement().getIdPart()));
    }

    return outcome;
  }
}
