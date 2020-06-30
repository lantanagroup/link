package com.lantanagroup.nandina.query.r4.cerner.report;

import com.lantanagroup.nandina.query.r4.cerner.filter.CovidFilter;
import com.lantanagroup.nandina.query.r4.cerner.filter.EdOverflowEncounterFilter;
import com.lantanagroup.nandina.query.r4.cerner.filter.Filter;
import com.lantanagroup.nandina.query.r4.cerner.scoop.EncounterScoop;

import java.util.List;

public class EdOverflowReport extends Report {

  public EdOverflowReport(EncounterScoop scoop, List<Filter> filters) {
    super(scoop, addFilters(filters));
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
