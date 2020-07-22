package com.lantanagroup.nandina.query.fhir.r4.cerner;

import com.lantanagroup.nandina.query.BaseQuery;
import com.lantanagroup.nandina.query.fhir.r4.cerner.filter.Filter;
import com.lantanagroup.nandina.query.fhir.r4.cerner.report.EdOverflowAndVentilatedReport;
import com.lantanagroup.nandina.query.fhir.r4.cerner.scoop.EncounterScoop;
import org.hl7.fhir.r4.model.Resource;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class EDOverflowAndVentilatedQuery extends BaseQuery {
    @Override
    public Integer execute() {
        EncounterScoop encounterScoop = (EncounterScoop) this.getContextData("scoopData");
        List<Filter> filters = new ArrayList<>();
         EdOverflowAndVentilatedReport edOverflowAndVentilatedReport = new EdOverflowAndVentilatedReport(encounterScoop, filters);
        this.addContextData("edOverflowAndVentilated", edOverflowAndVentilatedReport.getReportCount());

        return edOverflowAndVentilatedReport.getReportCount();
    }

    @Override
    protected Map<String, Resource> queryForData() {
        return null;
    }
}
