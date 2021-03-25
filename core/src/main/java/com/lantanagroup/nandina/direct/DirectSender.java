package com.lantanagroup.nandina.direct;

import ca.uhn.fhir.context.FhirContext;
import com.lantanagroup.nandina.config.IDirectConfig;
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
import java.util.ArrayList;
import java.util.List;

public class DirectSender {
  private FhirContext fhirContext;
  private IDirectConfig directConfig;

  public DirectSender(IDirectConfig directConfig, FhirContext fhirContext) throws ConfigurationException {
    this.directConfig = directConfig;
    this.fhirContext = fhirContext;

    if (this.directConfig == null) {
      throw new ConfigurationException("Direct integration is not configured");
    } else if (StringHelper.isNullOrEmptyString(this.directConfig.getUrl())) {
      throw new ConfigurationException("direct.url is required for Direct integration");
    } else if (StringHelper.isNullOrEmptyString(this.directConfig.getUsername())) {
      throw new ConfigurationException("direct.username is required for Direct integration");
    } else if (StringHelper.isNullOrEmptyString(this.directConfig.getPassword())) {
      throw new ConfigurationException("direct.password is required for Direct integration");
    } else if (StringHelper.isNullOrEmptyString(this.directConfig.getToAddress())) {
      throw new ConfigurationException("direct.toAddress is required for Direct integration");
    }
  }

  public void sendCSV(String subject, String message, String csvData) throws Exception {
    InputStream inputStream = new ByteArrayInputStream(csvData.getBytes());
    this.send(subject, message,
            new Attachment(inputStream, "text/csv", "report.csv"));
  }

  public void sendJSON(String subject, String message, QuestionnaireResponse questionnaireResponse) throws Exception {
    String json = this.fhirContext.newJsonParser().encodeResourceToString(questionnaireResponse);
    InputStream inputStream = new ByteArrayInputStream(json.getBytes());
    this.send(subject, message,
            new Attachment(inputStream, "application/json", "report.json"));
  }

  public void sendXML(String subject, String message, QuestionnaireResponse questionnaireResponse) throws Exception {
    String xml = this.fhirContext.newXmlParser().encodeResourceToString(questionnaireResponse);
    InputStream inputStream = new ByteArrayInputStream(xml.getBytes());
    this.send(subject, message,
            new Attachment(inputStream, "application/xml", "report.xml"));
  }

  /**
   * This method calls the send() and is responsible for attaching zip files by passing in the byte[] zipBytes. called
   * from pillbox.java
   * @param subject the subject of the email
   * @param message the email message
   * @param zipBytes the actual zip file contents
   * @throws Exception Exception is thrown when Rosetta responds with non-PASS
   */
  public void sendZip(String subject, String message, byte[] zipBytes) throws Exception {
    InputStream inputStream = new ByteArrayInputStream(zipBytes);
    this.send(subject, message,
            new Attachment(inputStream, "application/zip", "pillbox-report.zip"));
  }

  public void send(String subject, String message, Attachment ... attachments) throws Exception {
    List<String> attachmentIds = new ArrayList<>();

    for (Attachment attachment : attachments) {
      URL attachmentUrl = new URL(new URL(this.directConfig.getUrl()), "/MailManagement/ws/v3/send/message/attachment");
      HttpResponse<JsonNode> attachmentResponse = Unirest.put(attachmentUrl.toString())
              .header("accept", "application/json")
              .basicAuth(this.directConfig.getUsername(), this.directConfig.getPassword())
              .field("attachment", attachment.getInputStream(), ContentType.create(attachment.getMimeType()), attachment.getFileName())
              .asJson();
      JSONObject attachmentJson = attachmentResponse.getBody().getObject();
      String attachmentState = (String) attachmentJson.get("state");
      String attachmentId = (String) attachmentJson.get("attachmentID");
      String attachmentDesc = (String) attachmentJson.get("description");

      if (attachmentState == null || !attachmentState.equals("PASS")) {
        throw new Exception(attachmentDesc);
      }

      attachmentIds.add(attachmentId);
    }

    URL messageUrl = new URL(new URL(this.directConfig.getUrl()), "/MailManagement/ws/v3/send/message");
    String attachmentIdList = String.join(",", attachmentIds);
    HttpResponse<JsonNode> response = Unirest.put(messageUrl.toString())
            .header("accept", "application/json")
            .basicAuth(this.directConfig.getUsername(), this.directConfig.getPassword())
            .field("to", this.directConfig.getToAddress())
            .field("subject", subject)
            .field("body", message)
            .field("attachmentId", attachmentIdList)
            .asJson();
    JSONObject messageJson = response.getBody().getObject();
    String messageState = (String) messageJson.get("state");
    String messageDesc = (String) messageJson.get("description");

    if (messageState == null || !messageState.equals("PASS")) {
      throw new Exception(messageDesc);
    }
  }
}
