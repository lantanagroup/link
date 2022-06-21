package com.lantanagroup.link.agent;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.lantanagroup.link.FhirContextProvider;
import com.lantanagroup.link.FhirHelper;
import com.lantanagroup.link.config.query.QueryConfig;
import com.lantanagroup.link.serialize.FhirJsonDeserializer;
import com.lantanagroup.link.serialize.FhirJsonSerializer;
import com.lantanagroup.link.spring.FhirMessageConverter;
import org.hl7.fhir.r4.model.Resource;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.http.converter.HttpMessageConverter;

@SpringBootApplication(scanBasePackages = {
        "com.lantanagroup.link.agent",
        "com.lantanagroup.link.auth",
        "com.lantanagroup.link.config.query",
        "com.lantanagroup.link.query",
        "com.lantanagroup.link.spring"
})
public class AgentApplication extends SpringBootServletInitializer implements InitializingBean {
  @Autowired
  private QueryConfig config;

  public static void main(String[] args) {
    SpringApplication.run(AgentApplication.class, args);
  }

  @Override
  public void afterPropertiesSet() throws Exception {
    //System.out.println("test");
  }

  @Bean
  public HttpMessageConverter createFhirMessageConverter() {
    return new FhirMessageConverter();
  }

  /**
   * Responds with SimpleModule, which makes SpringBoot aware of FhirJsonSerializer, and uses it to
   * serialize FHIR resources as XML or JSON in API responses.
   * @return
   */
  @Bean
  public Module module() {
    FhirContext fhirContext = FhirContextProvider.getFhirContext();
    IParser jsonParser = fhirContext.newJsonParser();
    SimpleModule module = new SimpleModule();
    FhirHelper.initSerializers(module, jsonParser);
    return module;
  }
}
