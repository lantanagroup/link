package com.lantanagroup.nandina.query.fhir.r4.cerner.report;

import com.lantanagroup.nandina.query.fhir.r4.cerner.filter.Filter;
import com.lantanagroup.nandina.query.fhir.r4.cerner.filter.OnsetFilter;
import com.lantanagroup.nandina.query.fhir.r4.cerner.scoop.EncounterScoop;

import java.util.Date;
import java.util.List;

public class OnsetReport extends Report {

    /*
     *  Create this passing in an EncounterDateFilter for the date on which you are interested.
     */
    public OnsetReport(EncounterScoop scoop, List<Filter> filters, Date reportDate) {
        super(scoop, addFilters(filters, reportDate));
    }

    private static List<Filter> addFilters(List<Filter> filters, Date reportDate) {
        Filter onsetFilter = new OnsetFilter(reportDate);
        filters.add(onsetFilter);
        return filters;
    }
}
