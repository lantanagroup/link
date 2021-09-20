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
import org.springframework.core.env.CompositePropertySource;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;

import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

@Configuration
public class FhirServerConfigR4 extends BaseJavaConfigR4 {

  @Autowired
  private DataSource myDataSource;

  @Autowired
  private ConfigurableEnvironment configurableEnvironment;

  @Override
  public DatabaseBackedPagingProvider databaseBackedPagingProvider() {
    DatabaseBackedPagingProvider pagingProvider = super.databaseBackedPagingProvider();
    pagingProvider.setDefaultPageSize(50);
    pagingProvider.setMaximumPageSize(100);
    return pagingProvider;
  }

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

    Map<String, Object> jpaProps = getPropertiesStartingWith(this.configurableEnvironment, "spring.jpa.properties");
    for (Map.Entry<String, Object> entry : jpaProps.entrySet()) {
      String strippedKey = entry.getKey().replace("spring.jpa.properties.", "");
      properties.put(strippedKey, entry.getValue().toString());
    }

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

  private static Map<String, Object> getPropertiesStartingWith(ConfigurableEnvironment aEnv, String aKeyPrefix) {
    Map<String, Object> result = new HashMap<>();
    Map<String, Object> map = getAllProperties(aEnv);

    for (Map.Entry<String, Object> entry : map.entrySet()) {
      String key = entry.getKey();

      if (key.startsWith(aKeyPrefix)) {
        result.put(key, entry.getValue());
      }
    }

    return result;
  }

  private static Map<String, Object> getAllProperties(ConfigurableEnvironment aEnv) {
    Map<String, Object> result = new HashMap<>();
    aEnv.getPropertySources().forEach(ps -> addAll(result, getAllProperties(ps)));
    return result;
  }

  private static Map<String, Object> getAllProperties(PropertySource<?> aPropSource) {
    Map<String, Object> result = new HashMap<>();

    if (aPropSource instanceof CompositePropertySource) {
      CompositePropertySource cps = (CompositePropertySource) aPropSource;
      cps.getPropertySources().forEach(ps -> addAll(result, getAllProperties(ps)));
      return result;
    }

    if (aPropSource instanceof EnumerablePropertySource<?>) {
      EnumerablePropertySource<?> ps = (EnumerablePropertySource<?>) aPropSource;
      Arrays.asList(ps.getPropertyNames()).forEach(key -> result.put(key, ps.getProperty(key)));
      return result;
    }

    return result;
  }

  private static void addAll(Map<String, Object> aBase, Map<String, Object> aToBeAdded) {
    for (Map.Entry<String, Object> entry : aToBeAdded.entrySet()) {
      if (aBase.containsKey(entry.getKey())) {
        continue;
      }

      aBase.put(entry.getKey(), entry.getValue());
    }
  }
}
