package com.lantanagroup.link;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.r4.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.List;

public class FhirHelper {
  public enum AuditEventTypes {
    Generate,
    Export,
    Send,
    SearchLocations,
    InitiateQuery
  }

  private static final Logger logger = LoggerFactory.getLogger(FhirHelper.class);
  private static final String NAME = "name";
  private static final String SUBJECT = "sub";

  public static void recordAuditEvent(HttpServletRequest request, IGenericClient fhirClient, Authentication authentication, AuditEventTypes type, String outcomeDescription) {
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
      case InitiateQuery:
        auditEvent.setType(new Coding(null, "initiate-query", null));
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
    }
    if (jsonObject.has(SUBJECT)) {
      agent.setAltId(jsonObject.get(SUBJECT).toString());
    }

    String remoteAddress = request.getRemoteAddr() != null? (request.getRemoteHost() != null ? request.getRemoteAddr()+"("+request.getRemoteHost() +")": request.getRemoteAddr()):"";
    if (remoteAddress != null) {
      agent.setNetwork(new AuditEvent.AuditEventAgentNetworkComponent().setAddress(remoteAddress));
    }

    if(jsonObject.has("aud")) {
      String aud = jsonObject.get("aud").getAsString();
      Identifier identifier = new Identifier().setValue(aud);
      agent.setLocation(new Reference().setIdentifier(identifier));
    }
    agentList.add(agent);
    auditEvent.setAgent(agentList);


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

  public static Bundle bundleMeasureReport(MeasureReport measureReport, IGenericClient fhirServer, FhirContext ctx, String fhirServerStoreBase) {
    Meta meta = new Meta();
    Coding tag = meta.addTag();
    tag.setCode("measure-report");
    tag.setSystem("https://nhsnlink.org");

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

    Bundle patientBundle = generateBundle(resourceReferences);

    IGenericClient cqfRulerClient = ctx.newRestfulGenericClient(fhirServerStoreBase);
    Bundle patientBundleResponse = cqfRulerClient.transaction().withBundle(patientBundle).execute();

    patientBundleResponse.getEntry().parallelStream().forEach(entry -> {
      bundle.addEntry().setResource((Resource) entry.getResource());
    });

    return bundle;
  }

  private static Bundle generateBundle(List<String> resourceReferences ) {
    Bundle bundle = new Bundle();
    bundle.setType(Bundle.BundleType.TRANSACTION);
    resourceReferences.parallelStream().forEach(reference -> {
      String[] referenceSplit = reference.split("/");
      bundle.addEntry().getRequest().setMethod(Bundle.HTTPVerb.GET).setUrl(referenceSplit[0] + "/" + referenceSplit[1]);
    });
    return bundle;
  }
}
