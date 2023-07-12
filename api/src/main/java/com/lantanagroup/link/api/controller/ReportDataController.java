package com.lantanagroup.link.api.controller;

import com.lantanagroup.link.Constants;
import com.lantanagroup.link.FhirDataProvider;
import com.lantanagroup.link.IDataProcessor;
import com.lantanagroup.link.config.datagovernance.DataGovernanceConfig;
import com.lantanagroup.link.config.query.USCoreConfig;
import com.lantanagroup.link.config.query.USCoreOtherResourceTypeConfig;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r4.model.Bundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.Duration;
import java.util.Calendar;
import java.util.Date;

@RestController
@RequestMapping("/api")
public class ReportDataController extends BaseController {
  private static final Logger logger = LoggerFactory.getLogger(ReportDataController.class);

  @Autowired
  @Setter
  private ApplicationContext context;

  @Autowired
  @Setter
  @Getter
  private DataGovernanceConfig dataGovernanceConfig;

  @Autowired
  private USCoreConfig usCoreConfig;

  // Disallow binding of sensitive attributes
  // Ex: DISALLOWED_FIELDS = new String[]{"details.role", "details.age", "is_admin"};
  final String[] DISALLOWED_FIELDS = new String[]{};
  @InitBinder
  public void initBinder(WebDataBinder binder) {
    binder.setDisallowedFields(DISALLOWED_FIELDS);
  }

  @PostMapping(value = "/data/{type}")
  public void retrieveData(@RequestBody() byte[] content, @PathVariable("type") String type) throws Exception {
    if (config.getDataProcessor() == null || config.getDataProcessor().get(type) == null || config.getDataProcessor().get(type).equals("")) {
      throw new IllegalStateException("Cannot find data processor.");
    }

    logger.debug("Receiving " + type + " data. Parsing...");

    Class<?> dataProcessorClass = Class.forName(this.config.getDataProcessor().get(type));
    IDataProcessor dataProcessor = (IDataProcessor) this.context.getBean(dataProcessorClass);

    dataProcessor.process(content, getFhirDataProvider());
  }

  /**
   * @throws DatatypeConfigurationException
   * Deletes all census lists, patient data bundles, and measure reports stored on the server if their retention period
   * has been reached.
   */
  @DeleteMapping(value = "/data/expunge")
  public void expungeSpecificData() throws DatatypeConfigurationException {
    FhirDataProvider fhirDataProvider = getFhirDataProvider();

    if(dataGovernanceConfig != null) {
      expungeDataByType(fhirDataProvider, "List", false, dataGovernanceConfig.getCensusListRetention());
      expungeDataByType(fhirDataProvider, "Bundle", true, dataGovernanceConfig.getPatientDataRetention());
      expungeDataByType(fhirDataProvider, "Patient", false, dataGovernanceConfig.getPatientDataRetention());

      // Loop uscore.patient-resource-types & other-resource-types and delete
      for(String resourceType : usCoreConfig.getPatientResourceTypes()) {
        expungeDataByType(fhirDataProvider, resourceType, false, dataGovernanceConfig.getPatientDataRetention());
      }

      for(USCoreOtherResourceTypeConfig otherResourceTypes : usCoreConfig.getOtherResourceTypes()) {
        expungeDataByType(fhirDataProvider, otherResourceTypes.getResourceType(), false, dataGovernanceConfig.getPatientDataRetention());
      }

      // Individual MeasureReport for patient will be tagged.  Others have no PHI.
      expungeDataByType(fhirDataProvider, "MeasureReport", true, dataGovernanceConfig.getReportRetention());
    }
  }

  /**
   * @param fhirDataProvider used to work with data on the server.
   * @param resourceType the type of FHIR Resource for the data.
   * @param filterPatientData if looking for patientDataBundles and not other bundles.
   * @param retention how long to keep the data.
   * @throws DatatypeConfigurationException
   * if a retention period exists for the data, expunge the data.
   * if the data being expunged are patientDataBundles, only expunge bundles with the patient data tag.
   */
  private void expungeDataByType(FhirDataProvider fhirDataProvider, String resourceType, boolean filterPatientData, String retention) throws DatatypeConfigurationException {
    Bundle bundle = fhirDataProvider.getAllResourcesByType(resourceType);
    if(bundle != null)
    {
      for (Bundle.BundleEntryComponent entry : bundle.getEntry()) {
        String tag = entry.getResource().getMeta().getTag().isEmpty()? "" :entry.getResource().getMeta().getTag().get(0).getCode();
        Date lastUpdate = entry.getResource().getMeta().getLastUpdated();

        if((StringUtils.isNotBlank(retention)) && (!filterPatientData || tag.equals(Constants.patientDataTag))) {
          expungeSpecificData(fhirDataProvider,
                  retention,
                  lastUpdate,
                  entry.getResource().getIdElement().getIdPart(),
                  entry.getResource().getResourceType().toString());
        }
      }
    }
  }

  /**
   * @param fhirDataProvider used to work with data on the server.
   * @param retentionPeriod how long to keep the data.
   * @param lastDatePosted when the data was last updated.
   * @param id id of the data.
   * @param type the type of data.
   * @throws DatatypeConfigurationException
   * Determines if the retention period has passed, if so then it expunges the data.
   */
  private void expungeSpecificData(FhirDataProvider fhirDataProvider, String retentionPeriod, Date lastDatePosted, String id, String type) throws DatatypeConfigurationException {
    Date comp = adjustTime(retentionPeriod, lastDatePosted);
    Date today = new Date();

    if(today.compareTo(comp) >= 0) {
      try {
        fhirDataProvider.deleteResource(type, id, true);
        logger.info("Resource of Type '{}' with Id of '{}' has been expunged.", type, id);
      } catch (Exception ex) {
        logger.error("Issue Deleting Resource of Type '{}' with Id of '{}'", type, id);
      }
    }
  }

  /**
   * @param retentionPeriod how long to keep the data
   * @param lastDatePosted when the data was last updated
   * @return the adjusted day from when the data had been last updated to the end of its specified retention period
   * @throws DatatypeConfigurationException
   */
  private Date adjustTime(String retentionPeriod, Date lastDatePosted) throws DatatypeConfigurationException {
    Duration dur = DatatypeFactory.newInstance().newDuration(retentionPeriod);
    Calendar calendar = Calendar.getInstance();
    calendar.setTime(lastDatePosted);

    calendar.add(Calendar.YEAR, dur.getYears());
    calendar.add(Calendar.MONTH, dur.getMonths());
    calendar.add(Calendar.DAY_OF_MONTH, dur.getDays());
    calendar.add(Calendar.HOUR_OF_DAY, dur.getHours());
    calendar.add(Calendar.MINUTE, dur.getMinutes());
    calendar.add(Calendar.SECOND, dur.getSeconds());

    return calendar.getTime();
  }
}
