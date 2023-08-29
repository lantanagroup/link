package com.lantanagroup.link.query.uscore;

import ca.uhn.fhir.rest.api.SearchStyleEnum;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.IQuery;
import ca.uhn.fhir.rest.server.exceptions.ResourceGoneException;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import com.lantanagroup.link.Constants;
import com.lantanagroup.link.*;
import com.lantanagroup.link.db.TenantService;
import com.lantanagroup.link.db.model.tenant.*;
import com.lantanagroup.link.model.ReportContext;
import com.lantanagroup.link.model.ReportCriteria;
import com.lantanagroup.link.time.Stopwatch;
import com.lantanagroup.link.time.StopwatchManager;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.r4.model.*;
import org.hl7.fhir.r4.model.Bundle.BundleType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.Period;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class PatientData {
  private static final Logger logger = LoggerFactory.getLogger(PatientData.class);

  private final ReportCriteria criteria;
  private final ReportContext context;
  private final QueryPlan plan;
  private final IGenericClient fhirQueryServer;
  private String patientId;
  private Bundle bundle;
  private EventService eventService;
  private StopwatchManager stopwatchManager;

  public PatientData(StopwatchManager stopwatchManager, EventService eventService, IGenericClient fhirQueryServer, ReportCriteria criteria, ReportContext context, FhirQuery fhirQuery) {
    this.stopwatchManager = stopwatchManager;
    this.eventService = eventService;
    this.fhirQueryServer = fhirQueryServer;
    this.criteria = criteria;
    this.context = context;
    String planId = criteria.getQueryPlanId();
    this.plan = fhirQuery.getQueryPlans().get(planId);
    if (plan == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Query plan not found: " + planId);
    }
  }

  public Bundle getBundle() {
    return bundle;
  }

  public void loadInitialData(TenantService tenantService, Patient patient) {
    this.patientId = patient.getIdElement().getIdPart();
    this.bundle = new Bundle();
    this.bundle.setType(BundleType.TRANSACTION);
    this.bundle.setIdentifier(new Identifier().setValue(this.patientId));
    this.bundle.addEntry()
            .setResource(patient)
            .getRequest()
            .setMethod(Bundle.HTTPVerb.PUT)
            .setUrl("Patient/" + this.patientId);
    loadData(tenantService, plan.getInitial());
  }

  public void loadSupplementalData(TenantService tenantService, String patientId, Bundle bundle) {
    this.patientId = patientId;
    this.bundle = bundle;
    loadData(tenantService, plan.getSupplemental());
  }

  private void loadData(TenantService tenantService, List<TypedQueryPlan> plans) {
    for (TypedQueryPlan plan : plans) {
      int beforeCount = bundle.getEntry().size();
      search(tenantService, plan);
      int afterCount = bundle.getEntry().size();
      if (afterCount == beforeCount && plan.isEarlyExit()) {
        logger.info("No resources found; exiting early");
        break;
      }
    }
    logResourceTypeCounts();
  }

  private void search(TenantService tenantService, TypedQueryPlan plan) {
    logger.info("Querying for patient {} and resource type {}", patientId, plan.getResourceType());
    try (Stopwatch stopwatch = stopwatchManager.start("query-" + plan.getResourceType())) {
      if (plan.getReferences() == null) {
        searchByParameters(plan.getResourceType(), plan.getParameters());
      } else {
        searchByReferences(plan.getResourceType(), plan.getReferences());
      }
    } catch (Exception e) {
      logger.error("Error in query", e);
    }
    try {
      if (eventService != null) {
        eventService.triggerDataEvent(tenantService, EventTypes.AfterPatientResourceQuery, bundle, criteria, context, null);
      }
    } catch (Exception e) {
      logger.error("Error in AfterPatientResourceQuery", e);
    }
  }

  private void searchByParameters(String resourceType, List<ParameterConfig> parameters) {
    String pagedName = null;
    List<List<String>> pagedIds = null;
    Map<String, List<String>> unpagedMap = new HashMap<>();
    for (ParameterConfig parameter : parameters) {
      String resourceTypeForIds = parameter.getIds();
      int pageSize = parameter.getPaged();
      if (resourceTypeForIds != null && pageSize > 0) {
        if (pagedName != null) {
          throw new IllegalStateException("Multiple paged parameters: " + resourceType);
        }
        pagedName = parameter.getName();
        pagedIds = getIds(resourceTypeForIds).stream().collect(StreamUtils.paging(pageSize));
      } else {
        List<String> values = unpagedMap.computeIfAbsent(parameter.getName(), key -> new ArrayList<>());
        values.add(String.join(",", getParameterValue(parameter)));
      }
    }
    if (pagedName == null) {
      IQuery<Bundle> response = fhirQueryServer.search()
              .forResource(resourceType)
              .whereMap(unpagedMap)
              .returnBundle(Bundle.class);
      addAllResources(response);
    } else {
      for (List<String> ids : pagedIds) {
        String joinedIds = String.join(",", ids);
        IQuery<Bundle> query = fhirQueryServer.search()
                .forResource(resourceType)
                .usingStyle(SearchStyleEnum.POST)
                .whereMap(Map.of(pagedName, List.of(joinedIds)))
                .whereMap(unpagedMap)
                .returnBundle(Bundle.class);
        addAllResources(query);
      }
    }
  }

  private void searchByReferences(String resourceType, ReferencesConfig references) {
    List<String> unpagedIds = getReferencedIds(resourceType);
    OperationType operationType = references.getOperationType();
    switch (operationType) {
      case READ:
        for (String id : unpagedIds) {
          try {
            IBaseResource resource = fhirQueryServer.read()
                    .resource(resourceType)
                    .withId(id)
                    .execute();
            addResource((Resource) resource);
          } catch (ResourceNotFoundException | ResourceGoneException e) {
            logger.error("Resource not found or gone: {}/{}", resourceType, id, e);
          }
        }
        break;
      case SEARCH:
        int pageSize = references.getPaged();
        if (pageSize > 0) {
          List<List<String>> pagedIds = unpagedIds.stream().collect(StreamUtils.paging(pageSize));
          for (List<String> ids : pagedIds) {
            IQuery<Bundle> query = fhirQueryServer.search()
                    .forResource(resourceType)
                    .usingStyle(SearchStyleEnum.POST)
                    .where(Resource.RES_ID.exactly().codes(ids))
                    .returnBundle(Bundle.class);
            addAllResources(query);
          }
        } else {
          IQuery<Bundle> query = fhirQueryServer.search()
                  .forResource(resourceType)
                  .usingStyle(SearchStyleEnum.POST)
                  .where(Resource.RES_ID.exactly().codes(unpagedIds))
                  .returnBundle(Bundle.class);
          addAllResources(query);
        }
        break;
      default:
        throw new IllegalStateException("Unrecognized operation type: " + operationType);
    }
  }

  private List<String> getParameterValue(ParameterConfig parameter) {
    String literal = parameter.getLiteral();
    if (literal != null) {
      return List.of(literal);
    }
    String variable = parameter.getVariable();
    if (variable != null) {
      String value = getVariableValue(variable);
      String format = parameter.getFormat();
      String formattedValue = format != null ? String.format(format, value) : value;
      return List.of(formattedValue);
    }
    String resourceTypeForIds = parameter.getIds();
    if (resourceTypeForIds != null) {
      return getIds(resourceTypeForIds);
    }
    throw new IllegalStateException("Invalid parameter configuration: " + parameter.getName());
  }

  private String getVariableValue(String variable) {
    switch (variable.toLowerCase()) {
      case "patientid":
        return patientId;
      case "lookbackstart":
        Period lookback = Period.parse(plan.getLookback());
        return criteria.getPeriodStartDate().minus(lookback) + "T00:00:00Z";
      case "periodstart":
        return criteria.getPeriodStartDate().toString() + "T00:00:00Z";
      case "periodend":
        return criteria.getPeriodEndDate().toString() + "T23:59:59Z";
      default:
        throw new IllegalStateException("Unrecognized parameter variable: " + variable);
    }
  }

  private List<String> getIds(String resourceType) {
    return bundle.getEntry().stream()
            .map(Bundle.BundleEntryComponent::getResource)
            .filter(resource -> StringUtils.equals(resource.fhirType(), resourceType))
            .map(this::getOriginalId)
            .distinct()
            .collect(Collectors.toList());
  }

  private String getOriginalId(Resource resource) {
    String id = resource.getIdElement().getIdPart();
    if (!(resource instanceof DomainResource)) {
      return id;
    }
    Extension extension = ((DomainResource) resource).getExtensionByUrl(Constants.OriginalResourceIdExtension);
    return extension == null ? id : extension.getValueAsPrimitive().getValueAsString();
  }

  private List<String> getReferencedIds(String resourceType) {
    return bundle.getEntry().stream()
            .flatMap(entry -> FhirHelper.collect(entry.getResource(), Reference.class).stream())
            .map(Reference::getReferenceElement)
            .filter(resourceId -> StringUtils.equals(resourceId.getResourceType(), resourceType))
            .map(IIdType::getIdPart)
            .distinct()
            .collect(Collectors.toList());
  }

  private void addAllResources(IQuery<Bundle> query) {
    Bundle bundle = query.execute();
    while (true) {
      for (Bundle.BundleEntryComponent entry : bundle.getEntry()) {
        Resource resource = entry.getResource();
        if (resource instanceof OperationOutcome) {
          logIssues((OperationOutcome) resource);
        } else {
          addResource(resource);
        }
      }
      if (bundle.getLink(IBaseBundle.LINK_NEXT) == null) {
        break;
      }
      bundle = fhirQueryServer.loadPage()
              .next(bundle)
              .execute();
    }
  }

  private void addResource(Resource resource) {
    resource.getMeta().addExtension(Constants.ReceivedDateExtensionUrl, DateTimeType.now());
    bundle.addEntry().setResource(resource);
  }

  private void logResourceTypeCounts() {
    Map<String, Long> countsByResourceType = bundle.getEntry().stream()
            .collect(Collectors.groupingBy(entry -> entry.getResource().fhirType(), Collectors.counting()));
    int resourceTypeWidth = countsByResourceType.keySet().stream()
            .mapToInt(String::length)
            .max()
            .orElse(1);
    int countWidth = 6;
    String message = countsByResourceType.entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .map(entry -> String.format(
                    "%s  %s",
                    StringUtils.rightPad(entry.getKey(), resourceTypeWidth),
                    StringUtils.leftPad(entry.getValue().toString(), countWidth)))
            .collect(Collectors.joining("\n"));
    logger.info("Resource type counts:\n{}", message);
  }

  private void logIssues(OperationOutcome operationOutcome) {
    operationOutcome.getIssue().forEach(this::logIssue);
  }

  private void logIssue(OperationOutcome.OperationOutcomeIssueComponent issue) {
    String message = FhirHelper.toString(issue);
    switch (issue.getSeverity()) {
      case FATAL:
      case ERROR:
        logger.error(message);
        break;
      case WARNING:
        logger.warn(message);
        break;
      case INFORMATION:
        logger.info(message);
        break;
      default:
        logger.debug(message);
        break;
    }
  }
}
