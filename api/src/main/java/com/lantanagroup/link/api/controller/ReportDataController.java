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
        getFhirDataProvider().audit(request,
                user.getJwt(),
                FhirHelper.AuditEventTypes.Delete,
                String.format("Resource of Type '%s' with Id of '%s' has been expunged.", resourcesToDelete.getResourceType(), resourceIdentifier));
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

    Boolean hasExpungeRole = HasExpungeRole(user);

    if (!hasExpungeRole) {
      logger.error("User doesn't have proper role to expunge data");
      response.setMessage("User doesn't have proper role to expunge data");
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    if(dataGovernanceConfig != null) {
      expungeCountByTypeAndRetentionAndPatientFilter(request,
              user,
              dataGovernanceConfig.getExpungeChunkSize(),
              "List",
              dataGovernanceConfig.getCensusListRetention(),
              false);

      expungeCountByTypeAndRetentionAndPatientFilter(request,
              user,
              dataGovernanceConfig.getExpungeChunkSize(),
              "Bundle",
              dataGovernanceConfig.getPatientDataRetention(),
              true);

      // This to remove the "placeholder" Patient resources
      expungeCountByTypeAndRetentionAndPatientFilter(request,
              user,
              dataGovernanceConfig.getExpungeChunkSize(),
              "Patient",
              dataGovernanceConfig.getPatientDataRetention(),
              false);

      // Remove individual MeasureReport tied to Patient
      // Individual MeasureReport for patient will be tagged.  Others have no PHI.
      expungeCountByTypeAndRetentionAndPatientFilter(request,
              user,
              dataGovernanceConfig.getExpungeChunkSize(),
              "MeasureReport",
              dataGovernanceConfig.getMeasureReportRetention(),
              true);

      // Loop uscore.patient-resource-types & other-resource-types and delete
      for(String resourceType : usCoreConfig.getPatientResourceTypes()) {
        expungeCountByTypeAndRetentionAndPatientFilter(request,
                user,
                dataGovernanceConfig.getExpungeChunkSize(),
                resourceType,
                dataGovernanceConfig.getResourceTypeRetention(),
                false);
      }

      for(USCoreOtherResourceTypeConfig otherResourceType : usCoreConfig.getOtherResourceTypes()) {
        expungeCountByTypeAndRetentionAndPatientFilter(request,
                user,
                dataGovernanceConfig.getExpungeChunkSize(),
                otherResourceType.getResourceType(),
                dataGovernanceConfig.getOtherTypeRetention(),
                false);
      }

    }

    response.setMessage("All identified items submitted to Data Store for removal.");
    return ResponseEntity.ok(response);
  }

  private Date SubtractDurationFromNow(String retentionPeriod) throws DatatypeConfigurationException {

    Calendar rightNow = Calendar.getInstance();
    rightNow.setTime(new Date());

    Duration durationRetention = DatatypeFactory.newInstance().newDuration(retentionPeriod);

    // Subtract the duration from the current date
    rightNow.add(Calendar.YEAR, -durationRetention.getYears());
    rightNow.add(Calendar.MONTH, -durationRetention.getMonths());
    rightNow.add(Calendar.DAY_OF_MONTH, -durationRetention.getDays());
    rightNow.add(Calendar.HOUR_OF_DAY, -durationRetention.getHours());
    rightNow.add(Calendar.MINUTE, -durationRetention.getMinutes());
    rightNow.add(Calendar.SECOND, -durationRetention.getSeconds());

    return rightNow.getTime();
  }

  private void expungeCountByTypeAndRetentionAndPatientFilter(HttpServletRequest request, LinkCredentials user, Integer count, String resourceType, String retention, Boolean filterPatientTag) throws DatatypeConfigurationException {
    int bundleEntrySize = 1;
    int expunged = 0;
    Bundle bundle;
    FhirDataProvider fhirDataProvider = getFhirDataProvider();

    Date searchBeforeDate = SubtractDurationFromNow(retention);

    logger.info("Searching for {} last updated before {}, in chunks of {}", resourceType, searchBeforeDate, count);

    while (bundleEntrySize > 0) {

      if (filterPatientTag) {
        bundle = fhirDataProvider.getResourcesSummaryByCountTagLastUpdated(resourceType, count, Constants.MainSystem, Constants.patientDataTag, searchBeforeDate);
      } else {
        bundle = fhirDataProvider.getResourcesSummaryByCountLastUpdated(resourceType, count, searchBeforeDate);
      }

      if( (bundle != null) && (bundle.getEntry().size() > 0) ) {
        bundleEntrySize = bundle.getEntry().size();

        for (Bundle.BundleEntryComponent entry : bundle.getEntry()) {

          expungeResourceById(entry.getResource().getIdElement().getIdPart(),
                  entry.getResource().getResourceType().toString(),
                  request,
                  user);

          expunged++;

        }
      } else {
        bundleEntrySize = 0;
      }

    }

    logger.info("Total {} {} found and expunged.", expunged, resourceType);

  }

  private void expungeResourceById(String id, String type, HttpServletRequest request, LinkCredentials user) {
    FhirDataProvider fhirDataProvider = getFhirDataProvider();
    try {
      fhirDataProvider.deleteResource(type, id, true);
      getFhirDataProvider().audit(request,
              user.getJwt(),
              FhirHelper.AuditEventTypes.Delete,
              String.format("Resource of Type '%s' with Id of '%s' has been expunged.", type, id));
      logger.info("Resource of Type '{}' with Id of '{}' has been expunged.", type, id);
    } catch (Exception ex) {
      logger.error("Issue Deleting Resource of Type '{}' with Id of '{}'", type, id);
    }
  }

  public Boolean HasExpungeRole(LinkCredentials user) {
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
