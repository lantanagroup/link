package com.lantanagroup.link.datastore;

import ca.uhn.fhir.jpa.api.config.DaoConfig;
import ca.uhn.fhir.jpa.model.config.PartitionSettings;
import ca.uhn.fhir.jpa.model.entity.ModelConfig;
import com.lantanagroup.link.config.datastore.DataStoreConfig;
import com.lantanagroup.link.config.datastore.DataStoreDaoConfig;
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
        "com.lantanagroup.link.config.datastore",
        "com.lantanagroup.link.datastore",
        "com.lantanagroup.link.spring"
})
@ServletComponentScan(basePackageClasses = {JpaRestfulServer.class})
public class DataStoreApplication extends SpringBootServletInitializer {
  @Autowired
  AutowireCapableBeanFactory beanFactory;

  @Autowired
  private DataStoreConfig dataStoreConfig;

  public static void main(String[] args) {
    SpringApplication.run(DataStoreApplication.class, args);
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
    ModelConfig modelConfig = new ModelConfig();
    modelConfig.setAutoSupportDefaultSearchParams(true);
    modelConfig.setDefaultSearchParamsCanBeOverridden(true);
    return modelConfig;
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

    if (dataStoreConfig.getDao() != null) {
      DataStoreDaoConfig dao = dataStoreConfig.getDao();

      if (dao.getAutoCreatePlaceholderReferenceTargets() != null) { theDaoConfig.setAutoCreatePlaceholderReferenceTargets(dao.getAutoCreatePlaceholderReferenceTargets()); }
      if (dao.getEnforceReferentialIntegrityOnDelete() != null) { theDaoConfig.setEnforceReferentialIntegrityOnDelete(dao.getEnforceReferentialIntegrityOnDelete()); }
      if (dao.getEnforceReferentialIntegrityOnWrite() != null) { theDaoConfig.setEnforceReferentialIntegrityOnWrite(dao.getEnforceReferentialIntegrityOnWrite()); }
      if (dao.getAllowContainsSearches() != null) { theDaoConfig.setAllowContainsSearches(dao.getAllowContainsSearches()); }
      if (dao.getAllowMultipleDelete() != null) { theDaoConfig.setAllowMultipleDelete(dao.getAllowMultipleDelete()); }
      if (dao.getExpungeEnabled() != null) { theDaoConfig.setExpungeEnabled(dao.getExpungeEnabled()); }
      if (dao.getAllowMultipleDelete() != null) { theDaoConfig.setDeleteExpungeEnabled(dao.getAllowMultipleDelete()); }
      if (dao.getFetchSizeDefaultMaximum() != null) { theDaoConfig.setFetchSizeDefaultMaximum(dao.getFetchSizeDefaultMaximum()); }
      if (dao.getExpireSearchResultsAfterMillis() != null) { theDaoConfig.setExpireSearchResultsAfterMillis(dao.getExpireSearchResultsAfterMillis()); }
    }

    return theDaoConfig;
  }

  @Bean(destroyMethod = "close")
  public BasicDataSource dataSource() {
    BasicDataSource retVal = new BasicDataSource();
    if (this.dataStoreConfig != null && this.dataStoreConfig.getDataSource() != null) {
      retVal.setDriverClassName(this.dataStoreConfig.getDataSource().getDriverClassName());
      retVal.setUrl(this.dataStoreConfig.getDataSource().getUrl());
      retVal.setUsername(this.dataStoreConfig.getDataSource().getUsername());
      retVal.setPassword(this.dataStoreConfig.getDataSource().getPassword());
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
