package com.lantanagroup.link.api;

import ca.uhn.fhir.context.FhirContext;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.lantanagroup.link.FhirContextProvider;
import com.lantanagroup.link.FhirHelper;
import com.lantanagroup.link.TenantService;
import com.lantanagroup.link.config.api.ApiConfig;
import com.lantanagroup.link.config.nhsn.ReportingPlanConfig;
import com.lantanagroup.link.db.MongoService;
import com.lantanagroup.link.db.model.TenantConfig;
import com.lantanagroup.link.nhsn.ReportingPlanService;
import com.mongodb.client.MongoDatabase;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.HandlerMapping;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;
import java.util.TimeZone;

/**
 * Spring Boot application entry point for Link. Initializes as a SpringBootApplication, which
 * hosts controllers, and services defined within the project. ApiInit class is automatically detected and
 * invoked when the spring boot application starts up.
 */
@SpringBootApplication(scanBasePackages = {
        "com.lantanagroup.link",
        "com.lantanagroup.link.api",
        "com.lantanagroup.link.auth",
        "com.lantanagroup.link.config",
        "com.lantanagroup.link.config.api",
        "com.lantanagroup.link.nhsn",
        "com.lantanagroup.link.query",
        "com.lantanagroup.link.spring"
})
@EnableScheduling
@EnableAutoConfiguration(exclude = {MongoAutoConfiguration.class})
@EnableAsync
public class ApiApplication extends SpringBootServletInitializer implements InitializingBean {
  @Autowired
  private ApplicationContext context;

  @Autowired
  private ApiConfig config;

  @Autowired
  private ReportingPlanConfig reportingPlanConfig;

  @Autowired
  private MongoService mongoService;

  /**
   * Main entry point for SpringBoot application. Runs as a SpringBoot application.
   *
   * @param args
   */
  public static void main(String[] args) {
    SpringApplication.run(ApiApplication.class, args);
  }

  /**
   * Triggered after SpringBoot has set the properties/config for the application.
   * Performs additional validation of the configuration that are not supported by SpringBoot
   * config validation annotations.
   * @throws Exception
   */
  @Override
  public void afterPropertiesSet() throws Exception {

  }

  /**
   * Bean injection for the ApiInit class, which causes the ApiInit class to be executed during SpringBoot startup.
   * @return
   */
  @Bean(initMethod = "init")
  public ApiInit apiInit() {
    TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
    return new ApiInit();
  }

  /**
   * Responds with SimpleModule, which makes SpringBoot aware of FhirJsonSerializer, and uses it to
   * serialize FHIR resources as XML or JSON in API responses.
   * @return
   */
  @Bean
  public Module module() {
    FhirContext fhirContext = FhirContextProvider.getFhirContext();
    SimpleModule module = new SimpleModule();
    FhirHelper.initSerializers(module, fhirContext.newJsonParser());
    return module;
  }

  @Bean
  public ReportingPlanService reportingPlanService() {
    if (!reportingPlanConfig.isEnabled()) {
      return null;
    }
    logger.info("Initializing MRP service");
    return new ReportingPlanService(reportingPlanConfig.getUrl(), reportingPlanConfig.getNhsnOrgId());
  }

  /**
   * TenantService bean constructs a new instance for each REST request. Detects the id of the tenant based on a
   * PathVariable ("{tenantId}") being present in the URL path. If path doesn't have tenantId, no TenantService can
   * be constructed. If a tenantId exists in the path but isn't found in the database, exception is thrown.
   *
   * @param request
   * @param mongoService
   * @return
   */
  @Bean
  @Scope(value = "request", proxyMode = ScopedProxyMode.TARGET_CLASS)
  public TenantService tenantService(HttpServletRequest request, MongoService mongoService) {
    Map map = (Map) request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
    String tenantId = (String) map.get("tenantId");

    if (StringUtils.isEmpty(tenantId)) {
      return null;
    }

    TenantConfig tenantConfig = this.mongoService.getTenantConfig(tenantId);

    if (tenantConfig == null) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Tenant not found");
    }

    MongoDatabase database = this.mongoService.getClient().getDatabase(tenantConfig.getDatabase());
    return new TenantService(database, tenantConfig);
  }

  /**
   * This bean is critical for TenantService to be able to auto-wire in sub-threads of controller requests
   *
   * @return
   */
  @Bean
  public DispatcherServlet dispatcherServlet() {
    DispatcherServlet servlet = new DispatcherServlet();
    servlet.setThreadContextInheritable(true);
    return servlet;
  }
}
