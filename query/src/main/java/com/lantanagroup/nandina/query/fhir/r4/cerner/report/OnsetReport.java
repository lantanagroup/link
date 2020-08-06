package com.lantanagroup.nandina.query.fhir.r4.cerner.report;

import com.lantanagroup.nandina.query.fhir.r4.cerner.filter.CovidFilter;
import com.lantanagroup.nandina.query.fhir.r4.cerner.filter.Filter;
import com.lantanagroup.nandina.query.fhir.r4.cerner.filter.OnsetFilter;
import com.lantanagroup.nandina.query.fhir.r4.cerner.scoop.EncounterScoop;

import java.time.LocalDate;
import java.util.Date;
import java.util.List;

public class OnsetReport extends Report {

    /**
     * Create this passing in an EncounterDateFilter for the date on which you are interested.
     * @param scoop
     * @param filters
     * @param reportDate
     */
    public OnsetReport(EncounterScoop scoop, List<Filter> filters, Date reportDate) {
        super(scoop, addFilters(filters, reportDate));
    }

    /**
     * This constructor is used when getting the values for the previousDayOnset
     * @param scoop
     * @param filters
     * @param reportDate
     * @param previousDay
     */
    public OnsetReport(EncounterScoop scoop, List<Filter> filters, Date reportDate, LocalDate previousDay) {
        super(scoop, addFilters(filters, reportDate, previousDay));
    }

    /*
    This method is used for finding hospital onsets
     */
    private static List<Filter> addFilters(List<Filter> filters, Date reportDate) {
        Filter covidFilter = new CovidFilter();
        filters.add(covidFilter);
        Filter onsetFilter = new OnsetFilter(reportDate);
        filters.add(onsetFilter);
        return filters;
    }

    /*
    This method is used for finding previous day onsets only
     */
    private static List<Filter> addFilters(List<Filter> filters, Date reportDate, LocalDate previousDay) {
        Filter covidFilter = new CovidFilter();
        filters.add(covidFilter);
        Filter onsetFilter = new OnsetFilter(reportDate, previousDay);
        filters.add(onsetFilter);
        return filters;
    }
}
