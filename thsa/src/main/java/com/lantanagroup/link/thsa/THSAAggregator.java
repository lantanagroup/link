package com.lantanagroup.link.thsa;

import com.lantanagroup.link.Constants;
import com.lantanagroup.link.FhirDataProvider;
import com.lantanagroup.link.GenericAggregator;
import com.lantanagroup.link.IReportAggregator;
import com.lantanagroup.link.config.thsa.THSAConfig;
import com.lantanagroup.link.model.ReportContext;
import com.lantanagroup.link.model.ReportCriteria;
import org.hl7.fhir.r4.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.text.ParseException;
import java.util.*;


@Component
public class THSAAggregator extends GenericAggregator implements IReportAggregator {

  public final String NumTotBedsOcc = "numTotBedsOcc";
  public final String NumICUBedsOcc = "numICUBedsOcc";
  public final String NumVentUse = "numVentUse";
  public final String NumVent = "numVent";
  public final String NumTotBeds = "numTotBeds";
  public final String NumICUBeds = "numICUBeds";
  public final String NumTotBedsAvail = "numTotBedsAvail";
  public final String NumICUBedsAvail = "numICUBedsAvail";
  public final String NumVentAvail = "numVentAvail";

  @Autowired
  private FhirDataProvider provider;

  @Autowired
  private THSAConfig thsaConfig;

  private CodeableConcept getTranslatedPopulationCoding(String groupCode) {
    // get the alias populationcode
    String populationCode = "";
    String populationDisplay = "";

    switch (groupCode) {
      // populationDisplay = "Hospital Beds Occupied";
      case "beds" -> populationCode = NumTotBedsOcc;
      // populationDisplay = "ICU Bed Occupancy";
      case "icu-beds" -> populationCode = NumICUBedsOcc;
      // populationDisplay = "Mechanical Ventilators in Use";
      case "vents" -> populationCode = NumVentUse;
    }

    CodeableConcept codeableConcept = new CodeableConcept();
    Coding coding = new Coding();
    coding.setCode(populationCode);
    coding.setSystem(Constants.MeasuredValues);
    coding.setDisplay(populationDisplay);
    List<Coding> codingList = new ArrayList<>();
    codingList.add(coding);
    codeableConcept.setCoding(codingList);
    return codeableConcept;
  }

  @Override
  public MeasureReport generate(ReportCriteria criteria, ReportContext reportContext, ReportContext.MeasureContext measureContext) throws ParseException {

    HashMap<String, Integer> usedInventoryMap = new HashMap<>();
    HashMap<String, Integer> totalInventoryMap = new HashMap<>();

    // store the occupied counts from aggregated individual MeasureReport's
    // ONLY beds & icu beds at this point are available, via CQL so restricting to that here.
    MeasureReport measureReport = super.generate(criteria, reportContext, measureContext);
    PopulateInventoryMap(measureReport, usedInventoryMap, Arrays.asList(NumTotBedsOcc,NumICUBedsOcc));

    // look up the bed inventory on the Fhir Server but save the original ID before to restore it
    String id = measureReport.getId();
    MeasureReport bedMeasureReport = provider.getMeasureReportById(thsaConfig.getBedInventoryReportId());
    bedMeasureReport.setId(id);

    //store the total inventory this is coming from bed-inventorydata
    PopulateInventoryMap(bedMeasureReport, totalInventoryMap, Arrays.asList(NumTotBeds,NumICUBeds));

    // Look up the Vent inventory on the Data Store FHIR server
    MeasureReport ventMeasureReport = provider.getMeasureReportById(thsaConfig.getVentInventoryReportId());
    // Store the Total # of Vents and # of Vents occupied/used from vent inventory report
    PopulateInventoryMap(ventMeasureReport, totalInventoryMap, List.of(NumVent));
    PopulateInventoryMap(ventMeasureReport, usedInventoryMap, List.of(NumVentUse));

    // compute/store the available counts
    for (MeasureReport.MeasureReportGroupComponent group1 : bedMeasureReport.getGroup()) {
      for (MeasureReport.MeasureReportGroupPopulationComponent population : group1.getPopulation()) {
        String populationCode = population.getCode().getCoding().size() > 0 ? population.getCode().getCoding().get(0).getCode() : "";
        switch (populationCode) {
          case NumTotBedsOcc, NumICUBedsOcc, NumVentUse ->
                  population.setCount(usedInventoryMap.get(populationCode) != null ? usedInventoryMap.get(populationCode) : 0);
          case NumTotBedsAvail -> {
            int available = (totalInventoryMap.get(NumTotBeds) != null ? totalInventoryMap.get(NumTotBeds) : 0) - (usedInventoryMap.get(NumTotBedsOcc) != null ? usedInventoryMap.get(NumTotBedsOcc) : 0);
            population.setCount(available);
          }
          case NumICUBedsAvail -> {
            int available = (totalInventoryMap.get(NumICUBeds) != null ? totalInventoryMap.get(NumICUBeds) : 0) - (usedInventoryMap.get(NumICUBedsOcc) != null ? usedInventoryMap.get(NumICUBedsOcc) : 0);
            population.setCount(available);
          }
          case NumVentAvail -> {
            int available = (totalInventoryMap.get(NumVent) != null ? totalInventoryMap.get(NumVent) : 0) - (usedInventoryMap.get(NumVentUse) != null ? usedInventoryMap.get(NumVentUse) : 0);
            population.setCount(available);
          }
          case NumVent -> population.setCount(totalInventoryMap.get(NumVent));
          case NumTotBeds -> population.setCount(totalInventoryMap.get(NumTotBeds));
          case NumICUBeds -> population.setCount(totalInventoryMap.get(NumICUBeds));
        }
      }
    }
    return bedMeasureReport;
  }

  private void PopulateInventoryMap(MeasureReport report, HashMap<String, Integer> inventoryMap, List<String> populationCodes) {
    for (MeasureReport.MeasureReportGroupComponent group : report.getGroup()) {
      for (MeasureReport.MeasureReportGroupPopulationComponent population : group.getPopulation()) {
        String populationCode = population.getCode().getCoding().size() > 0 ? population.getCode().getCoding().get(0).getCode() : "";
        if (populationCodes.contains(populationCode)) {
          inventoryMap.put(populationCode, population.getCount());
        }
      }
    }
  }

  protected MeasureReport.MeasureReportGroupPopulationComponent getOrCreateGroupAndPopulation(MeasureReport
                                                                                                      masterReport, MeasureReport.MeasureReportGroupPopulationComponent
                                                                                                      reportPopulation, MeasureReport.MeasureReportGroupComponent reportGroup) {
    MeasureReport.MeasureReportGroupComponent masterReportGroupValue = null;
    MeasureReport.MeasureReportGroupPopulationComponent masterReportGroupPopulationValue;
    Optional<MeasureReport.MeasureReportGroupComponent> masterReportGroup;

    String populationCode = reportPopulation.getCode().getCoding().size() > 0 ? reportPopulation.getCode().getCoding().get(0).getCode() : "";
    if (!populationCode.equals("numerator")) {
      return null;
    }
    String groupCode = reportGroup.getCode().getCoding().size() > 0 ? reportGroup.getCode().getCoding().get(0).getCode() : "";
    CodeableConcept translatedPopulationCoding = getTranslatedPopulationCoding(groupCode);
    String translatedPopulationCode = translatedPopulationCoding.getCoding().get(0).getCode();

    /* find the group by code */
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
    Optional<MeasureReport.MeasureReportGroupPopulationComponent> masterReportGroupPopulation = masterReportGroupValue.getPopulation().stream().filter(population -> population.getCode().getCoding().size() > 0 && population.getCode().getCoding().get(0).getCode().equals(translatedPopulationCode)).findFirst();
    // if empty create it
    if (masterReportGroupPopulation.isPresent()) {
      masterReportGroupPopulationValue = masterReportGroupPopulation.get();
    } else {
      masterReportGroupPopulationValue = new MeasureReport.MeasureReportGroupPopulationComponent();
      masterReportGroupPopulationValue.setCode(translatedPopulationCoding);
      masterReportGroupValue.addPopulation(masterReportGroupPopulationValue);
    }
    return masterReportGroupPopulationValue;
  }

  @Override
  protected void createGroupsFromMeasure(MeasureReport masterMeasureReport, ReportContext.MeasureContext measureContext) {
    // if there are no groups generated then gets them from the measure
    if (masterMeasureReport.getGroup().size() == 0) {
      Bundle bundle = measureContext.getReportDefBundle();
      Optional<Bundle.BundleEntryComponent> measureEntry = bundle.getEntry().stream()
              .filter(e -> e.getResource().getResourceType() == ResourceType.Measure)
              .findFirst();

      if (measureEntry.isPresent()) {
        Measure measure = (Measure) measureEntry.get().getResource();
        measure.getGroup().forEach(group -> {
          MeasureReport.MeasureReportGroupComponent groupComponent = new MeasureReport.MeasureReportGroupComponent();
          groupComponent.setCode(group.getCode());
          group.getPopulation().forEach(population -> {
            MeasureReport.MeasureReportGroupPopulationComponent populationComponent = new MeasureReport.MeasureReportGroupPopulationComponent();
            if (!population.getCode().toString().equals("numerator")) {
              if (group.getCode().getCoding() != null && group.getCode().getCoding().size() > 0) {
                populationComponent.setCode(getTranslatedPopulationCoding(group.getCode().getCoding().get(0).getCode()));
                populationComponent.setCount(0);
                groupComponent.addPopulation(populationComponent);
              }
            }

          });
          masterMeasureReport.addGroup(groupComponent);
        });
      }
    }
  }

  @Override
  protected void aggregatePatientReports(MeasureReport masterMeasureReport, List<MeasureReport> measureReports) {
    for (MeasureReport patientMeasureReportResource : measureReports) {
      for (MeasureReport.MeasureReportGroupComponent group : patientMeasureReportResource.getGroup()) {
        for (MeasureReport.MeasureReportGroupPopulationComponent population : group.getPopulation()) {
          // Check if group and population code exist in master, if not create
          MeasureReport.MeasureReportGroupPopulationComponent measureGroupPopulation = getOrCreateGroupAndPopulation(masterMeasureReport, population, group);
          // Add population.count to the master group/population count
          if (measureGroupPopulation != null) {
            measureGroupPopulation.setCount(measureGroupPopulation.getCount() + population.getCount());
          }
        }
      }
    }
  }

}
