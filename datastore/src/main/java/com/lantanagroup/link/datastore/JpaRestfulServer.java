package com.lantanagroup.link.datastore;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.support.IValidationSupport;
import ca.uhn.fhir.jpa.api.config.DaoConfig;
import ca.uhn.fhir.jpa.api.dao.IFhirSystemDao;
import ca.uhn.fhir.jpa.cache.ResourceChangeListenerCacheFactory;
import ca.uhn.fhir.jpa.cache.ResourceChangeListenerRegistryImpl;
import ca.uhn.fhir.jpa.config.HibernatePropertiesProvider;
import ca.uhn.fhir.jpa.model.entity.ModelConfig;
import ca.uhn.fhir.jpa.provider.IJpaSystemProvider;
import ca.uhn.fhir.jpa.provider.JpaCapabilityStatementProvider;
import ca.uhn.fhir.jpa.searchparam.matcher.InMemoryResourceMatcher;
import ca.uhn.fhir.jpa.searchparam.registry.SearchParamRegistryImpl;
import ca.uhn.fhir.rest.server.RestfulServer;
import ca.uhn.fhir.rest.server.interceptor.LoggingInterceptor;
import ca.uhn.fhir.rest.server.provider.ResourceProviderFactory;
import ca.uhn.fhir.rest.server.util.ISearchParamRegistry;
import com.lantanagroup.link.FhirContextProvider;
import com.lantanagroup.link.config.datastore.DataStoreConfig;
import com.lantanagroup.link.datastore.auth.UserInterceptor;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.dialect.internal.StandardDialectResolver;
import org.hibernate.engine.jdbc.dialect.spi.DatabaseMetaDataDialectResolutionInfoAdapter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;

import javax.servlet.ServletException;
import javax.sql.DataSource;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Set;

public class JpaRestfulServer extends RestfulServer {
  @Autowired
  ApplicationContext applicationContext;

  @Autowired
  IFhirSystemDao fhirSystemDao;

  @Autowired
  IJpaSystemProvider jpaSystemProvider;

  @Autowired
  DaoConfig daoConfig;

  @Autowired
  private IValidationSupport validationSupport;

  @Autowired
  private DataSource dataSource;

  @Autowired
  private ModelConfig modelConfig;

  @Autowired
  private DataStoreConfig dataStoreConfig;

  @Autowired
  ResourceProviderFactory resourceProviders;

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
    this.registerProvider(jpaSystemProvider);

    JpaCapabilityStatementProvider confProvider = new JpaCapabilityStatementProvider(this, this.fhirSystemDao, this.daoConfig, this.searchParamRegistry, validationSupport);
    this.setServerConformanceProvider(confProvider);

    daoConfig.setBundleTypesAllowedForStorage(Set.of("document", "message", "transaction", "batch", "searchset", "collection"));
    daoConfig.setAutoCreatePlaceholderReferenceTargets(true);
    daoConfig.setResourceServerIdStrategy(DaoConfig.IdStrategyEnum.UUID);
    daoConfig.setResourceClientIdStrategy(DaoConfig.ClientIdStrategyEnum.ANY);

    LoggingInterceptor loggingInterceptor = new LoggingInterceptor();
    loggingInterceptor.setLoggerName("datastore");
    loggingInterceptor.setMessageFormat(
            "Path[${servletPath}] Source[${requestHeader.x-forwarded-for}] " +
            "Operation[${operationType} ${operationName} ${idOrResourceName}] " +
            "UA[${requestHeader.user-agent}] Params[${requestParameters}] " +
            "ResponseEncoding[${responseEncodingNoDefault}]");
    loggingInterceptor.setErrorMessageFormat("ERROR - ${requestVerb} ${requestUrl}");
    loggingInterceptor.setLogExceptions(true);
    this.registerInterceptor(loggingInterceptor);

    this.registerInterceptor(new UserInterceptor(this.dataStoreConfig));
  }

  @Bean
  public ISearchParamRegistry searchParamRegistry() {
    return this.searchParamRegistry;
  }

  @Primary
  @Bean
  public HibernatePropertiesProvider jpaStarterDialectProvider(LocalContainerEntityManagerFactoryBean myEntityManagerFactory) throws SQLException {
    DatabaseMetaData metaData = this.dataSource.getConnection().getMetaData();
    return new HibernatePropertiesProvider() {
      @Override
      public Dialect getDialect() {
        return new StandardDialectResolver().resolveDialect(new DatabaseMetaDataDialectResolutionInfoAdapter(metaData));
      }
    };
  }
}
