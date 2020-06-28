package com.lantanagroup.nandina;

import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.r4.model.AuditEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;

import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.List;

public class FhirHelper {
  private static final Logger logger = LoggerFactory.getLogger(FhirHelper.class);
  private static final String NAME = "name";
  private static final String SUBJECT = "sub";

  public static void recordAuditEvent(IGenericClient fhirClient, Authentication authentication, String url,
                                      String controllerMethod, String code, String display, String outcomeDescription) {
    AuditEvent auditEvent = new AuditEvent();
    auditEvent.addSubtype().setSystem(url).setDisplay(controllerMethod);
    auditEvent.addSubtype().setCode(code).setDisplay(display);
    auditEvent.setAction(AuditEvent.AuditEventAction.E);
    auditEvent.setRecorded(new Date());
    auditEvent.setOutcome(AuditEvent.AuditEventOutcome._0);
    auditEvent.setOutcomeDesc(outcomeDescription);
    List<AuditEvent.AuditEventAgentComponent> agentList = new ArrayList<>();
    AuditEvent.AuditEventAgentComponent agent = new AuditEvent.AuditEventAgentComponent();
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

    MethodOutcome outcome = fhirClient.create()
            .resource(auditEvent)
            .prettyPrint()
            .encodedJson()
            .execute();

    IIdType id = outcome.getId();
    logger.info("AuditEvent LOGGED: " + id.getValue());
  }
}
