package com.lantanagroup.link.cli;

import com.lantanagroup.link.FhirDataProvider;
import com.lantanagroup.link.config.api.ApiDataStoreConfig;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.boot.web.servlet.context.AnnotationConfigServletWebServerApplicationContext;
import org.springframework.context.ApplicationContext;

import javax.validation.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class BaseShellCommand {
  @Autowired
  protected ApplicationContext applicationContext;

  protected List<Class<?>> getBeanClasses() {
    return new ArrayList<>();
  }

  private GenericBeanDefinition getBeanDef(Class<?> beanClass) {
    GenericBeanDefinition gbd = new GenericBeanDefinition();
    gbd.setBeanClass(beanClass);
    return gbd;
  }

  protected void registerBeans() {
    DefaultListableBeanFactory beanFactory = (DefaultListableBeanFactory) ((AnnotationConfigServletWebServerApplicationContext) this.applicationContext).getBeanFactory();

    for (Class<?> beanClass : this.getBeanClasses()) {
      String beanName = beanClass.getName().substring(0, 1).toLowerCase() + beanClass.getName().substring(1);

      if (!beanFactory.containsBeanDefinition(beanName)) {
        beanFactory.registerBeanDefinition(beanName, this.getBeanDef(beanClass));
      }
    }
  }

  protected void registerFhirDataProvider() {
    DefaultListableBeanFactory beanFactory = (DefaultListableBeanFactory) ((AnnotationConfigServletWebServerApplicationContext) this.applicationContext).getBeanFactory();
    QueryCliConfig config = this.applicationContext.getBean(QueryCliConfig.class);
    FhirDataProvider fhirDataprovider;
    try {
      fhirDataprovider = this.applicationContext.getBean(FhirDataProvider.class);
    } catch (NoSuchBeanDefinitionException ex) {
      ApiDataStoreConfig dataStoreConfig = config.getDataStore();
      fhirDataprovider = new FhirDataProvider(dataStoreConfig);
      beanFactory.registerSingleton(String.valueOf(FhirDataProvider.class), fhirDataprovider);
    }
  }

  protected <T> void validate(T object) {
    Validator validator;
    try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
      validator = factory.getValidator();
    }
    Set<ConstraintViolation<T>> violations = validator.validate(object);
    if (!violations.isEmpty()) {
      throw new ConstraintViolationException(violations);
    }
  }
}
