package com.lantanagroup.link.nhsn;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.utils.URIBuilder;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;

public class ReportingPlanService {
  private final String url;
  private final String nhsnOrgId;
  private final ObjectMapper objectMapper = new ObjectMapper()
          .configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true)
          .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

  public ReportingPlanService(String url, String nhsnOrgId) {
    this.url = url;
    this.nhsnOrgId = nhsnOrgId;
  }

  public boolean isReporting(String name, int year, int month) throws URISyntaxException, IOException {
    URIBuilder builder = new URIBuilder(url);
    builder.addParameter("nhsnorgid", nhsnOrgId);
    builder.addParameter("name", name);
    builder.addParameter("year", Integer.toString(year));
    builder.addParameter("month", Integer.toString(month));
    URL url = builder.build().toURL();
    Response response = objectMapper.reader().readValue(url, Response.class);
    for (Plan plan : response.getPlans()) {
      if (!StringUtils.equals(plan.getNhsnOrgId(), nhsnOrgId)) {
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

  @Getter
  @Setter
  private static class Response {
    private List<Plan> plans = List.of();
  }

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
