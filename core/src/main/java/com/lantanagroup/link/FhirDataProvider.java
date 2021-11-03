package com.lantanagroup.link;

import ca.uhn.fhir.rest.api.CacheControlDirective;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.api.SummaryEnum;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.DateClientParam;
import ca.uhn.fhir.rest.gclient.ICriterion;
import ca.uhn.fhir.rest.gclient.ReferenceClientParam;
import com.auth0.jwt.interfaces.DecodedJWT;
import lombok.Getter;
import org.hl7.fhir.r4.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

public class FhirDataProvider {
  private static final Logger logger = LoggerFactory.getLogger(FhirDataProvider.class);
  private static final String PeriodStartParamName = "periodStart";
  private static final String PeriodEndParamName = "periodEnd";
  private static final String ReportDocRefSystem = "urn:ietf:rfc:3986";

  @Getter
  private IGenericClient client;

  public FhirDataProvider(IGenericClient client) {
    this.client = client;
  }

  public Resource createResource(Resource resource) {
    MethodOutcome outcome = this.client
            .create()
            .resource(resource)
            .execute();

    if (!outcome.getCreated() || outcome.getResource() == null) {
      logger.error("Failed to store/create FHIR resource");
    } else {
      logger.debug("Stored FHIR resource with new ID of " + outcome.getResource().getIdElement().getIdPart());
    }

    return (Resource) outcome.getResource();
  }

  public void updateResource(Resource resource) {
    int initialVersion = resource.getMeta().getVersionId() != null?Integer.parseInt(resource.getMeta().getVersionId()):0;

    // Make sure the ID is not version-specific
    if (resource.getIdElement() != null && resource.getIdElement().getIdPart() != null) {
      resource.setId(resource.getIdElement().getIdPart());
    }

    MethodOutcome outcome = this.client
            .update()
            .resource(resource)
            .execute();

    DomainResource domainResource = (DomainResource) outcome.getResource();
    int updatedVersion = Integer.parseInt(outcome.getId().getVersionIdPart());
    if (updatedVersion > initialVersion) {
      logger.debug(String.format("Update is successful for %s/%s", domainResource.getResourceType().toString(), domainResource.getIdElement().getIdPart()));
    } else {
      logger.error(String.format("Failed to update FHIR resource %s/%s", domainResource.getResourceType().toString(), domainResource.getIdElement().getIdPart()));
    }
  }

  public DocumentReference findDocRefByMeasureAndPeriod(Identifier identifier, String periodStart, String periodEnd) throws Exception {
    DocumentReference documentReference = null;
    DateClientParam periodStartParam = new DateClientParam(PeriodStartParamName);
    DateClientParam periodEndParam = new DateClientParam(PeriodEndParamName);

    Bundle bundle = this.client
            .search()
            .forResource(DocumentReference.class)
            .where(DocumentReference.IDENTIFIER.exactly().systemAndValues(identifier.getSystem(), identifier.getValue()))
            .and(periodStartParam.afterOrEquals().second(periodStart))
            .and(periodEndParam.beforeOrEquals().second(periodEnd))
            .returnBundle(Bundle.class)
            .cacheControl(new CacheControlDirective().setNoCache(true))
            .execute();

    int size = bundle.getEntry().size();
    if (size > 0) {
      if (size == 1) {
        documentReference = (DocumentReference) bundle.getEntry().get(0).getResource();
      } else {
        throw new Exception("We have more than 1 report for the selected measure and report date.");
      }
    }

    return documentReference;
  }

  public DocumentReference findDocRefForReport(String reportId) {
    Bundle bundle = this.client
            .search()
            .forResource(DocumentReference.class)
            .where(DocumentReference.IDENTIFIER.exactly().systemAndValues(ReportDocRefSystem, reportId))
            .returnBundle(Bundle.class)
            .cacheControl(new CacheControlDirective().setNoCache(true))
            .execute();

    if (bundle.getEntry().size() != 1) {
      return null;
    }

    return (DocumentReference) bundle.getEntryFirstRep().getResource();
  }

  public Bundle findBundleByIdentifier(String system, String value) {
    Bundle bundle = (Bundle) this.client
            .search()
            .forResource(Bundle.class)
            .where(Bundle.IDENTIFIER.exactly().systemAndValues(system, value))
            .execute();

    if (bundle.getEntry().size() != 1) {
      return null;
    }

    return (Bundle) bundle.getEntryFirstRep().getResource();
  }

  public MeasureReport getMeasureReportById(String reportId) {
    MeasureReport report = this.client
            .read()
            .resource(MeasureReport.class)
            .withId(reportId)
            .execute();

    return report;
  }

  public Bundle getMeasureReportsByIds(List<String> reportIds) {
    Bundle response = this.client
            .search()
            .forResource(MeasureReport.class)
            .where(Resource.RES_ID.exactly().codes(reportIds))
            .returnBundle(Bundle.class)
            .cacheControl(new CacheControlDirective().setNoCache(true))
            .execute();

    return response;
  }

  public Measure getMeasureForReport(DocumentReference docRef) {
    logger.debug(String.format("Getting Measure for DocumentReference %s", docRef.getId()));

    if (docRef.getIdentifier().size() > 0) {
      for (Identifier identifier : docRef.getIdentifier()) {
        Bundle matchingMeasures = this.client
                .search()
                .forResource(Measure.class)
                .where(DocumentReference.IDENTIFIER.exactly().systemAndValues(identifier.getSystem(), identifier.getValue()))
                .returnBundle(Bundle.class)
                .summaryMode(SummaryEnum.TRUE)
                .execute();

        if (matchingMeasures.getEntry().size() == 1) {
          return (Measure) matchingMeasures.getEntry().get(0).getResource();
        }
      }
    } else {
      logger.warn("No identifier specified on DocumentReference");
    }

    return null;
  }

  public Bundle transaction(Bundle txBundle) {
    Bundle responseBundle = this.client
            .transaction()
            .withBundle(txBundle)
            .execute();

    return responseBundle;
  }

  public Measure findMeasureByIdentifier(Identifier identifier) {
    Bundle measureBundle = this.client.search()
            .forResource("Measure")
            .where(Measure.IDENTIFIER.exactly().systemAndIdentifier(identifier.getSystem(), identifier.getValue()))
            .returnBundle(Bundle.class)
            .cacheControl(new CacheControlDirective().setNoCache(true))
            .execute();

    if (measureBundle.getEntry().size() != 1) {
      return null;
    }

    return (Measure) measureBundle.getEntryFirstRep().getResource();
  }

  public void audit(HttpServletRequest request, DecodedJWT jwt, FhirHelper.AuditEventTypes type, String outcomeDescription) {
    AuditEvent auditEvent = FhirHelper.createAuditEvent(request, jwt, type, outcomeDescription);
    this.createResource(auditEvent);
  }

  public Bundle getPatientResources(ICriterion<ReferenceClientParam> criterion, String resourceType) {
    return this.client
            .search()
            .forResource(resourceType)
            .where(criterion)
            .returnBundle(Bundle.class)
            .cacheControl(new CacheControlDirective().setNoCache(true))
            .execute();
  }
}
