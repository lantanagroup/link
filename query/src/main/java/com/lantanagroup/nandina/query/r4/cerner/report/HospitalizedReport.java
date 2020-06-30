package com.lantanagroup.nandina.query.r4.cerner.report;

import com.lantanagroup.nandina.query.r4.cerner.filter.CovidFilter;
import com.lantanagroup.nandina.query.r4.cerner.filter.Filter;
import com.lantanagroup.nandina.query.r4.cerner.filter.HospitalizedEncounterFilter;
import com.lantanagroup.nandina.query.r4.cerner.scoop.EncounterScoop;

import java.util.List;

public class HospitalizedReport extends Report {

  /*
   *  Create this passing in an EncounterDateFilter for the date on which you are interested.
   */
  public HospitalizedReport(EncounterScoop scoop, List<Filter> filters) {
    super(scoop, addFilters(filters));
  }


  private static List<Filter> addFilters(List<Filter> filters) {
    Filter covidFilter = new CovidFilter();
    filters.add(covidFilter);
    Filter hospitalizedFilter = new HospitalizedEncounterFilter();
    filters.add(hospitalizedFilter);
    return filters;
  }


}
