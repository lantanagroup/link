package com.lantanagroup.link.thsa;

import com.lantanagroup.link.Constants;
import com.lantanagroup.link.*;
import com.lantanagroup.link.model.PatientOfInterestModel;
import com.lantanagroup.link.model.ReportContext;
import com.lantanagroup.link.model.ReportCriteria;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;


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

  private CodeableConcept getTranslatedPopulationCoding(String groupCode) {
    // get the alias populationcode
    String populationCode = "";
    String populationDisplay = "";

    if (groupCode.equals("beds")) {
      populationCode = NumTotBedsOcc;
      // populationDisplay = "Hospital Beds Occupied";
    } else if (groupCode.equals("icu-beds")) {
      populationCode = NumICUBedsOcc;
      // populationDisplay = "ICU Bed Occupancy";
    } else if (groupCode.equals("vents")) {
      populationCode = NumVentUse;
      // populationDisplay = "Mechanical Ventilators in Use";
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
  public MeasureReport generate(ReportCriteria criteria, ReportContext context) throws ParseException {

    HashMap usedInventoryMap = new HashMap();
    HashMap totalInventoryMap = new HashMap();
    // store the used counts
    MeasureReport measureReport = super.generate(criteria, context);
    for (MeasureReport.MeasureReportGroupComponent group : measureReport.getGroup()) {
      for (MeasureReport.MeasureReportGroupPopulationComponent population : group.getPopulation()) {
        String populationCode = population.getCode().getCoding().size() > 0 ? population.getCode().getCoding().get(0).getCode() : "";
        usedInventoryMap.put(populationCode, population.getCount());
      }
    }
    // look up the inventory on the Fhir Server
    MeasureReport masterMeasureReport = provider.getMeasureReportById(context.getInventoryId());

    //store the total inventory
    for (MeasureReport.MeasureReportGroupComponent group : masterMeasureReport.getGroup()) {
      for (MeasureReport.MeasureReportGroupPopulationComponent population : group.getPopulation()) {
        String populationCode = population.getCode().getCoding().size() > 0 ? population.getCode().getCoding().get(0).getCode() : "";
        if (populationCode.equals(NumTotBeds)) {
          totalInventoryMap.put(NumTotBeds, population.getCount());
        } else if (populationCode.equals(NumICUBeds)) {
          totalInventoryMap.put(NumICUBeds, population.getCount());
        } else if (populationCode.equals(NumVent)) {
          totalInventoryMap.put(NumVent, population.getCount());
        }
      }
    }

    // store the available counts
    for (MeasureReport.MeasureReportGroupComponent group1 : masterMeasureReport.getGroup()) {
      for (MeasureReport.MeasureReportGroupPopulationComponent population : group1.getPopulation()) {
        String populationCode = population.getCode().getCoding().size() > 0 ? population.getCode().getCoding().get(0).getCode() : "";
        if (populationCode.equals(NumTotBedsOcc)) {
          population.setCount(usedInventoryMap.get(populationCode) != null ? (Integer) usedInventoryMap.get(populationCode) : 0);
        } else if (populationCode.equals(NumICUBedsOcc)) {
          population.setCount(usedInventoryMap.get(populationCode) != null ? (Integer) usedInventoryMap.get(populationCode) : 0);
        } else if (populationCode.equals(NumVentUse)) {
          population.setCount(usedInventoryMap.get(populationCode) != null ? (Integer) usedInventoryMap.get(populationCode) : 0);
        } else if (populationCode.equals(NumTotBedsAvail)) {
          int available = (totalInventoryMap.get(NumTotBeds) != null ? (Integer) totalInventoryMap.get(NumTotBeds) : 0) - (usedInventoryMap.get(NumTotBedsOcc) != null ? (Integer) usedInventoryMap.get(NumTotBedsOcc) : 0);
          population.setCount(available);
        } else if (populationCode.equals(NumICUBedsAvail)) {
          int available = (totalInventoryMap.get(NumICUBeds) != null ? (Integer) totalInventoryMap.get(NumICUBeds) : 0) - (usedInventoryMap.get(NumICUBedsOcc) != null ? (Integer) usedInventoryMap.get(NumICUBedsOcc) : 0);
          population.setCount(available);
        } else if (populationCode.equals(NumVentAvail)) {
          int available = (totalInventoryMap.get(NumVent) != null ? (Integer) totalInventoryMap.get(NumVent) : 0) - (usedInventoryMap.get(NumVentUse) != null ? (Integer) usedInventoryMap.get(NumVentUse) : 0);
          population.setCount(available);
        }
      }
    }
    return masterMeasureReport;
  }

  protected MeasureReport.MeasureReportGroupPopulationComponent getOrCreateGroupAndPopulation(MeasureReport
                                                                                                      masterReport, MeasureReport.MeasureReportGroupPopulationComponent
                                                                                                      reportPopulation, MeasureReport.MeasureReportGroupComponent reportGroup) {
    MeasureReport.MeasureReportGroupComponent masterReportGroupValue = null;
    MeasureReport.MeasureReportGroupPopulationComponent masteReportGroupPopulationValue;
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
      masteReportGroupPopulationValue = masterReportGroupPopulation.get();
    } else {
      masteReportGroupPopulationValue = new MeasureReport.MeasureReportGroupPopulationComponent();
      masteReportGroupPopulationValue.setCode(translatedPopulationCoding);
      masterReportGroupValue.addPopulation(masteReportGroupPopulationValue);
    }
    return masteReportGroupPopulationValue;
  }

  @Override
  protected void createGroupsFromMeasure(MeasureReport masterMeasureReport, ReportContext context) {
    // if there are no groups generated then gets them from the measure
    if (masterMeasureReport.getGroup().size() == 0) {
      Bundle bundle = context.getReportDefBundle();
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
            if (!population.getCode().equals("numerator")) {
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
  protected void aggregatePatientReports(MeasureReport masterMeasureReport, List<PatientOfInterestModel> patientOfInterestModelList) {
    List<String> reportIds = patientOfInterestModelList.stream().filter(patient -> !StringUtils.isEmpty(patient.getId())).map(patient -> masterMeasureReport.getId() + "-" + patient.getId().hashCode()).collect(Collectors.toList());
    Bundle patientMeasureReportsBundle = provider.getMeasureReportsByIds(reportIds);
    List<IBaseResource> bundles = FhirHelper.getAllPages(patientMeasureReportsBundle, provider, FhirContextProvider.getFhirContext());
    for (IBaseResource patientMeasureReportResource : bundles) {
      for (MeasureReport.MeasureReportGroupComponent group : ((MeasureReport) patientMeasureReportResource).getGroup()) {
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
