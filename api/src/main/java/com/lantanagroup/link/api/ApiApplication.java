package com.lantanagroup.link.api;

import ca.uhn.fhir.context.FhirContext;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.lantanagroup.link.FhirContextProvider;
import com.lantanagroup.link.FhirHelper;
import com.lantanagroup.link.db.SharedService;
import com.lantanagroup.link.validation.Validator;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

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
@EnableAsync
public class ApiApplication extends SpringBootServletInitializer implements InitializingBean {
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

  @Bean
  public Validator validator(SharedService sharedService) {
    return new Validator(sharedService);
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
}
