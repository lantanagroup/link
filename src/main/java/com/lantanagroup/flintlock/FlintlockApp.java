package com.lantanagroup.flintlock;

import com.lantanagroup.flintlock.client.MainPageTransformer;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

@SpringBootApplication
public class FlintlockApp extends SpringBootServletInitializer implements InitializingBean {
  @Autowired
  private ApplicationContext context;

  public static void main(String[] args) {
    SpringApplication.run(FlintlockApp.class, args);
  }

  @Override
  public void afterPropertiesSet() throws Exception {
    // Initialize the config using the Spring application context so that @Value annotations resolve/work
    Config config = this.context.getBean(Config.class);
    Config.setInstance(config);
  }
}
