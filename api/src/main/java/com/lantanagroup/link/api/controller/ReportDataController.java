package com.lantanagroup.link.api.controller;

import com.lantanagroup.link.Constants;
import com.lantanagroup.link.FhirDataProvider;
import com.lantanagroup.link.FhirHelper;
import com.lantanagroup.link.IDataProcessor;
import com.lantanagroup.link.auth.LinkCredentials;
import com.lantanagroup.link.config.datagovernance.DataGovernanceConfig;
import com.lantanagroup.link.config.query.USCoreConfig;
import com.lantanagroup.link.config.query.USCoreOtherResourceTypeConfig;
import com.lantanagroup.link.model.ExpungeResourcesToDelete;
import com.lantanagroup.link.model.ExpungeResponse;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r4.model.Bundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.Duration;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
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

  @PostMapping(value = "/data/manual-expunge")
  public ResponseEntity<ExpungeResponse> manualExpunge(
          @AuthenticationPrincipal LinkCredentials user,
          HttpServletRequest request,
          @RequestBody ExpungeResourcesToDelete resourcesToDelete) throws Exception {

    ExpungeResponse response = new ExpungeResponse();
    FhirDataProvider fhirDataProvider = getFhirDataProvider();

    Boolean hasExpungeRole = HasExpungeRole(user);

    if (!hasExpungeRole) {
      logger.error("User doesn't have proper role to expunge data");
      response.setMessage("User doesn't have proper role to expunge data");
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    if (resourcesToDelete == null) {
      logger.error("Payload not provided");
      throw new Exception();
    } else if (resourcesToDelete.getResourceType() == null || resourcesToDelete.getResourceType().trim().isEmpty()) {
      logger.error("Resource type to delete not specified");
      throw new Exception();
    } else if (resourcesToDelete.getResourceIdentifiers() == null || resourcesToDelete.getResourceIdentifiers().length == 0) {
      logger.error("Resource Identifiers to delete not specified");
      throw new Exception();
    }

    for (String resourceIdentifier : resourcesToDelete.getResourceIdentifiers()) {
      try {
        fhirDataProvider.deleteResource(resourcesToDelete.getResourceType(), resourceIdentifier, true);
        this.getFhirDataProvider().audit(request, user.getJwt(), FhirHelper.AuditEventTypes.Delete, String.format("Successfully Deleted {} Resource", resourcesToDelete.getResourceType()));
        logger.info("Removing Resource of type {} with Identifier {}", resourcesToDelete.getResourceType(), resourceIdentifier);
      } catch (Exception ex) {
        logger.info("Issue Removing Resource of type {} with Identifier {}", resourcesToDelete.getResourceType(), resourceIdentifier);
        throw new Exception();
      }
    }

    response.setMessage("All specified items submitted to Data Store for removal.");
    return ResponseEntity.ok(response);

  }

  /**
   * @throws DatatypeConfigurationException
   * Deletes all census lists, patient data bundles, and measure reports stored on the server if their retention period
   * has been reached.
   */
  @DeleteMapping(value = "/data/expunge")
  public ResponseEntity<ExpungeResponse> expungeSpecificData(@AuthenticationPrincipal LinkCredentials user,
                                                             HttpServletRequest request) throws Exception {
    ExpungeResponse response = new ExpungeResponse();
    FhirDataProvider fhirDataProvider = getFhirDataProvider();

    Boolean hasExpungeRole = HasExpungeRole(user);

    if (!hasExpungeRole) {
      logger.error("User doesn't have proper role to expunge data");
      response.setMessage("User doesn't have proper role to expunge data");
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    // TODO - add auditing... somehow

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

    response.setMessage("All specified items submitted to Data Store for removal.");
    return ResponseEntity.ok(response);
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

  private Boolean HasExpungeRole(LinkCredentials user) {
    ArrayList<String> roles = (ArrayList<String>)user.getJwt().getClaim("realm_access").asMap().get("roles");

    boolean hasExpungeRole = false;
    for (String role : roles) {
      if (role.equals(dataGovernanceConfig.getExpungeRole())) {
        hasExpungeRole = true;
        break;
      }
    }

    return hasExpungeRole;

  }
}
