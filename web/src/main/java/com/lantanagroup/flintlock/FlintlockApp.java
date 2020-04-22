package com.lantanagroup.flintlock;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.annotation.PropertySources;

@SpringBootApplication
@PropertySources({@PropertySource(value={"classpath:application.properties"}), @PropertySource(value={"file:/flintlock.properties"}, ignoreResourceNotFound = true)})
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