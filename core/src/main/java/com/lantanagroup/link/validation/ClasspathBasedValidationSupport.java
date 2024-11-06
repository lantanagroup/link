package com.lantanagroup.link.validation;

import ca.uhn.fhir.context.support.ValidationSupportContext;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.parser.LenientErrorHandler;
import ca.uhn.fhir.rest.api.EncodingEnum;
import com.lantanagroup.link.FhirContextProvider;
import org.hl7.fhir.common.hapi.validation.support.PrePopulatedValidationSupport;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.ImplementationGuide;
import org.hl7.fhir.r4.model.ResourceType;
import org.hl7.fhir.utilities.npm.NpmPackage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;

public class ClasspathBasedValidationSupport extends PrePopulatedValidationSupport {
  private static final String[] FHIR_RESOURCE_TYPES = new String[]{
          ResourceType.CodeSystem.name(),
          ResourceType.ImplementationGuide.name(),
          ResourceType.StructureDefinition.name(),
          ResourceType.ValueSet.name()
  };
  private static final Logger logger = LoggerFactory.getLogger(ClasspathBasedValidationSupport.class);

  private final PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
  private final Map<String, ImplementationGuide> implementationGuidesByPackageId = new TreeMap<>();

  private ClasspathBasedValidationSupport() {
    super(FhirContextProvider.getFhirContext());
    addPackages();
    addTerminology();
  }

  public static ClasspathBasedValidationSupport getInstance() {
    return Singleton.INSTANCE;
  }

  public Collection<ImplementationGuide> getImplementationGuides() {
    return implementationGuidesByPackageId.values();
  }

  private IParser getParser(EncodingEnum encoding) {
    return encoding.newParser(getFhirContext())
            .setParserErrorHandler(new LenientErrorHandler(false));
  }

  private void addPackages() {
    logger.info("Adding packages");
    try {
      for (Resource javaResource : resolver.getResources("classpath:/packages/*.tgz")) {
        addPackage(javaResource);
      }
    } catch (IOException e) {
      logger.error("Failed to add packages", e);
    }
  }

  private void addPackage(Resource javaResource) {
    logger.info("Adding package: {}", javaResource.getDescription());
    try {
      NpmPackage npmPackage;
      try (InputStream stream = javaResource.getInputStream()) {
        npmPackage = NpmPackage.fromPackage(stream);
      }
      IParser parser = getParser(EncodingEnum.JSON);
      for (String file : npmPackage.listResources(FHIR_RESOURCE_TYPES)) {
        try {
          IBaseResource fhirResource;
          try (InputStream stream = npmPackage.loadResource(file)) {
            fhirResource = parser.parseResource(stream);
          }
          if (fhirResource instanceof ImplementationGuide) {
            ImplementationGuide implementationGuide = (ImplementationGuide) fhirResource;
            implementationGuidesByPackageId.put(implementationGuide.getPackageId(), implementationGuide);
          } else {
            addResource(fhirResource);
          }
        } catch (Exception e) {
          logger.error("Failed to add package resource: {}", file, e);
        }
      }
    } catch (IOException e) {
      logger.error("Failed to add package", e);
    }
  }

  private void addTerminology() {
    addTerminology("classpath:/terminology/*.json", EncodingEnum.JSON);
    addTerminology("classpath:/terminology/*.xml", EncodingEnum.XML);
  }

  private void addTerminology(String pattern, EncodingEnum encoding) {
    logger.info("Adding terminology: {}", pattern);
    try {
      IParser parser = getParser(encoding);
      for (Resource javaResource : resolver.getResources(pattern)) {
        try {
          IBaseResource fhirResource;
          try (InputStream stream = javaResource.getInputStream()) {
            fhirResource = parser.parseResource(stream);
          }
          addResource(fhirResource);
        } catch (Exception e) {
          logger.error("Failed to add terminology resource: {}", javaResource.getDescription(), e);
        }
      }
    } catch (IOException e) {
      logger.error("Failed to add terminology", e);
    }
  }

  @Override
  public boolean isCodeSystemSupported(ValidationSupportContext theValidationSupportContext, String theSystem) {
    return false;
  }

  @Override
  public boolean isValueSetSupported(ValidationSupportContext theValidationSupportContext, String theValueSetUrl) {
    return false;
  }

  private static class Singleton {
    public static final ClasspathBasedValidationSupport INSTANCE = new ClasspathBasedValidationSupport();
  }
}
