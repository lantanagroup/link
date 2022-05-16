package com.lantanagroup.link.api;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.lantanagroup.link.FhirDataProvider;
import com.lantanagroup.link.FhirHelper;
import com.lantanagroup.link.config.api.ApiConfig;
import com.lantanagroup.link.config.api.ApiQueryConfigModes;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.web.filter.DelegatingFilterProxy;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import java.util.TimeZone;

/**
 * Main REST API for NHSNLink. Entry point for SpringBoot. Initializes as a SpringBootApplication, which
 * hosts controllers defined within the project.
 */
@OpenAPIDefinition(
        info = @Info(title = "Link API"),
        security = { @SecurityRequirement(name = "oauth") })
@SpringBootApplication(scanBasePackages = {
        "com.lantanagroup.link.api",
        "com.lantanagroup.link.config",
        "com.lantanagroup.link.config.api",
        "com.lantanagroup.link.query",
        "com.lantanagroup.link.auth",
        "com.lantanagroup.link.nhsn"})
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
    // Do some advanced validation on the configuration
    if (this.config.getQuery().getMode() == ApiQueryConfigModes.Remote) {
      if (StringUtils.isEmpty(this.config.getQuery().getUrl())) {
        throw new Exception("When query.mode is \"Remote\", query.url is required");
      }
      if (StringUtils.isEmpty(this.config.getQuery().getApiKey())) {
        throw new Exception("When query.mode is \"Remote\", query.apiKey is required");
      }
    }
  }

  /**
   * Triggered during SpringBoot application's startup. Adds to the security filter chain.
   * @param servletContext
   * @throws ServletException
   */
  @Override
  public void onStartup(ServletContext servletContext) throws ServletException {
    servletContext
            .addFilter("securityFilter", new DelegatingFilterProxy("springSecurityFilterChain"))
            .addMappingForUrlPatterns(null, false, "/*");

    super.onStartup(servletContext);
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
    return new FhirDataProvider(config.getFhirServerStore());
  }

  /**
   * Responds with SimpleModule, which makes SpringBoot aware of FhirJsonSerializer, and uses it to
   * serialize FHIR resources as XML or JSON in API responses.
   * @return
   */
  @Bean
  public Module module() {
    FhirContext fhirContext = FhirContext.forR4();
    SimpleModule module = new SimpleModule();
    FhirHelper.initSerializers(module, fhirContext.newJsonParser());
    return module;
  }
}
