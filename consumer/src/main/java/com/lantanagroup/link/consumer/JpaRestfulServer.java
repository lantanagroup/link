package com.lantanagroup.link.consumer;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.support.IValidationSupport;
import ca.uhn.fhir.jpa.api.config.DaoConfig;
import ca.uhn.fhir.jpa.api.dao.IFhirSystemDao;
import ca.uhn.fhir.jpa.cache.ResourceChangeListenerCacheFactory;
import ca.uhn.fhir.jpa.cache.ResourceChangeListenerRegistryImpl;
import ca.uhn.fhir.jpa.config.HibernatePropertiesProvider;
import ca.uhn.fhir.jpa.model.entity.ModelConfig;
import ca.uhn.fhir.jpa.provider.JpaCapabilityStatementProvider;
import ca.uhn.fhir.jpa.searchparam.matcher.InMemoryResourceMatcher;
import ca.uhn.fhir.jpa.searchparam.registry.SearchParamRegistryImpl;
import ca.uhn.fhir.rest.server.RestfulServer;
import ca.uhn.fhir.rest.server.provider.ResourceProviderFactory;
import ca.uhn.fhir.rest.server.util.ISearchParamRegistry;
import com.lantanagroup.link.FhirContextProvider;
import com.lantanagroup.link.config.consumer.ConsumerConfig;
import com.lantanagroup.link.consumer.auth.AuthInterceptor;
import com.lantanagroup.link.consumer.auth.UserInterceptor;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.dialect.internal.StandardDialectResolver;
import org.hibernate.engine.jdbc.dialect.spi.DatabaseMetaDataDialectResolutionInfoAdapter;
import org.hibernate.internal.util.config.ConfigurationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;

import javax.servlet.ServletException;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.ArrayList;

public class JpaRestfulServer extends RestfulServer {
  private static final long serialVersionUID = 1L;

  @Autowired
  ApplicationContext applicationContext;

  @Autowired
  @SuppressWarnings("rawtypes")
  IFhirSystemDao fhirSystemDao;

  @Autowired
  DaoConfig daoConfig;

  @Autowired
  private IValidationSupport validationSupport;

  @Autowired
  private DataSource dataSource;

  @Autowired
  private ModelConfig modelConfig;

  @Autowired
  private ConsumerConfig consumerConfig;

  @Autowired
  ResourceProviderFactory resourceProviders;

  @Autowired
  ReportCsvOperationProvider reportCsvOperationProvider;

  private FhirContext fhirContext;
  private SearchParamRegistryImpl searchParamRegistry = new SearchParamRegistryImpl();
  private ResourceChangeListenerRegistryImpl resourceChangeListenerRegistry = new ResourceChangeListenerRegistryImpl();
  private InMemoryResourceMatcher inMemoryResourceMatcher = new InMemoryResourceMatcher();

  public JpaRestfulServer() {
  }

  @Override
  protected void initialize() throws ServletException {
    super.initialize();

    this.fhirContext = FhirContextProvider.getFhirContext();
    this.setFhirContext(this.fhirContext);

    this.resourceChangeListenerRegistry.setFhirContext(this.fhirContext);
    this.resourceChangeListenerRegistry.setResourceChangeListenerCacheFactory(this.applicationContext.getBean(ResourceChangeListenerCacheFactory.class));
    this.resourceChangeListenerRegistry.setInMemoryResourceMatcher(new InMemoryResourceMatcher());

    this.searchParamRegistry.setFhirContext(this.fhirContext);
    this.searchParamRegistry.setModelConfig(this.modelConfig);
    this.searchParamRegistry.setResourceChangeListenerRegistry(this.resourceChangeListenerRegistry);
    this.searchParamRegistry.registerListener();
    this.searchParamRegistry.handleInit(new ArrayList<>());

    this.registerProviders(this.resourceProviders.createProviders());

    JpaCapabilityStatementProvider confProvider = new JpaCapabilityStatementProvider(this, this.fhirSystemDao, this.daoConfig, this.searchParamRegistry, validationSupport);
    this.setServerConformanceProvider(confProvider);

    this.registerInterceptor(
            new UserInterceptor(
                    consumerConfig.getLinkAuthManager().getIssuer(),
                    consumerConfig.getLinkAuthManager().getAuthJwksUrl()
            )
    );
    this.registerInterceptor(new AuthInterceptor(consumerConfig));

    reportCsvOperationProvider.initialize();
    this.registerProvider(reportCsvOperationProvider);
  }

  @Bean
  public ISearchParamRegistry searchParamRegistry() {
    return this.searchParamRegistry;
  }

  @Primary
  @Bean
  public HibernatePropertiesProvider jpaStarterDialectProvider(LocalContainerEntityManagerFactoryBean myEntityManagerFactory) throws SQLException {
    try(Connection conn = this.dataSource.getConnection()) {
      DatabaseMetaData metaData = conn.getMetaData();
      return new HibernatePropertiesProvider() {
        @Override
        public Dialect getDialect() {
          return new StandardDialectResolver().resolveDialect(new DatabaseMetaDataDialectResolutionInfoAdapter(metaData));
        }
      };
    } catch (SQLException sqlException) {
      throw new ConfigurationException(sqlException.getMessage(), sqlException);
    }
  }
}
