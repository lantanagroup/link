package com.lantanagroup.link.nhsn;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lantanagroup.link.db.model.tenant.ReportingPlan;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.apache.http.HttpHeaders;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

public class ReportingPlanService {
  private static final Logger logger = LoggerFactory.getLogger(ReportingPlanService.class);

  private final ReportingPlan config;
  private final String orgId;
  private final HttpClient httpClient = HttpClients.createDefault();
  private final ObjectMapper objectMapper = new ObjectMapper()
          .configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true)
          .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

  public ReportingPlanService(ReportingPlan config, String orgId) {
    this.config = config;
    this.orgId = orgId;
  }

  public boolean isReporting(String name, int year, int month) throws URISyntaxException, IOException {
    if (!config.isEnabled()) {
      logger.info("MRP is disabled; not checking");
      return true;
    }
    logger.info("Checking MRP");
    String accessToken = getAccessToken();
    for (Plan plan : getPlans(name, year, month, accessToken)) {
      if (!StringUtils.equals(plan.getNhsnOrgId(), orgId)) {
        continue;
      }
      if (!StringUtils.equals(plan.getName(), name)) {
        continue;
      }
      if (plan.getYear() != year) {
        continue;
      }
      if (plan.getMonth() != month) {
        continue;
      }
      return StringUtils.equalsIgnoreCase(plan.getReporting(), "Y");
    }
    return false;
  }

  private String getAccessToken() throws IOException {
    logger.info("Retrieving access token from identity provider");
    ReportingPlan.SamsAuth samsAuth = config.getSamsAuth();

    // Make an HTTP request to the token endpoint to acquire an access token.
    RestTemplate restTemplate = new RestTemplate();
    org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
    headers.add("Accept", "application/json");
    headers.add("Content-Type", "application/x-www-form-urlencoded");

    MultiValueMap<String, String> requestBody = new LinkedMultiValueMap<>();
    requestBody.add("grant_type", "password");
    requestBody.add("scope", "profile email");
    requestBody.add("username", samsAuth.getUsername());
    requestBody.add("password", samsAuth.getPassword());
    requestBody.add("client_id", samsAuth.getClientId());
    requestBody.add("client_secret", samsAuth.getClientSecret());

    HttpEntity<MultiValueMap<String, String>> tokenRequest = new HttpEntity<>(requestBody, headers);

    try
    {
      ResponseEntity<String> idpResponse = restTemplate.postForEntity(StringEscapeUtils.escapeHtml4(samsAuth.getTokenUrl()), tokenRequest, String.class);

      if (idpResponse.getStatusCode() == HttpStatus.OK) {
        logger.info("Response: {}", idpResponse.getStatusCodeValue());
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode response = objectMapper.readTree(idpResponse.getBody());
        return response.get("access_token").asText();
      }
      else {
        return null;
      }
    } catch (Exception ex) {
      logger.error("Failed to retrieve token from identity provider", ex);
      return null;
    }


//    HttpPost request = new HttpPost(StringEscapeUtils.escapeHtml4(samsAuth.getTokenUrl()));
//    List<NameValuePair> parameters = new ArrayList<>();
//    parameters.add(new BasicNameValuePair("grant_type", "password"));
//    parameters.add(new BasicNameValuePair("username", samsAuth.getUsername()));
//    parameters.add(new BasicNameValuePair("password", samsAuth.getPassword()));
//    parameters.add(new BasicNameValuePair("client_id", samsAuth.getClientId()));
//    parameters.add(new BasicNameValuePair("client_secret", samsAuth.getClientSecret()));
//    parameters.add(new BasicNameValuePair("scope", "profile email"));
//    request.setEntity(new UrlEncodedFormEntity(parameters));
//    return httpClient.execute(request, response -> {
//      logger.info("Response: {}", response.getStatusLine());
//      String body = EntityUtils.toString(response.getEntity());
//      return objectMapper.reader().readValue(body, JsonNode.class)
//              .get("access_token")
//              .asText();
//    });
  }

  private List<Plan> getPlans(String name, int year, int month, String accessToken)
          throws URISyntaxException, IOException {
    logger.info("Retrieving plans from NHSN");
    URIBuilder builder = new URIBuilder(config.getUrl());
    builder.addParameter("nhsnorgid", orgId);
    builder.addParameter("name", name);
    builder.addParameter("year", Integer.toString(year));
    builder.addParameter("month", Integer.toString(month));
    HttpGet request = new HttpGet(builder.build());
    request.addHeader(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", accessToken));
    request.addHeader("Access-Token", config.getSamsAuth().getEmailAddress());
    return httpClient.execute(request, response -> {
      logger.info("Response: {}", response.getStatusLine());
      String body = EntityUtils.toString(response.getEntity());
      return objectMapper.reader().readValue(body, Response.class).getPlans();
    });
  }

  @Getter
  @Setter
  private static class Response {
    private List<Plan> plans = List.of();
  }

  @SuppressWarnings("unused")
  @Getter
  @Setter
  private static class Plan {
    private String nhsnOrgId;
    private String name;
    private Integer year;
    private Integer month;
    private String reporting;
  }
}
