package com.lantanagroup.nandina.query.fhir.r4.cerner;

import com.lantanagroup.nandina.query.BaseQuery;
import com.lantanagroup.nandina.query.fhir.r4.cerner.filter.Filter;
import com.lantanagroup.nandina.query.fhir.r4.cerner.report.DeathReport;
import com.lantanagroup.nandina.query.fhir.r4.cerner.scoop.EncounterScoop;
import org.hl7.fhir.r4.model.Resource;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DeathsQuery extends BaseQuery {

    @Override
    public Integer execute() {
        if (!this.criteria.containsKey("reportDate")) return null;

        EncounterScoop encounterScoop = (EncounterScoop) this.getContextData("scoopData");
        List<Filter> filters = new ArrayList<Filter>();
        DeathReport deathReport = new DeathReport(encounterScoop, filters, java.sql.Date.valueOf(LocalDate.parse(this.criteria.get("reportDate")).minusDays(1)));
        this.addContextData("deaths", deathReport.getReportCount());

        return deathReport.getReportCount();
    }

    @Override
    protected Map<String, Resource> queryForData() {
        return null;
    }
}