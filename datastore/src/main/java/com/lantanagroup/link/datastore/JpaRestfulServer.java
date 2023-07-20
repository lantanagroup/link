package com.lantanagroup.link.datastore;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.support.IValidationSupport;
import ca.uhn.fhir.jpa.api.config.DaoConfig;
import ca.uhn.fhir.jpa.api.dao.IFhirSystemDao;
import ca.uhn.fhir.jpa.cache.ResourceChangeListenerCacheFactory;
import ca.uhn.fhir.jpa.cache.ResourceChangeListenerRegistryImpl;
import ca.uhn.fhir.jpa.config.HibernatePropertiesProvider;
import ca.uhn.fhir.jpa.provider.IJpaSystemProvider;
import ca.uhn.fhir.jpa.provider.JpaCapabilityStatementProvider;
import ca.uhn.fhir.jpa.searchparam.matcher.InMemoryResourceMatcher;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.server.HardcodedServerAddressStrategy;
import ca.uhn.fhir.rest.server.RestfulServer;
import ca.uhn.fhir.rest.server.interceptor.LoggingInterceptor;
import ca.uhn.fhir.rest.server.provider.ResourceProviderFactory;
import ca.uhn.fhir.rest.server.util.ISearchParamRegistry;
import com.lantanagroup.link.FhirContextProvider;
import com.lantanagroup.link.config.datastore.DataStoreConfig;
import com.lantanagroup.link.datastore.auth.UserInterceptor;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.dialect.internal.StandardDialectResolver;
import org.hibernate.engine.jdbc.dialect.spi.DatabaseMetaDataDialectResolutionInfoAdapter;
import org.hibernate.internal.util.config.ConfigurationException;
import org.hl7.fhir.r4.model.Bundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.Resource;

import javax.servlet.ServletException;
import javax.sql.DataSource;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Serial;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.Set;
import java.util.stream.Collectors;

public class JpaRestfulServer extends RestfulServer {
  @Serial
  private static final long serialVersionUID = 1L;

  @Autowired
  ApplicationContext applicationContext;

  @Autowired
  ISearchParamRegistry searchParamRegistry;

  @Autowired
  @SuppressWarnings("rawtypes")
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
  private DataStoreConfig dataStoreConfig;

  @Autowired
  ResourceProviderFactory resourceProviders;

  @Value("classpath:fhir/*")
  private Resource[] fhirResources;

  private FhirContext fhirContext;
  private final ResourceChangeListenerRegistryImpl resourceChangeListenerRegistry = new ResourceChangeListenerRegistryImpl();

  private static final Logger logger = LoggerFactory.getLogger(JpaRestfulServer.class);

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

    this.registerProviders(this.resourceProviders.createProviders());
    this.registerProvider(jpaSystemProvider);

    if (StringUtils.isNotEmpty(this.dataStoreConfig.getPublicAddress())) {
      this.setServerAddressStrategy(new HardcodedServerAddressStrategy(this.dataStoreConfig.getPublicAddress()));
    }

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

    /*
    Bundle transactionBundle = new Bundle();
    transactionBundle.setType(Bundle.BundleType.TRANSACTION);
    Patient patient = new Patient();
    patient.addName().setFamily("Smith").addGiven("John");
    transactionBundle.addEntry().setResource(patient).getRequest().setMethod(Bundle.HTTPVerb.POST);

    Object resultBundle = fhirSystemDao.transaction(null, transactionBundle);

     */

    LoadFhirResources();

    this.registerInterceptor(new UserInterceptor(this.dataStoreConfig));
  }

  @Primary
  @Bean
  public HibernatePropertiesProvider jpaStarterDialectProvider() {
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

  private  org.hl7.fhir.r4.model.Resource readFileAsFhirResource(IParser parser, InputStream file) {
    String resourceString = new BufferedReader(new InputStreamReader(file)).lines().parallel().collect(Collectors.joining("\n"));
    return (org.hl7.fhir.r4.model.Resource)parser.parseResource(resourceString);
  }

  private void LoadFhirResources() {

    logger.info("Loading {} FHIR Resources.", fhirResources.length);

    try {
      IParser parser = fhirContext.newJsonParser();
      for (final Resource fhirResource : fhirResources) {
        try (InputStream inputStream = fhirResource.getInputStream()) {

          org.hl7.fhir.r4.model.Resource resource = readFileAsFhirResource(parser, inputStream);

          Bundle transactionBundle = new Bundle();
          transactionBundle.setType(Bundle.BundleType.TRANSACTION);
          transactionBundle
                  .addEntry()
                  .setResource(resource)
                  .getRequest()
                  .setMethod(Bundle.HTTPVerb.PUT)
                  .setUrl(resource.getResourceType().name() + "/" + resource.getIdElement().getIdPart());

          logger.info("Resource Type '{}' with ID '{}' read from '{}'",
                  resource.getResourceType().name(),
                  resource.getIdElement().getIdPart(),
                  fhirResource.getFilename());

          Bundle resultBundle = (Bundle)fhirSystemDao.transaction(null, transactionBundle);

          logger.info("Resource Type '{}' with ID '{}' PUT with status {}",
                  resource.getResourceType().name(),
                  resource.getIdElement().getIdPart(),
                  resultBundle.getEntry().get(0).getResponse().getStatus());
        }
      }
    } catch (Exception ex) {
      logger.error(ex.getMessage());
    }
  }
}
