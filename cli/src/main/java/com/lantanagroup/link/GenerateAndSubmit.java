package com.lantanagroup.link;

import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.lantanagroup.link.auth.LinkCredentials;
import com.lantanagroup.link.auth.OAuth2Helper;
import com.lantanagroup.link.model.GenerateResponse;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class GenerateAndSubmit {

  private static final Logger logger = LoggerFactory.getLogger(GenerateAndSubmit.class);

  public static void main(String[] args) {

    try {
      Options options = new Options();
      CommandLineParser parser = new DefaultParser();
      RestTemplate restTemplate = new RestTemplate();
      options.addOption("config", true, "Path to Cli.yml file.");

      CommandLine cmd = parser.parse(options, args);
      String config = cmd.getOptionValue("config");

      if (Strings.isBlank(config)) {
        logger.error("The 'config' parameter is required.");
        return;
      }

      Path configPath = Paths.get(config);
      String line = Files.readString(configPath);
      ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
      mapper.setPropertyNamingStrategy(PropertyNamingStrategy.KEBAB_CASE);

      CliConfig configInfo = mapper.readValue(line, CliConfig.class);

      if (Strings.isBlank(configInfo.getApiUrl())) {
        logger.error("The api-url parameter is required.");
        return;
      }
      if (Strings.isBlank(configInfo.getPeriodStart())) {
        logger.error("The period-start parameter is required.");
        return;
      }
      if (Strings.isBlank(configInfo.getReportTypeId())) {
        logger.error("The report-type-is is required.");
        return;
      }
      if (configInfo.getAuth() == null) {
        logger.error("Auth is required.");
        return;
      }
      if (Strings.isBlank(configInfo.getAuth().getTokenUrl())) {
        logger.error("The token-url is required.");
        return;
      }
      if (Strings.isBlank(configInfo.getAuth().getUser())) {
        logger.error("The user is required.");
        return;
      }
      if (Strings.isBlank(configInfo.getAuth().getPass())) {
        logger.error("The password is required.");
        return;
      }
      if (Strings.isBlank(configInfo.getAuth().getScope())) {
        logger.error("The scope is required.");
        return;
      }
      int periodStartDate = Integer.parseInt(configInfo.getPeriodStart());
      if (periodStartDate % 24 != 0) {
        logger.error("Period start date should be multiple of 24.");
        return;
      }
      HttpClient client = HttpClientBuilder.create().build();
      String token = OAuth2Helper.getPasswordCredentialsToken(client, configInfo.getAuth().getTokenUrl(), configInfo.getAuth().getUser(), configInfo.getAuth().getPass(), "nhsnlink-app", configInfo.getAuth().getScope());
      if (token == null) {
        logger.error("Authentication failed. Please contact the system administrator.");
        System.exit(1);
      }
      DecodedJWT jwt = JWT.decode(token);
      LinkCredentials principal = new LinkCredentials();
      principal.setJwt(jwt);
      principal.setPractitioner(FhirHelper.toPractitioner(jwt));
      String url = configInfo.getApiUrl() + "/report/$generate";

      // We generate reports for 1 day now so end date will be midnight of the start date (23h.59min.59s)- the period end date will be used when we will generate reports for more than 1 day
      Date startDate = getStartDate(periodStartDate);
      String startDateFormatted = Helper.getFhirDate(startDate);
      String endDateFormatted = Helper.getFhirDate(getEndOfDay(startDate));

      UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(url)
              // Add query parameter
              .queryParam("reportDefIdentifier", configInfo.getReportTypeId())
              .queryParam("periodStart", startDateFormatted)
              .queryParam("periodEnd", endDateFormatted)
              .queryParam("regenerate", false);
      Map<String, String> uriParams = new HashMap<>();
      URI urlWithParameters = builder.buildAndExpand(uriParams).toUri();

      // create headers
      HttpHeaders headers = new HttpHeaders();
      // set `content-type` header
      headers.setContentType(MediaType.APPLICATION_JSON);

      headers.setBearerAuth(token);
      // set `accept` header
      headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

      // build the request
      HttpEntity<Map<String, Object>> entity = new HttpEntity<>(headers);

      // send POST request
      ResponseEntity<GenerateResponse> response = restTemplate.postForEntity(urlWithParameters, entity, GenerateResponse.class);

      String reportId = response.getBody() != null?response.getBody().getReportId():null;
      if (reportId == null) {
        logger.error("Error generating report. Please contact the system administrator.");
        System.exit(1);
      }

      url = configInfo.getApiUrl() + "/report/{reportId}/$send";
      UriComponentsBuilder builder1 = UriComponentsBuilder.fromUriString(url);

      Map<String, Object> map = new HashMap<>();
      map.put("reportId", reportId);
      URI urlWithParameters1 = builder1.buildAndExpand(map).toUri();

      // send the report
      restTemplate.exchange(urlWithParameters1, HttpMethod.GET, entity, String.class);
      logger.info("Report successfully generated and submitted.");
      System.exit(0);
    } catch (Exception ex) {
      logger.error(String.format("Error generating and submitting report: %s", ex.getMessage()), ex);
      System.exit(1);
    }
  }

  private static Date getStartDate(int adjustment) {
    TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
    Calendar cal = new GregorianCalendar();
    cal.set(Calendar.MILLISECOND, 0);
    cal.set(Calendar.SECOND, 0);
    cal.set(Calendar.MINUTE, 0);
    cal.set(Calendar.HOUR_OF_DAY, 0);
    cal.add(Calendar.HOUR, adjustment);
    return cal.getTime();
  }


  private static Date getEndOfDay(Date date) {
    TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
    Calendar cal = new GregorianCalendar();
    cal.setTime(date);
    cal.set(Calendar.HOUR, 23);
    cal.set(Calendar.MINUTE, 59);
    cal.set(Calendar.SECOND, 59);
    cal.set(Calendar.MILLISECOND, 0);
    return cal.getTime();
  }

}
