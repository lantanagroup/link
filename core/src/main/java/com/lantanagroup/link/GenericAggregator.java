package com.lantanagroup.link;

import com.lantanagroup.link.config.api.ApiConfig;
import com.lantanagroup.link.config.query.USCoreConfig;
import com.lantanagroup.link.model.ReportContext;
import com.lantanagroup.link.model.ReportCriteria;
import org.hl7.fhir.r4.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.text.ParseException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.lantanagroup.link.Constants.LINK_VERSION_URL;
import static com.lantanagroup.link.Constants.MEASURE_VERSION_URL;

public abstract class GenericAggregator implements IReportAggregator {
  private static final Logger logger = LoggerFactory.getLogger(GenericAggregator.class);



  @Autowired
  private ApiConfig config;
  @Autowired
  private USCoreConfig usCoreConfig;

  protected abstract void aggregatePatientReports(MeasureReport masterMeasureReport, List<MeasureReport> measureReports);

  private void setSubject(MeasureReport masterMeasureReport) {
    if (this.config.getMeasureLocation() != null) {
      logger.debug("Creating MeasureReport.subject based on config");
      Reference subjectRef = masterMeasureReport.getSubject() != null && masterMeasureReport.getSubject().getReference() != null
              ? masterMeasureReport.getSubject() : new Reference();
      if (this.config.getMeasureLocation().getSystem() != null || this.config.getMeasureLocation().getValue() != null) {
        subjectRef.setIdentifier(new Identifier()
                .setSystem(this.config.getMeasureLocation().getSystem())
                .setValue(this.config.getMeasureLocation().getValue()));
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
  public MeasureReport generate(ReportCriteria criteria, ReportContext reportContext, ReportContext.MeasureContext measureContext) throws ParseException {
    // Create the master measure report
    MeasureReport masterMeasureReport = new MeasureReport();
    masterMeasureReport.setId(measureContext.getReportId());
    masterMeasureReport.setType(MeasureReport.MeasureReportType.SUBJECTLIST);
    masterMeasureReport.setStatus(MeasureReport.MeasureReportStatus.COMPLETE);
    masterMeasureReport.setPeriod(new Period());
    masterMeasureReport.getPeriod().setStart(Helper.parseFhirDate(criteria.getPeriodStart()));
    masterMeasureReport.getPeriod().setEnd(Helper.parseFhirDate(criteria.getPeriodEnd()));
    masterMeasureReport.setMeasure(measureContext.getMeasure().getUrl());

    //Get version info for measure and LINK and store as extensions in master measure report
    try {

      FhirDataProvider fhirStoreProvider = new FhirDataProvider(this.usCoreConfig.getFhirServerBase());
      String measureId = measureContext.getMeasure().getId();
      Bundle measureBundle = (Bundle) fhirStoreProvider
              .getResourceByTypeAndId("Bundle", measureId.substring(measureId.indexOf("/") + 1));
      Library measureLibrary = (Library) measureBundle.getEntry().stream().filter(e ->
                      e.getResource().getResourceType().toString().equals("Library")
                              && e.getResource().getIdElement().getIdPart().equals(measureId.substring(measureId.indexOf("/") + 1)))
              .collect(Collectors.toList()).get(0).getResource();
      masterMeasureReport.addExtension(MEASURE_VERSION_URL, new StringType(measureLibrary.getVersion()));
      masterMeasureReport.addExtension(LINK_VERSION_URL, new StringType(Helper.getVersionInfo().getVersion()));
    }catch(Exception e){
      //do nothing, just don't put version info if can't be found
      logger.debug("Couldn't find measure bundle in USCore server to add version info to master measure report");
    }
    // TODO: Swap the order of aggregatePatientReports and createGroupsFromMeasure?
    this.aggregatePatientReports(masterMeasureReport, measureContext.getPatientReports());

    this.createGroupsFromMeasure(masterMeasureReport, measureContext);

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

  protected abstract void createGroupsFromMeasure(MeasureReport masterMeasureReport, ReportContext.MeasureContext measureContext);


}
