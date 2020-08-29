package com.lantanagroup.nandina.direct;

import ca.uhn.fhir.context.FhirContext;
import com.lantanagroup.nandina.JsonProperties;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import org.apache.http.entity.ContentType;
import org.hibernate.validator.internal.util.StringHelper;
import org.hl7.fhir.r4.model.QuestionnaireResponse;
import org.json.JSONObject;

import javax.naming.ConfigurationException;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URL;

public class DirectSender {
  private FhirContext fhirContext;
  private JsonProperties jsonProperties;

  public DirectSender(JsonProperties jsonProperties, FhirContext fhirContext) throws ConfigurationException {
    this.jsonProperties = jsonProperties;
    this.fhirContext = fhirContext;

    if (jsonProperties.getDirect() == null) {
      throw new ConfigurationException("Direct integration is not configured");
    } else if (StringHelper.isNullOrEmptyString(jsonProperties.getDirect().get(JsonProperties.DIRECT_URL))) {
      throw new ConfigurationException("direct.url is required for Direct integration");
    } else if (StringHelper.isNullOrEmptyString(jsonProperties.getDirect().get(JsonProperties.DIRECT_USERNAME))) {
      throw new ConfigurationException("direct.username is required for Direct integration");
    } else if (StringHelper.isNullOrEmptyString(jsonProperties.getDirect().get(JsonProperties.DIRECT_PASSWORD))) {
      throw new ConfigurationException("direct.password is required for Direct integration");
    } else if (StringHelper.isNullOrEmptyString(jsonProperties.getDirect().get(JsonProperties.DIRECT_TO_ADDRESS))) {
      throw new ConfigurationException("direct.toAddress is required for Direct integration");
    }
  }

  public void sendCSV(String subject, String message, String csvData) throws Exception {
    InputStream is = new ByteArrayInputStream(csvData.getBytes());
    this.send(subject, message, is, "text/plain", "report.csv");
  }

  public void sendJSON(String subject, String message, QuestionnaireResponse questionnaireResponse) throws Exception {
    String json = this.fhirContext.newJsonParser().encodeResourceToString(questionnaireResponse);
    InputStream is = new ByteArrayInputStream(json.getBytes());
    this.send(subject, message, is, "application/json", "report.json");
  }

  public void sendXML(String subject, String message, QuestionnaireResponse questionnaireResponse) throws Exception {
    String xml = this.fhirContext.newXmlParser().encodeResourceToString(questionnaireResponse);
    InputStream is = new ByteArrayInputStream(xml.getBytes());
    this.send(subject, message, is, "application/xml", "report.xml");
  }

  public void send(String subject, String message, InputStream attachmentStream, String attachmentMimeType, String attachmentFileName) throws Exception {
    URL attachmentUrl = new URL(new URL(jsonProperties.getDirect().get(JsonProperties.DIRECT_URL)), "/MailManagement/ws/v3/send/message/attachment");
    HttpResponse<JsonNode> attachmentResponse = Unirest.put(attachmentUrl.toString())
            .header("accept", "application/json")
            .basicAuth(jsonProperties.getDirect().get(JsonProperties.DIRECT_USERNAME), jsonProperties.getDirect().get(JsonProperties.DIRECT_PASSWORD))
            .field("attachment", attachmentStream, ContentType.create(attachmentMimeType), attachmentFileName)
            .asJson();
    JSONObject attachmentJson = attachmentResponse.getBody().getObject();
    String attachmentState = (String) attachmentJson.get("state");
    String attachmentID = (String) attachmentJson.get("attachmentID");
    String attachmentDesc = (String) attachmentJson.get("description");

    if (attachmentState == null || !attachmentState.equals("PASS")) {
      throw new Exception(attachmentDesc);
    }

    URL messageUrl = new URL(new URL(this.jsonProperties.getDirect().get(JsonProperties.DIRECT_URL)), "/MailManagement/ws/v3/send/message");
    HttpResponse<JsonNode> response = Unirest.put(messageUrl.toString())
            .header("accept", "application/json")
            .basicAuth(jsonProperties.getDirect().get(JsonProperties.DIRECT_USERNAME), jsonProperties.getDirect().get(JsonProperties.DIRECT_PASSWORD))
            .field("to", jsonProperties.getDirect().get(JsonProperties.DIRECT_TO_ADDRESS))
            .field("subject", subject)
            .field("body", message)
            .field("attachmentId", attachmentID)
            .asJson();
    JSONObject messageJson = response.getBody().getObject();
    String messageState = (String) attachmentJson.get("state");
    String messageDesc = (String) attachmentJson.get("description");

    if (messageState == null || !messageState.equals("PASS")) {
      throw new Exception(messageDesc);
    }
  }
}
