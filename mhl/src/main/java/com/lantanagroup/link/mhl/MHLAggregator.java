package com.lantanagroup.link.mhl;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.util.BundleUtil;
import ca.uhn.fhir.util.UrlUtil;
import com.lantanagroup.link.Constants;
import com.lantanagroup.link.FhirContextProvider;
import com.lantanagroup.link.FhirDataProvider;
import com.lantanagroup.link.nhsn.ReportAggregator;
import com.lantanagroup.link.query.uscore.Query;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.r4.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

@Component
public class MHLAggregator extends ReportAggregator {
  public static final String SUPPLEMENTAL_DATA_EXTENSION_URL =
          "https://mhl.nhsnlink.org/fhir/StructureDefinition/supplemental-data";
  private static final String INITIAL_POPULATION_SYSTEM =
          "http://terminology.hl7.org/CodeSystem/measure-population";
  private static final String INITIAL_POPULATION_CODE = "initial-population";
  private static final String INITIAL_POPULATION_CHILDREN_SYSTEM =
          String.format("%s/measure-population", Constants.MainSystem);
  private static final String INITIAL_POPULATION_CHILDREN_CODE = "initial-population-children";
  private static final String TOTAL_NUMBER_OF_RECORDS_QUERIED_SYSTEM =   String.format("%s/CodeSystem/measure-population", Constants.MainSystem);;
  private static final String TOTAL_NUMBER_OF_RECORDS_QUERIED_CODE = "total-queried-records";
  private static final Logger logger = LoggerFactory.getLogger(MHLAggregator.class);

  @Autowired
  private Query query;

  @Autowired
  private FhirDataProvider fhirDataProvider;

  /**
   * Suppresses adding subject results to the "initial population children" population.
   * Otherwise, delegates to the superclass implementation.
   */
  @Override
  protected void addSubjectResult(
          MeasureReport individualMeasureReport,
          MeasureReport.MeasureReportGroupPopulationComponent aggregatePopulation) {
    if (aggregatePopulation.getCode().hasCoding(INITIAL_POPULATION_CHILDREN_SYSTEM, INITIAL_POPULATION_CHILDREN_CODE)) {
      return;
    }
    super.addSubjectResult(individualMeasureReport, aggregatePopulation);
  }

  /**
   * Counts and queries supplemental data (via {@code $everything}) for children of mothers in the initial population.
   * Stores child counts in the "initial population children" population.
   * Stores supplemental data (one bundle per mother) on the data store service.
   * Adds an extension to the individual measure report referencing the stored supplemental data bundle.
   * Delegates aggregation to the superclass implementation.
   */
  @Override
  public void aggregatePatientReports(MeasureReport masterMeasureReport, List<MeasureReport> measureReports) {
    for (MeasureReport measureReport : measureReports) {
      for (MeasureReport.MeasureReportGroupComponent group : measureReport.getGroup()) {
        for (MeasureReport.MeasureReportGroupPopulationComponent population : group.getPopulation()) {
          Coding motherCoding = getCoding(population.getCode(), INITIAL_POPULATION_SYSTEM, INITIAL_POPULATION_CODE);
          if (motherCoding == null) {
            continue;
          }
          motherCoding.setDisplay("Total maternal clinical records in cohort");
          Coding childrenCoding = new Coding()
                  .setSystem(INITIAL_POPULATION_CHILDREN_SYSTEM)
                  .setCode(INITIAL_POPULATION_CHILDREN_CODE)
                  .setDisplay("Total children clinical records in cohort");
          MeasureReport.MeasureReportGroupPopulationComponent childrenPopulation = group.addPopulation();
          childrenPopulation.getCode().addCoding(childrenCoding);
          childrenPopulation.setCount(population.getCount() == 0 ? 0 : storeSupplementalData(measureReport));

          Coding totalRecordsCoding = new Coding()
                  .setSystem(TOTAL_NUMBER_OF_RECORDS_QUERIED_SYSTEM)
                  .setCode(TOTAL_NUMBER_OF_RECORDS_QUERIED_CODE)
                  .setDisplay("Total number of records queried");
          MeasureReport.MeasureReportGroupPopulationComponent totalPopulation = group.addPopulation();
          totalPopulation.getCode().addCoding(totalRecordsCoding);
          totalPopulation.setCount(1);
          break;
        }
      }
    }
    super.aggregatePatientReports(masterMeasureReport, measureReports);
  }

  private Coding getCoding(CodeableConcept codeableConcept, String system, String code) {
    for (Coding coding : codeableConcept.getCoding()) {
      if (StringUtils.equals(coding.getSystem(), system) && StringUtils.equals(coding.getCode(), code)) {
        return coding;
      }
    }
    return null;
  }

  private int storeSupplementalData(MeasureReport measureReport) {
    try {
      Bundle supplementalData = new Bundle();
      supplementalData.setId(UUID.randomUUID().toString());
      supplementalData.setType(Bundle.BundleType.COLLECTION);
      IIdType motherId = measureReport.getSubject().getReferenceElement();
      Collection<RelatedPerson> children = getChildren(motherId);
      for (RelatedPerson child : children) {
        supplementalData.addEntry().setResource(child);
        IIdType childId = child.getPatient().getReferenceElement();
        for (Resource resource : getEverything(childId)) {
          supplementalData.addEntry().setResource(resource);
        }
      }
      fhirDataProvider.updateResource(supplementalData);
      measureReport.addExtension(
              SUPPLEMENTAL_DATA_EXTENSION_URL,
              new Reference("Bundle/" + supplementalData.getIdElement().getIdPart()));
      return children.size();
    } catch (Exception e) {
      logger.error("Failed to store supplemental data", e);
      return 0;
    }
  }

  private Patient getMother(IIdType motherId) throws Exception {
    return query.getFhirQueryClient()
            .read()
            .resource(Patient.class)
            .withId(motherId)
            .execute();
  }

  private Collection<RelatedPerson> getChildren(IIdType motherId) throws Exception {
    Bundle bundle = query.getFhirQueryClient()
            .search()
            .byUrl(String.format(
                    "RelatedPerson?_has:Patient:link:_id=%s&relationship=MTH,GESTM,NMTH",
                    UrlUtil.escapeUrlParam(motherId.getValue())))
            .returnBundle(Bundle.class)
            .execute();
    FhirContext fhirContext = FhirContextProvider.getFhirContext();
    Collection<RelatedPerson> children =
            new LinkedList<>(BundleUtil.toListOfResourcesOfType(fhirContext, bundle, RelatedPerson.class));
    while (bundle.getLink(IBaseBundle.LINK_NEXT) != null) {
      bundle = query.getFhirQueryClient()
              .loadPage()
              .next(bundle)
              .execute();
      children.addAll(BundleUtil.toListOfResourcesOfType(fhirContext, bundle, RelatedPerson.class));
    }
    return children;
  }

  private Collection<Resource> getEverything(IIdType childId) throws Exception {
    Bundle bundle = query.getFhirQueryClient()
            .operation()
            .onInstance(childId)
            .named("$everything")
            .withNoParameters(Parameters.class)
            .returnResourceType(Bundle.class)
            .execute();
    return BundleUtil.toListOfResourcesOfType(FhirContextProvider.getFhirContext(), bundle, Resource.class);
  }
}
