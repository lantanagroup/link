package com.lantanagroup.link.cli;

import com.auth0.jwt.JWT;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.lantanagroup.link.FhirHelper;
import com.lantanagroup.link.Helper;
import com.lantanagroup.link.auth.LinkCredentials;
import com.lantanagroup.link.auth.OAuth2Helper;
import com.lantanagroup.link.model.GenerateRequest;
import com.lantanagroup.link.model.GenerateResponse;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URI;
import java.util.*;


@ShellComponent
public class GenerateAndSubmitCommand {
  private static final Logger logger = LoggerFactory.getLogger(GenerateAndSubmitCommand.class);
  private final CloseableHttpClient httpClient = HttpClients.createDefault();

  @Autowired
  @Setter
  @Getter
  private GenerateAndSubmitConfig configInfo;

  public static Date getStartDate(int adjustHours, int adjustMonths, boolean startOfDay) {
    TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
    Calendar cal = new GregorianCalendar();
    cal.add(Calendar.HOUR, adjustHours);
    cal.add(Calendar.MONTH, adjustMonths);
    if (startOfDay) {
      cal.set(Calendar.MILLISECOND, 0);
      cal.set(Calendar.SECOND, 0);
      cal.set(Calendar.MINUTE, 0);
      cal.set(Calendar.HOUR_OF_DAY, 0);
    }
    return cal.getTime();
  }

  public static Date getEndOfDay(int adjustHours, int adjustMonths, boolean endOfDay) {
    TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
    Calendar cal = new GregorianCalendar();
    cal.add(Calendar.HOUR, adjustHours);
    cal.add(Calendar.MONTH, adjustMonths);
    if (endOfDay) {
      cal.set(Calendar.HOUR_OF_DAY, 23);
      cal.set(Calendar.MINUTE, 59);
      cal.set(Calendar.SECOND, 59);
      cal.set(Calendar.MILLISECOND, 0);
    }
    return cal.getTime();
  }

  @ShellMethod("generate-and-submit")
  public void generateAndSubmit() throws IOException {
    CloseableHttpClient client = HttpClientBuilder.create().build();
    try {
      RestTemplate restTemplate = new RestTemplate();

      if (StringUtils.isBlank(configInfo.getApiUrl())) {
        logger.error("The api-url parameter is required.");
        return;
      }
      if (StringUtils.isBlank(String.valueOf(this.configInfo.getPeriodStart().getAdjustDay()))) {
        logger.error("The period-start parameter is required.");
        return;
      }
      if (StringUtils.isBlank(String.valueOf(this.configInfo.getPeriodEnd().getAdjustDay()))) {
        logger.error("The period-start parameter is required.");
        return;
      }
      if (configInfo.getAuth() == null) {
        logger.error("Auth is required.");
        return;
      }
      if (StringUtils.isBlank(configInfo.getAuth().getTokenUrl())) {
        logger.error("The token-url is required.");
        return;
      }
      if (StringUtils.isBlank(configInfo.getAuth().getUser())) {
        logger.error("The user is required.");
        return;
      }
      if (StringUtils.isBlank(configInfo.getAuth().getPass())) {
        logger.error("The password is required.");
        return;
      }
      if (StringUtils.isBlank(configInfo.getAuth().getScope())) {
        logger.error("The scope is required.");
        return;
      }
      //String token = OAuth2Helper.getPasswordCredentialsToken(client, configInfo.getAuth().getTokenUrl(), configInfo.getAuth().getUser(), configInfo.getAuth().getPass(), "nhsnlink-app", configInfo.getAuth().getScope());
      if (configInfo.getAuth() == null && configInfo.getAuth().getCredentialMode() == null) {
        logger.error("Invalid authorization configuration.");
        System.exit(1);
      }

      String token = null;

      ///TODO: Potentially change this to a implementation of an interface instead of using the helper class
      if(StringUtils.equalsIgnoreCase(configInfo.getAuth().getCredentialMode(), "password")) {
        token = OAuth2Helper.getPasswordCredentialsToken(
                httpClient,
                configInfo.getAuth().getTokenUrl(),
                configInfo.getAuth().getUser(),
                configInfo.getAuth().getPass(),
                configInfo.getAuth().getClientId(),
                configInfo.getAuth().getScope());
      }
      else if(StringUtils.equalsIgnoreCase(configInfo.getAuth().getCredentialMode(), "sams-password")) {
        token = OAuth2Helper.getSamsPasswordCredentialsToken(
                httpClient,
                configInfo.getAuth().getTokenUrl(),
                configInfo.getAuth().getUser(),
                configInfo.getAuth().getPass(),
                configInfo.getAuth().getClientId(),
                configInfo.getAuth().getClientSecret(),
                configInfo.getAuth().getScope());
      }
      else if (StringUtils.equalsIgnoreCase(configInfo.getAuth().getCredentialMode(), "client")) {
        token = OAuth2Helper.getClientCredentialsToken(
                httpClient,
                configInfo.getAuth().getTokenUrl(),
                configInfo.getAuth().getClientId(),
                configInfo.getAuth().getPass(),
                configInfo.getAuth().getScope());
      }

      if (token == null) {
        client.close();
        logger.error("Authentication failed. Please contact the system administrator.");
        System.exit(1);
      }
      DecodedJWT jwt = JWT.decode(token);
      LinkCredentials principal = new LinkCredentials();
      principal.setJwt(jwt);
      principal.setPractitioner(FhirHelper.toPractitioner(jwt));
      String url = configInfo.getApiUrl() + "/report/$generate";

      // We generate reports for 1 day now so end date will be midnight of the start date (23h.59min.59s)- the period end date will be used when we will generate reports for more than 1 day
      Date startDate = getStartDate(this.configInfo.getPeriodStart().getAdjustDay() * 24, this.configInfo.getPeriodStart().getAdjustMonth(), this.configInfo.getPeriodStart().isStartOfDay());
      String startDateFormatted = Helper.getFhirDate(startDate);
      String endDateFormatted = Helper.getFhirDate(getEndOfDay(this.configInfo.getPeriodEnd().getAdjustDay() * 24, this.configInfo.getPeriodStart().getAdjustMonth(), this.configInfo.getPeriodEnd().isEndOfDay()));

      UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(url);

      Map<String, String> uriParams = new HashMap<>();
      GenerateRequest generateRequest = new GenerateRequest();
      generateRequest.setPeriodEnd(endDateFormatted);
      generateRequest.setPeriodStart(startDateFormatted);
      generateRequest.setRegenerate(true);
      generateRequest.setBundleIds(configInfo.getBundleIds());

      URI urlWithParameters = builder.buildAndExpand(uriParams).toUri();

      // create headers
      HttpHeaders headers = new HttpHeaders();
      // set `content-type` header
      headers.setContentType(MediaType.APPLICATION_JSON);

      //bearer token header
      if (OAuth2Helper.validateHeaderJwtToken(token)) {
        headers.setBearerAuth(token);
      } else {
        client.close();
        throw new JWTVerificationException("Invalid token format");
      }

      // set `accept` header
      headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

      // build the request
      HttpEntity<GenerateRequest> entity = new HttpEntity<>(generateRequest, headers);

      // send POST request
      ResponseEntity<GenerateResponse> response = restTemplate.postForEntity(urlWithParameters, entity, GenerateResponse.class);

      String reportId = response.getBody() != null ? response.getBody().getMasterId() : null;
      if (reportId == null) {
        logger.error("Error generating report. Please contact the system administrator.");
        client.close();
        System.exit(1);
      }

      url = configInfo.getApiUrl() + "/report/{reportId}/$send";
      UriComponentsBuilder builder1 = UriComponentsBuilder.fromUriString(url);

      Map<String, Object> map = new HashMap<>();
      map.put("reportId", reportId);
      URI urlWithParameters1 = builder1.buildAndExpand(map).toUri();

      // send the report
      restTemplate.exchange(urlWithParameters1, HttpMethod.POST, entity, String.class);
      logger.info("Report successfully generated and submitted.");
      client.close();
      System.exit(0);
    } catch (Exception ex) {
      client.close();
      logger.error(String.format("Error generating and submitting report: %s", ex.getMessage()), ex);
      System.exit(1);
    } finally {
      if (client != null) {
        client.close();
      }
    }
  }
}
