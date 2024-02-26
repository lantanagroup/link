package com.lantanagroup.link.api;

import com.lantanagroup.link.StreamUtils;
import lombok.Getter;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Measure;

import java.util.List;
import java.util.stream.Collectors;

@Getter
public class MeasureDef {
  private final Measure measure;
  private final List<IBaseResource> resources;

  public MeasureDef(Bundle bundle) {
    measure = bundle.getEntry().stream()
            .map(Bundle.BundleEntryComponent::getResource)
            .filter(resource -> resource instanceof Measure)
            .map(resource -> (Measure) resource)
            .reduce(StreamUtils::toOnlyElement)
            .orElseThrow();

    // Ensure all populations have an ID
    for (Measure.MeasureGroupComponent group : measure.getGroup()) {
      for (Measure.MeasureGroupPopulationComponent population : group.getPopulation()) {
        if (!population.hasId()) {
          population.setId(population.getCode().getCodingFirstRep().getCode());
        }
      }
    }

    resources = bundle.getEntry().stream()
            .map(Bundle.BundleEntryComponent::getResource)
            .filter(resource -> !(resource instanceof Measure))
            .collect(Collectors.toList());
  }
}
