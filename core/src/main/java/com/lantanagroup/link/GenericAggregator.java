package com.lantanagroup.link;

import com.lantanagroup.link.config.api.ApiConfig;
import com.lantanagroup.link.db.TenantService;
import com.lantanagroup.link.db.model.PatientMeasureReport;
import com.lantanagroup.link.model.PatientOfInterestModel;
import com.lantanagroup.link.model.ReportContext;
import com.lantanagroup.link.model.ReportCriteria;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r4.model.MeasureReport;
import org.hl7.fhir.r4.model.Period;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.text.ParseException;
import java.util.Optional;

public abstract class GenericAggregator implements IReportAggregator {
  private static final Logger logger = LoggerFactory.getLogger(GenericAggregator.class);

  @Autowired
  private ApiConfig config;

  protected abstract void aggregatePatientReport(MeasureReport masterMeasureReport, MeasureReport measureReport);

  protected abstract void finishAggregation(MeasureReport masterMeasureReport);

  @Override
  public MeasureReport generate(TenantService tenantService, ReportCriteria criteria, ReportContext.MeasureContext measureContext) throws ParseException {
    // Create the master measure report
    MeasureReport masterMeasureReport = new MeasureReport();
    masterMeasureReport.setId(measureContext.getReportId());
    masterMeasureReport.setType(MeasureReport.MeasureReportType.SUBJECTLIST);
    masterMeasureReport.setStatus(MeasureReport.MeasureReportStatus.COMPLETE);
    masterMeasureReport.setPeriod(new Period());
    masterMeasureReport.getPeriod().setStart(Helper.parseFhirDate(criteria.getPeriodStart()));
    masterMeasureReport.getPeriod().setEnd(Helper.parseFhirDate(criteria.getPeriodEnd()));

    if (StringUtils.isNotEmpty(measureContext.getMeasure().getVersion())) {
      masterMeasureReport.setMeasure(measureContext.getMeasure().getUrl() + "|" + measureContext.getMeasure().getVersion());
    } else {
      masterMeasureReport.setMeasure(measureContext.getMeasure().getUrl());
    }

    for (PatientOfInterestModel poi : measureContext.getPatientsOfInterest()) {
      if (StringUtils.isEmpty(poi.getId())) {
        logger.error("Patient {} has no ID; cannot aggregate", poi);
        continue;
      }
      String pmrId = ReportIdHelper.getPatientMeasureReportId(measureContext.getReportId(), poi.getId());
      PatientMeasureReport pmr = tenantService.getPatientMeasureReport(pmrId);
      if (pmr == null) {
        logger.warn("Patient measure report not found in database: {}", pmrId);
        continue;
      }
      this.aggregatePatientReport(masterMeasureReport, pmr.getMeasureReport());
    }
    this.finishAggregation(masterMeasureReport);

    this.createGroupsFromMeasure(masterMeasureReport, measureContext);

    return masterMeasureReport;
  }

  protected MeasureReport.MeasureReportGroupPopulationComponent getOrCreateGroupAndPopulation(MeasureReport masterReport, MeasureReport.MeasureReportGroupPopulationComponent reportPopulation, MeasureReport.MeasureReportGroupComponent reportGroup) {

    String populationCode = reportPopulation.getCode().getCoding().size() > 0 ? reportPopulation.getCode().getCoding().get(0).getCode() : "";
    String groupCode = reportGroup.getCode().getCoding().size() > 0 ? reportGroup.getCode().getCoding().get(0).getCode() : "";

    MeasureReport.MeasureReportGroupComponent masterReportGroupValue = null;
    MeasureReport.MeasureReportGroupPopulationComponent masteReportGroupPopulationValue;
    // find the group by code
    Optional<MeasureReport.MeasureReportGroupComponent> masterReportGroup;
    masterReportGroup = masterReport.getGroup().stream().filter(group -> group.getCode().getCoding().size() > 0 && group.getCode().getCoding().get(0).getCode().equals(groupCode)).findFirst();
    // if empty find the group without the code
    if (masterReportGroup.isPresent()) {
      masterReportGroupValue = masterReportGroup.get();
    } else {
      if (groupCode.equals("")) {
        masterReportGroupValue = masterReport.getGroup().size() > 0 ? masterReport.getGroup().get(0) : null; // only one group with no code
      }
    }
    // if still empty create it
    if (masterReportGroupValue == null) {
      masterReportGroupValue = new MeasureReport.MeasureReportGroupComponent();
      masterReportGroupValue.setCode(reportGroup.getCode() != null ? reportGroup.getCode() : null);
      masterReport.addGroup(masterReportGroupValue);
    }
    // find population by code
    Optional<MeasureReport.MeasureReportGroupPopulationComponent> masterReportGroupPopulation = masterReportGroupValue.getPopulation().stream().filter(population -> population.getCode().getCoding().size() > 0 && population.getCode().getCoding().get(0).getCode().equals(populationCode)).findFirst();
    // if empty create it
    if (masterReportGroupPopulation.isPresent()) {
      masteReportGroupPopulationValue = masterReportGroupPopulation.get();
    } else {
      masteReportGroupPopulationValue = new MeasureReport.MeasureReportGroupPopulationComponent();
      masteReportGroupPopulationValue.setCode(reportPopulation.getCode());
      masterReportGroupValue.addPopulation(masteReportGroupPopulationValue);
    }
    return masteReportGroupPopulationValue;
  }

  protected abstract void createGroupsFromMeasure(MeasureReport masterMeasureReport, ReportContext.MeasureContext measureContext);


}
