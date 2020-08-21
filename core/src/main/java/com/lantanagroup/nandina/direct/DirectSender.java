package com.lantanagroup.nandina.direct;

import ca.uhn.fhir.context.FhirContext;
import com.lantanagroup.nandina.JsonProperties;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.apache.http.entity.ContentType;
import org.hibernate.validator.internal.util.StringHelper;
import org.hl7.fhir.r4.model.QuestionnaireResponse;

import javax.naming.ConfigurationException;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

public class DirectSender {
  private FhirContext fhirContext;
  private JsonProperties.DirectProperties directProperties;

  public DirectSender(JsonProperties.DirectProperties directProperties, FhirContext fhirContext) throws ConfigurationException {
    this.directProperties = directProperties;
    this.fhirContext = fhirContext;

    if (this.directProperties == null) {
      throw new ConfigurationException("Direct integration is not configured");
    } else if (StringHelper.isNullOrEmptyString(this.directProperties.getUrl())) {
      throw new ConfigurationException("direct.url is required for Direct integration");
    } else if (StringHelper.isNullOrEmptyString(this.directProperties.getUsername())) {
      throw new ConfigurationException("direct.username is required for Direct integration");
    } else if (StringHelper.isNullOrEmptyString(this.directProperties.getPassword())) {
      throw new ConfigurationException("direct.password is required for Direct integration");
    } else if (StringHelper.isNullOrEmptyString(this.directProperties.getToAddress())) {
      throw new ConfigurationException("direct.toAddress is required for Direct integration");
    }
  }

  public void sendCSV(String subject, String message, String csvData) throws MalformedURLException, UnirestException {
    InputStream is = new ByteArrayInputStream(csvData.getBytes());
    this.send(subject, message, is, "text/plain", "report.csv");
  }

  public void sendJSON(String subject, String message, QuestionnaireResponse questionnaireResponse) throws MalformedURLException, UnirestException {
    String json = this.fhirContext.newJsonParser().encodeResourceToString(questionnaireResponse);
    InputStream is = new ByteArrayInputStream(json.getBytes());
    this.send(subject, message, is, "application/json", "report.json");
  }

  public void sendXML(String subject, String message, QuestionnaireResponse questionnaireResponse) throws MalformedURLException, UnirestException {
    String xml = this.fhirContext.newXmlParser().encodeResourceToString(questionnaireResponse);
    InputStream is = new ByteArrayInputStream(xml.getBytes());
    this.send(subject, message, is, "application/xml", "report.xml");
  }

  public void send(String subject, String message, InputStream attachmentStream, String attachmentMimeType, String attachmentFileName) throws MalformedURLException, UnirestException {
    URL attachmentUrl = new URL(new URL(this.directProperties.getUrl()), "/MailManagement/ws/v3/send/message/attachment");
    HttpResponse<JsonNode> attachmentResponse = Unirest.put(attachmentUrl.toString())
            .header("accept", "application/json")
            .basicAuth(this.directProperties.getUsername(), this.directProperties.getPassword())
            .field("attachment", attachmentStream, ContentType.create(attachmentMimeType), attachmentFileName)
            .asJson();
    JsonNode attachmentJson = attachmentResponse.getBody();

    System.out.println("test");

    /*
    URL messageUrl = new URL(new URL(this.directProperties.getUrl()), "/MailManagement/ws/v3/send/message");
    HttpResponse<JsonNode> response = Unirest.put(messageUrl.toString())
            .header("accept", "application/json")
            .basicAuth(this.directProperties.getUsername(), this.directProperties.getPassword())
            .field("to", this.directProperties.getToAddress())
            .field("subject", subject)
            .field("body", message)
            .field("attachmentId", "")
            .asJson();
    JsonNode json = response.getBody();
     */
  }
}
