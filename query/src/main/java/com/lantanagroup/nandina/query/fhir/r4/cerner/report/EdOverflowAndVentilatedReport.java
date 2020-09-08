package com.lantanagroup.nandina.query.fhir.r4.cerner.report;

import ca.uhn.fhir.context.FhirContext;
import com.lantanagroup.nandina.query.fhir.r4.cerner.filter.CovidFilter;
import com.lantanagroup.nandina.query.fhir.r4.cerner.filter.Filter;
import com.lantanagroup.nandina.query.fhir.r4.cerner.filter.VentilatedFilter;
import com.lantanagroup.nandina.query.fhir.r4.cerner.scoop.EncounterScoop;

import java.util.List;

public class EdOverflowAndVentilatedReport extends EdOverflowReport {

    public EdOverflowAndVentilatedReport(EncounterScoop scoop, List<Filter> filters, FhirContext ctx) {
        super(scoop, addFilters(filters), ctx);
    }

    private static List<Filter> addFilters(List<Filter> filters) {
        Filter covidFilter = new CovidFilter();
        filters.add(covidFilter);
        Filter ventilatedFilter = new VentilatedFilter();
        filters.add(ventilatedFilter);
        return filters;
    }
}
