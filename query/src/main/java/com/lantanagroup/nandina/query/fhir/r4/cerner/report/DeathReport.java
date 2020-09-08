package com.lantanagroup.nandina.query.fhir.r4.cerner.report;

import ca.uhn.fhir.context.FhirContext;
import com.lantanagroup.nandina.query.fhir.r4.cerner.filter.CovidFilter;
import com.lantanagroup.nandina.query.fhir.r4.cerner.filter.DeathFilter;
import com.lantanagroup.nandina.query.fhir.r4.cerner.filter.Filter;
import com.lantanagroup.nandina.query.fhir.r4.cerner.scoop.EncounterScoop;

import java.util.Date;
import java.util.List;

public class DeathReport extends Report {

    /*
     *  Create this passing in an EncounterDateFilter for the date on which you are interested.
     */
    public DeathReport(EncounterScoop scoop, List<Filter> filters, Date reportDate, FhirContext ctx) {
        super(scoop, addFilters(filters, reportDate), ctx);
    }

    private static List<Filter> addFilters(List<Filter> filters, Date reportDate) {
        Filter covidFilter = new CovidFilter();
        filters.add(covidFilter);
        Filter deathFilter = new DeathFilter(reportDate);
        filters.add(deathFilter);
        return filters;
    }
}