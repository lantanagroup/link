package com.lantanagroup.link.api;

import ca.uhn.fhir.context.FhirContext;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.lantanagroup.link.FhirContextProvider;
import com.lantanagroup.link.FhirDataProvider;
import com.lantanagroup.link.FhirHelper;
import com.lantanagroup.link.config.api.ApiConfig;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.TimeZone;

/**
 * Main REST API for NHSNLink. Entry point for SpringBoot. Initializes as a SpringBootApplication, which
 * hosts controllers defined within the project.
 */
@SpringBootApplication(scanBasePackages = {
        "com.lantanagroup.link",
        "com.lantanagroup.link.api",
        "com.lantanagroup.link.auth",
        "com.lantanagroup.link.config",
        "com.lantanagroup.link.config.api",
        "com.lantanagroup.link.mhl",
        "com.lantanagroup.link.nhsn",
        "com.lantanagroup.link.query",
        "com.lantanagroup.link.spring",
        "com.lantanagroup.link.thsa"
})
public class ApiApplication extends SpringBootServletInitializer implements InitializingBean {
  @Autowired
  private ApplicationContext context;

  @Autowired
  private ApiConfig config;

  /**
   * Main entry point for SpringBoot application. Runs as a SpringBoot application.
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
   * Sets the CORS configuration based on the api.yml (or its override)
   * @return WebMvcConfigurer
   */
  @Bean
  public WebMvcConfigurer corsConfigurer() {
    String allowsOrigins = this.config.getCors().getAllowedOrigins();
    String[] allowedMethods = this.config.getCors().getAllowedMethods();
    String allowedHeaders = this.config.getCors().getAllowedHeaders();
    Boolean allowCredentials = this.config.getCors().getAllowedCredentials();

    return new WebMvcConfigurer() {
      @Override
      public void addCorsMappings(CorsRegistry registry) {
        registry
                .addMapping("/**")
                .allowedOrigins(allowsOrigins)
                .allowedMethods(allowedMethods)
                .allowedHeaders(allowedHeaders)
                .allowCredentials(allowCredentials);
      }
    };
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

  @Bean()
  public FhirDataProvider getProvider() {
    return new FhirDataProvider(config.getDataStore());
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
