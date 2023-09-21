package com.lantanagroup.link.validation;

import org.hl7.fhir.common.hapi.validation.support.NpmPackageValidationSupport;
import org.hl7.fhir.common.hapi.validation.support.PrePopulatedValidationSupport;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.*;
import org.hl7.fhir.r5.elementmodel.Element;
import org.hl7.fhir.r5.model.CanonicalResource;
import org.hl7.fhir.r5.utils.validation.IResourceValidator;
import org.hl7.fhir.r5.utils.validation.IValidatorResourceFetcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class ResourceFetcher implements IValidatorResourceFetcher {
  protected static final Logger logger = LoggerFactory.getLogger(ResourceFetcher.class);
  private List<String> canonicalUrls = new ArrayList<>(List.of(
          "http://hl7.org/fhir/StructureDefinition/data-absent-reason",
          "https://www.cdc.gov/nhsn/OrgID",
          "https://nhsnlink.org",
          "http://hospital.smarthealthit.org",
          "https://www.cdc.gov/nhsn/fhir/nhsnlink/StructureDefinition/link-version",
          "https://www.cdc.gov/nhsn/fhir/nhsnlink/StructureDefinition/measure-version",
          "urn:ietf:rfc:3986",
          "http://www.cdc.gov/nhsn/fhirportal/dqm/ig/Measure/NHSNRespiratoryPathogenSurveillanceInitialPopulation",
          "http://hl7.org/fhir/R4/v3/ActCode/cs.html"   // TODO: fix?
  ));
  private PrePopulatedValidationSupport prePopulatedValidationSupport;

  public ResourceFetcher(PrePopulatedValidationSupport prePopulatedValidationSupport) {
    this.prePopulatedValidationSupport = prePopulatedValidationSupport;

    List<IBaseResource> allConformanceResources = this.prePopulatedValidationSupport.fetchAllConformanceResources();

    if (allConformanceResources != null) {
      this.canonicalUrls.addAll(
              allConformanceResources
                      .stream()
                      .filter(r -> this.getCanonicalUrl(r) != null)
                      .map(this::getCanonicalUrl)
                      .collect(Collectors.toList())
      );
    }
  }

  @Override
  public Element fetch(IResourceValidator iResourceValidator, Object o, String s) throws FHIRException, IOException {
    logger.debug("fetch {}", s);
    return null;
  }

  private String getCanonicalUrl(IBaseResource resource) {
    if (resource instanceof StructureDefinition) {
      return ((StructureDefinition) resource).getUrl();
    } else if (resource instanceof CodeSystem) {
      return ((CodeSystem) resource).getUrl();
    } else if (resource instanceof ValueSet) {
      return ((ValueSet) resource).getUrl();
    } else if (resource instanceof Measure) {
      return ((Measure) resource).getUrl();
    }
    return null;
  }

  @Override
  public boolean resolveURL(IResourceValidator iResourceValidator, Object o, String s, String s1, String s2) throws IOException, FHIRException {
    logger.debug("resolveURL {}, {}, {}", s, s1, s2);

    boolean found = false;

    if (s1 != null && s1.indexOf("|") > 0) {
      found = this.canonicalUrls.contains(s1.substring(0, s1.lastIndexOf("|")));
    } else {
      found = this.canonicalUrls.contains(s1);
    }

    return found;
  }

  @Override
  public byte[] fetchRaw(IResourceValidator iResourceValidator, String s) throws IOException {
    logger.debug("fetchRaw {}", s);
    return new byte[0];
  }

  @Override
  public IValidatorResourceFetcher setLocale(Locale locale) {
    logger.debug("setLocale {}", locale);
    return null;
  }

  @Override
  public CanonicalResource fetchCanonicalResource(IResourceValidator iResourceValidator, String s) throws URISyntaxException {
    logger.debug("fetchCanonicalResource {}", s);
    return null;
  }

  @Override
  public boolean fetchesCanonicalResource(IResourceValidator iResourceValidator, String s) {
    logger.debug("fetchesCanonicalResource {}", s);
    return false;
  }
}
