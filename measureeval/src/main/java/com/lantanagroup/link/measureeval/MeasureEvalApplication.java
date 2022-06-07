package com.lantanagroup.link.measureeval;

import ca.uhn.fhir.context.FhirContext;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.lantanagroup.link.FhirContextProvider;
import com.lantanagroup.link.FhirHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class MeasureEvalApplication extends SpringBootServletInitializer {
  @Autowired
  private FhirContext fhirContext;

  public static void main(String[] args) {
    SpringApplication.run(MeasureEvalApplication.class, args);
  }

  @Bean
  public FhirContext ctx() {
    return FhirContextProvider.getFhirContext();
  }

  /**
   * Responds with SimpleModule, which makes SpringBoot aware of FhirJsonSerializer, and uses it to
   * serialize FHIR resources as XML or JSON in API responses.
   * @return
   */
  @Bean
  public Module module() {
    SimpleModule module = new SimpleModule();
    FhirHelper.initSerializers(module, this.fhirContext.newJsonParser());
    return module;
  }

}
