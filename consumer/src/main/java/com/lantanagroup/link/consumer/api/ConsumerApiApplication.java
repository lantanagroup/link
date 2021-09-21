package com.lantanagroup.link.consumer.api;

import ca.uhn.fhir.jpa.api.config.DaoConfig;
import ca.uhn.fhir.jpa.api.dao.IFhirSystemDao;
import ca.uhn.fhir.jpa.dao.r4.FhirSystemDaoR4;
import ca.uhn.fhir.jpa.model.config.PartitionSettings;
import ca.uhn.fhir.jpa.model.entity.ModelConfig;
import ca.uhn.fhir.jpa.searchparam.registry.SearchParamRegistryImpl;
import ca.uhn.fhir.rest.server.util.ISearchParamRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
@ServletComponentScan(basePackageClasses = {JpaRestfulServer.class})
public class ConsumerApiApplication extends SpringBootServletInitializer {
  @Autowired
  AutowireCapableBeanFactory beanFactory;

  public static void main(String[] args) {
    SpringApplication.run(ConsumerApiApplication.class, args);
  }

  @Bean
  public ServletRegistrationBean fhirServletRegistration() {
    ServletRegistrationBean servletRegistrationBean = new ServletRegistrationBean();
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
    return theDaoConfig;
  }
}
