package com.lantanagroup.link.consumer;

import ca.uhn.fhir.jpa.api.config.DaoConfig;
import ca.uhn.fhir.jpa.model.config.PartitionSettings;
import ca.uhn.fhir.jpa.model.entity.ModelConfig;
import com.lantanagroup.link.config.consumer.ConsumerConfig;
import org.apache.commons.dbcp2.BasicDataSource;
import org.h2.Driver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.annotation.Bean;

@SpringBootApplication(scanBasePackages = {
        "com.lantanagroup.link.config.consumer",
        "com.lantanagroup.link.consumer",
        "com.lantanagroup.link.spring"
})
@ServletComponentScan(basePackageClasses = {JpaRestfulServer.class})
public class ConsumerApplication extends SpringBootServletInitializer {
  @Autowired
  AutowireCapableBeanFactory beanFactory;

  @Autowired
  private ConsumerConfig consumerConfig;

  public static void main(String[] args) {
    SpringApplication.run(ConsumerApplication.class, args);
  }

  @Bean
  public ServletRegistrationBean<JpaRestfulServer> fhirServletRegistration() {
    ServletRegistrationBean<JpaRestfulServer> servletRegistrationBean = new ServletRegistrationBean<>();
    JpaRestfulServer jpaRestfulServer = new JpaRestfulServer();
    beanFactory.autowireBean(jpaRestfulServer);
    servletRegistrationBean.setServlet(jpaRestfulServer);
    servletRegistrationBean.addUrlMappings("/fhir/*");
    servletRegistrationBean.setLoadOnStartup(1);

    return servletRegistrationBean;
  }

  @Bean
  public ModelConfig modelConfig() {
    return new ModelConfig();
  }

  @Bean
  public PartitionSettings partitionSettings() {
    PartitionSettings partitionSettings = new PartitionSettings();
    partitionSettings.setPartitioningEnabled(false);
    return partitionSettings;
  }

  @Bean
  public DaoConfig daoConfig() {
    DaoConfig theDaoConfig = new DaoConfig();
    theDaoConfig.setAutoCreatePlaceholderReferenceTargets(true);
    theDaoConfig.setEnforceReferentialIntegrityOnDelete(false);
    theDaoConfig.setEnforceReferentialIntegrityOnWrite(true);
    theDaoConfig.setAllowContainsSearches(true);
    theDaoConfig.setAllowMultipleDelete(false);
    theDaoConfig.setExpungeEnabled(true);
    theDaoConfig.setFetchSizeDefaultMaximum(100);
    theDaoConfig.setExpireSearchResultsAfterMillis(3600000);    // 60 minutes
    theDaoConfig.setResourceServerIdStrategy(DaoConfig.IdStrategyEnum.UUID);
    theDaoConfig.setResourceClientIdStrategy(DaoConfig.ClientIdStrategyEnum.ANY);
    theDaoConfig.setExpungeEnabled(true);
    return theDaoConfig;
  }

  @Bean(destroyMethod = "close")
  public BasicDataSource dataSource() {
    BasicDataSource retVal = new BasicDataSource();
    if (this.consumerConfig != null && this.consumerConfig.getDataSource() != null) {
      retVal.setDriverClassName(this.consumerConfig.getDataSource().getDriverClassName());
      retVal.setUrl(this.consumerConfig.getDataSource().getUrl());
      retVal.setUsername(this.consumerConfig.getDataSource().getUsername());
      retVal.setPassword(this.consumerConfig.getDataSource().getPassword());
    } else {
      Driver driver = new Driver();
      retVal.setDriver(driver);
      retVal.setUrl("jdbc:h2:file:./target/database/h2");
      retVal.setUsername("sa");
      retVal.setPassword(null);
    }
    return retVal;
  }
}
