package com.lantanagroup.nandina;

import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.r4.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;

import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.List;

public class FhirHelper {
  public enum AuditEventTypes {
    Generate,
    Export,
    Send,
    SearchLocations
  }

  private static final Logger logger = LoggerFactory.getLogger(FhirHelper.class);
  private static final String NAME = "name";
  private static final String SUBJECT = "sub";

  public static void recordAuditEvent(IGenericClient fhirClient, Authentication authentication, AuditEventTypes type, String outcomeDescription) {
    AuditEvent auditEvent = new AuditEvent();

    switch (type) {
      case Export:
        auditEvent.setType(new Coding(null, "export", null));
        break;
      case Generate:
        auditEvent.setType(new Coding(null, "generate", null));
        break;
      case Send:
        auditEvent.setType(new Coding(null, "send", null));
        break;
      case SearchLocations:
        auditEvent.setType(new Coding(null, "search-locations", null));
        break;
    }

    auditEvent.setAction(AuditEvent.AuditEventAction.E);
    auditEvent.setRecorded(new Date());
    auditEvent.setOutcome(AuditEvent.AuditEventOutcome._0);
    auditEvent.setOutcomeDesc(outcomeDescription);
    List<AuditEvent.AuditEventAgentComponent> agentList = new ArrayList<>();
    AuditEvent.AuditEventAgentComponent agent = new AuditEvent.AuditEventAgentComponent();
    agent.setRequestor(false);

    DecodedJWT jwt = (DecodedJWT) authentication.getCredentials();
    String payload = jwt.getPayload();
    byte[] decodedBytes = Base64.getDecoder().decode(payload);
    String decodedString = new String(decodedBytes);

    JsonObject jsonObject = new JsonParser().parse(decodedString).getAsJsonObject();
    if (jsonObject.has(NAME)) {
      agent.setName(jsonObject.get(NAME).toString());
    } else if (jsonObject.has(SUBJECT)) {
      agent.setName(jsonObject.get(SUBJECT).toString());
    }
    agentList.add(agent);
    auditEvent.setAgent(agentList);

    // TODO: Need to set "source" in AuditEvent. It is required by the AuditEvent resource. Some FHIR servers may reject this as-is.

    try {
      MethodOutcome outcome = fhirClient.create()
              .resource(auditEvent)
              .prettyPrint()
              .encodedJson()
              .execute();

      IIdType id = outcome.getId();
      logger.info("AuditEvent LOGGED: " + id.getValue());
    } catch (Exception ex) {
      logger.error("Failed to record AuditEvent", ex);
    }
  }

  public static Bundle bundleMeasureReport(MeasureReport measureReport, IGenericClient fhirServer) {
    Meta meta = new Meta();
    Coding tag = meta.addTag();
    tag.setCode("measure-report");
    tag.setSystem("https://nandina.org");

    Bundle bundle = new Bundle();
    bundle.setType(Bundle.BundleType.COLLECTION);
    bundle.setMeta(meta);
    bundle.addEntry().setResource(measureReport);

    List<String> resourceReferences = new ArrayList<>();

    for (Reference evaluatedResource : measureReport.getEvaluatedResource()) {
      if (!evaluatedResource.hasReference()) continue;

      if (!evaluatedResource.getReference().startsWith("#")) {
        resourceReferences.add(evaluatedResource.getReference());
      }
    }

    for (String resourceReference : resourceReferences) {
      String[] referenceSplit = resourceReference.split("/");
      IBaseResource resource = fhirServer.read().resource(referenceSplit[0]).withId(referenceSplit[1]).execute();
      bundle.addEntry().setResource((Resource) resource);
    }

    return bundle;
  }
}
