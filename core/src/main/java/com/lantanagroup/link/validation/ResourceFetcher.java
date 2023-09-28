package com.lantanagroup.link.validation;

import org.hl7.fhir.common.hapi.validation.support.PrePopulatedValidationSupport;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.CodeSystem;
import org.hl7.fhir.r4.model.Measure;
import org.hl7.fhir.r4.model.StructureDefinition;
import org.hl7.fhir.r4.model.ValueSet;
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

/**
 * I'm actually not entirely sure _why_ this ResourceFetcher is needed. I just know that I saw it in the HAPI
 * validation implementation and it seemed to be required (through a null exception if it was not provided). I added
 * all these logs in each method to find out which of the methods validation was using, and found it was only using the
 * `resolveUrl()` method. It seems that it uses `resolveUrl()` to do a pre-check on what URLs/resources the validator
 * has available to it. If `resolveUrl()` returns false, then the validator includes a "Cannot resolve url XX" as
 * an error.
 */
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
          "http://hl7.org/fhir/sid/us-ssn"
  ));

  private PrePopulatedValidationSupport prePopulatedValidationSupport;

  public ResourceFetcher(PrePopulatedValidationSupport prePopulatedValidationSupport) {
    this.prePopulatedValidationSupport = prePopulatedValidationSupport;
    this.refreshCanonicalUrls();
  }

  public void addCanonicalUrl(String url) {
    if (!this.canonicalUrls.contains(url)) {
      this.canonicalUrls.add(url);
    }
  }

  public void refreshCanonicalUrls() {
    List<IBaseResource> allConformanceResources = this.prePopulatedValidationSupport.fetchAllConformanceResources();

    if (allConformanceResources != null) {
      allConformanceResources
              .stream()
              .filter(r -> this.getCanonicalUrl(r) != null)
              .map(this::getCanonicalUrl)
              .forEach(url -> {
                if (!this.canonicalUrls.contains(url)) {
                  this.canonicalUrls.add(url);
                }
              });
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
    boolean found = false;

    if (s1 != null && s1.indexOf("|") > 0) {
      found = this.canonicalUrls.contains(s1.substring(0, s1.lastIndexOf("|")));
    } else {
      found = this.canonicalUrls.contains(s1);
    }

    if (!found) {
      logger.warn("Did not find {}, {}, {}", s, s1, s2);
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
