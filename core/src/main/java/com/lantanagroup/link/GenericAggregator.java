package com.lantanagroup.link;

import com.lantanagroup.link.config.api.ApiConfig;
import com.lantanagroup.link.model.PatientOfInterestModel;
import com.lantanagroup.link.model.ReportContext;
import com.lantanagroup.link.model.ReportCriteria;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r4.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.text.ParseException;
import java.util.List;
import java.util.Optional;

public abstract class GenericAggregator implements IReportAggregator {
  private static final Logger logger = LoggerFactory.getLogger(GenericAggregator.class);

  @Autowired
  @Setter
  private ApiConfig config;

  protected abstract void aggregatePatientReports(MeasureReport masterMeasureReport, List<PatientOfInterestModel> patientsOfInterest);

  private void setSubject(MeasureReport masterMeasureReport) {
    if (this.config.getMeasureLocation() != null) {
      logger.debug("Creating MeasureReport.subject based on config");

      Reference subjectRef = masterMeasureReport.getSubject() != null && masterMeasureReport.getSubject().getReference() != null ?
              masterMeasureReport.getSubject() :
              new Reference();

      Boolean hasSystem = StringUtils.isNotEmpty(this.config.getMeasureLocation().getSystem());
      Boolean hasValue = StringUtils.isNotEmpty(this.config.getMeasureLocation().getValue());

      if (hasSystem || hasValue) {
        subjectRef.setIdentifier(new Identifier());

        if (hasSystem) {
          subjectRef.getIdentifier().setSystem(this.config.getMeasureLocation().getSystem());
        }

        if (hasValue) {
          subjectRef.getIdentifier().setValue(this.config.getMeasureLocation().getValue());
        }
      }

      if (StringUtils.isNotEmpty(this.config.getMeasureLocation().getName())) {
        subjectRef.setDisplay(this.config.getMeasureLocation().getName());
      }

      if (this.config.getMeasureLocation().getLatitude() != null || this.config.getMeasureLocation().getLongitude() != null) {
        Extension positionExt = new Extension(Constants.ReportPositionExtUrl);

        if (this.config.getMeasureLocation().getLongitude() != null) {
          Extension longExt = new Extension("longitude");
          longExt.setValue(new DecimalType(this.config.getMeasureLocation().getLongitude()));
          positionExt.addExtension(longExt);
        }

        if (this.config.getMeasureLocation().getLatitude() != null) {
          Extension latExt = new Extension("latitude");
          latExt.setValue(new DecimalType(this.config.getMeasureLocation().getLatitude()));
          positionExt.addExtension(latExt);
        }

        subjectRef.addExtension(positionExt);
      }

      masterMeasureReport.setSubject(subjectRef);
    }
  }

  @Override
  public MeasureReport generate(ReportCriteria criteria, ReportContext context) throws ParseException {
    // Create the master measure report
    MeasureReport masterMeasureReport = new MeasureReport();
    masterMeasureReport.setId(context.getReportId());
    masterMeasureReport.setType(MeasureReport.MeasureReportType.SUBJECTLIST);
    masterMeasureReport.setStatus(MeasureReport.MeasureReportStatus.COMPLETE);
    masterMeasureReport.setPeriod(new Period());
    masterMeasureReport.getPeriod().setStart(Helper.parseFhirDate(criteria.getPeriodStart()));
    masterMeasureReport.getPeriod().setEnd(Helper.parseFhirDate(criteria.getPeriodEnd()));

    if (context.getMeasure() != null && StringUtils.isNotEmpty(context.getMeasure().getUrl())) {
      masterMeasureReport.setMeasure(context.getMeasure().getUrl());
    }

    this.aggregatePatientReports(masterMeasureReport, context.getPatientsOfInterest());

    this.createGroupsFromMeasure(masterMeasureReport, context);

    this.setSubject(masterMeasureReport);

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

  protected abstract void createGroupsFromMeasure(MeasureReport masterMeasureReport, ReportContext context);
}
