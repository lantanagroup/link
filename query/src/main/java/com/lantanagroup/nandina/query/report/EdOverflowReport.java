package com.lantanagroup.nandina.query.report;

import ca.uhn.fhir.context.FhirContext;
import com.lantanagroup.nandina.query.filter.CovidFilter;
import com.lantanagroup.nandina.query.filter.EdOverflowEncounterFilter;
import com.lantanagroup.nandina.query.filter.Filter;
import com.lantanagroup.nandina.query.scoop.EncounterScoop;

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
