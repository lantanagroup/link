package com.lantanagroup.link.consumer.api;

import ca.uhn.fhir.context.ConfigurationException;
import ca.uhn.fhir.jpa.config.BaseJavaConfigR4;
import ca.uhn.fhir.jpa.config.HibernatePropertiesProvider;
import ca.uhn.fhir.jpa.search.DatabaseBackedPagingProvider;
import ca.uhn.fhir.jpa.search.HapiLuceneAnalysisConfigurer;
import org.apache.lucene.util.Version;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.search.backend.lucene.cfg.LuceneBackendSettings;
import org.hibernate.search.backend.lucene.cfg.LuceneIndexSettings;
import org.hibernate.search.backend.lucene.lowlevel.directory.impl.LocalFileSystemDirectoryProvider;
import org.hibernate.search.engine.cfg.BackendSettings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.*;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;

import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;
import java.util.Properties;

@Configuration
public class FhirServerConfigR4 extends BaseJavaConfigR4 {

  @Autowired
  private DataSource myDataSource;

  @Override
  public DatabaseBackedPagingProvider databaseBackedPagingProvider() {
    DatabaseBackedPagingProvider pagingProvider = super.databaseBackedPagingProvider();
    pagingProvider.setDefaultPageSize(50);
    pagingProvider.setMaximumPageSize(100);
    return pagingProvider;
  }

  @Autowired
  private ConfigurableEnvironment configurableEnvironment;

  @Override
  @Bean()
  public LocalContainerEntityManagerFactoryBean entityManagerFactory() {
    LocalContainerEntityManagerFactoryBean retVal = super.entityManagerFactory();
    retVal.setPersistenceUnitName("HAPI_PU");

    try {
      retVal.setDataSource(myDataSource);
    } catch (Exception e) {
      throw new ConfigurationException("Could not set the data source due to a configuration issue", e);
    }

    Properties properties = new Properties();
    properties.putIfAbsent(BackendSettings.backendKey(BackendSettings.TYPE), LuceneBackendSettings.TYPE_NAME);
    properties.putIfAbsent(BackendSettings.backendKey(LuceneIndexSettings.DIRECTORY_TYPE), LocalFileSystemDirectoryProvider.NAME);
    properties.putIfAbsent(BackendSettings.backendKey(LuceneIndexSettings.DIRECTORY_ROOT), "target/lucenefiles");
    properties.putIfAbsent(BackendSettings.backendKey(LuceneBackendSettings.ANALYSIS_CONFIGURER), HapiLuceneAnalysisConfigurer.class.getName());
    properties.putIfAbsent(BackendSettings.backendKey(LuceneBackendSettings.LUCENE_VERSION), Version.LATEST);
    properties.putIfAbsent(AvailableSettings.HBM2DDL_AUTO, "update");

    retVal.setJpaProperties(properties);
    return retVal;
  }

  @Bean
  @Primary
  public JpaTransactionManager hapiTransactionManager(EntityManagerFactory entityManagerFactory) {
    JpaTransactionManager retVal = new JpaTransactionManager();
    retVal.setEntityManagerFactory(entityManagerFactory);
    return retVal;
  }

  @Primary
  @Bean
  public HibernatePropertiesProvider jpaStarterDialectProvider(LocalContainerEntityManagerFactoryBean myEntityManagerFactory) {
    return new JpaHibernatePropertiesProvider(myEntityManagerFactory);
  }
}
