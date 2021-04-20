package com.lantanagroup.nandina.query.api;

import com.lantanagroup.nandina.config.QueryConfig;
import com.lantanagroup.nandina.spring.FhirMessageConverter;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.http.converter.HttpMessageConverter;

@SpringBootApplication(scanBasePackages = "com.lantanagroup.nandina.query.auth")
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
}
