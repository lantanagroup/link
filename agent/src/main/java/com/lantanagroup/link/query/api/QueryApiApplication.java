package com.lantanagroup.link.query.api;

import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.lantanagroup.link.config.query.QueryConfig;
import com.lantanagroup.link.serialize.FhirJsonSerializer;
import com.lantanagroup.link.spring.FhirMessageConverter;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.http.converter.HttpMessageConverter;

@SpringBootApplication(scanBasePackages = {
        "com.lantanagroup.link.query",
        "com.lantanagroup.link.config.query",
        "com.lantanagroup.link.auth"
})
public class QueryApiApplication extends SpringBootServletInitializer implements InitializingBean {
  @Autowired
  private QueryConfig config;

  public static void main(String[] args) {
    SpringApplication.run(QueryApiApplication.class, args);
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
    SimpleModule module = new SimpleModule();
    module.addSerializer(new FhirJsonSerializer());
    return module;
  }
}
