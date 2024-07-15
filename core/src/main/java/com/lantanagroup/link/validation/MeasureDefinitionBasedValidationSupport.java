package com.lantanagroup.link.validation;

import ca.uhn.fhir.context.FhirContext;
import jakarta.annotation.Nullable;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.common.hapi.validation.support.BaseValidationSupport;
import org.hl7.fhir.common.hapi.validation.support.PrePopulatedValidationSupport;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Measure;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MeasureDefinitionBasedValidationSupport extends BaseValidationSupport {
  private final PrePopulatedValidationSupport prePopulatedValidationSupport;
  private final Map<String, IBaseResource> additionalSupport;

  public MeasureDefinitionBasedValidationSupport(FhirContext fhirContext) {
    super(fhirContext);
    prePopulatedValidationSupport = new PrePopulatedValidationSupport(fhirContext);
    additionalSupport = new HashMap<>();
  }

  private static List<String> getUrls(Measure measure) {
    List<String> urls = new ArrayList<>();
    String url = measure.getUrl();
    if (StringUtils.isNotEmpty(url)) {
      urls.add(url);
      String version = measure.getVersion();
      if (StringUtils.isNotEmpty(version)) {
        urls.add(String.format("%s|%s", url, version));
      }
    }
    return urls;
  }

  public void addResource(IBaseResource resource) {
    if (resource instanceof Measure) {
      for (String url : getUrls((Measure) resource)) {
        additionalSupport.put(url, resource);
      }
      return;
    }
    prePopulatedValidationSupport.addResource(resource);
  }

  @Nullable
  @Override
  @SuppressWarnings("unchecked")
  public <T extends IBaseResource> T fetchResource(@Nullable Class<T> resourceType, String uri) {
    IBaseResource resource = additionalSupport.get(uri);
    if (resource != null) {
      return (T) resource;
    }
    return super.fetchResource(resourceType, uri);
  }
}
