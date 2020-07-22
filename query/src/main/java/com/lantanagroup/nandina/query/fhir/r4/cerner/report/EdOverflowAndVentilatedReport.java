package com.lantanagroup.nandina.query.fhir.r4.cerner.report;

import com.lantanagroup.nandina.query.fhir.r4.cerner.filter.Filter;
import com.lantanagroup.nandina.query.fhir.r4.cerner.filter.VentilatedFilter;
import com.lantanagroup.nandina.query.fhir.r4.cerner.scoop.EncounterScoop;

import java.util.List;

public class EdOverflowAndVentilatedReport extends EdOverflowReport {

    public EdOverflowAndVentilatedReport(EncounterScoop scoop, List<Filter> filters) {
        super(scoop, addFilters(filters));
    }

    private static List<Filter> addFilters(List<Filter> filters) {
        Filter ventilatedFilter = new VentilatedFilter();
        filters.add(ventilatedFilter);
        return filters;
    }
}
