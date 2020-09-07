package com.lantanagroup.nandina.query.fhir.r4.cerner.report;

import ca.uhn.fhir.context.FhirContext;
import com.lantanagroup.nandina.query.fhir.r4.cerner.filter.CovidFilter;
import com.lantanagroup.nandina.query.fhir.r4.cerner.filter.EdOverflowEncounterFilter;
import com.lantanagroup.nandina.query.fhir.r4.cerner.filter.Filter;
import com.lantanagroup.nandina.query.fhir.r4.cerner.scoop.EncounterScoop;

import java.util.List;

public class EdOverflowReport extends Report {

  public EdOverflowReport(EncounterScoop scoop, List<Filter> filters, FhirContext ctx) {
    super(scoop, addFilters(filters), ctx);
    // TODO Auto-generated constructor stub
  }

  private static List<Filter> addFilters(List<Filter> filters) {
    Filter covidFilter = new CovidFilter();
    filters.add(covidFilter);
    Filter edOverflowFilter = new EdOverflowEncounterFilter();
    filters.add(edOverflowFilter);
    return filters;
  }
}
