package com.lantanagroup.link;

import com.lantanagroup.link.config.api.ApiConfigEvents;
import com.lantanagroup.link.model.ReportContext;
import com.lantanagroup.link.model.ReportCriteria;
import lombok.Setter;
import org.hl7.fhir.r4.model.Bundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;


@Component
@Service
public class EventService {

  private static final Logger logger = LoggerFactory.getLogger(EventService.class);

  @Autowired
  @Setter
  protected ApplicationContext context;

  @Autowired
  @Setter
  private ApiConfigEvents apiConfigEvents;

  public void triggerEvent(EventTypes eventType, ReportCriteria criteria, ReportContext reportContext, ReportContext.MeasureContext measureContext) throws Exception {
    List<Object> beans = getBeans(eventType);
    if (beans == null || beans.size() == 0) return;
    for (Object bean : beans) {
      if (bean instanceof IReportGenerationEvent) {
        ((IReportGenerationEvent) bean).execute(criteria, reportContext, measureContext);
      }
    }
  }

  public void triggerEvent(EventTypes eventType, ReportCriteria criteria, ReportContext context) throws Exception {
    triggerEvent(eventType, criteria, context, null);
  }

  public void triggerDataEvent(EventTypes eventType, Bundle bundle, ReportCriteria criteria, ReportContext reportContext, ReportContext.MeasureContext measureContext) throws Exception {
    List<Object> beans = getBeans(eventType);
    if (beans == null || beans.size() == 0) return;
    for (Object bean : beans) {
      if (bean instanceof IReportGenerationDataEvent) {
        ((IReportGenerationDataEvent) bean).execute(bundle, criteria, reportContext, measureContext);
      }
    }
  }

  public List<Object> getBeans(EventTypes eventType) throws Exception{
    List<Class<?>> classes = new ArrayList<>();
    Method eventMethodInvoked = ApiConfigEvents.class.getMethod("get" + eventType.toString());
    List<String> classNames = (List<String>) eventMethodInvoked.invoke(apiConfigEvents);
    if (classNames == null) {
      logger.debug(String.format("No class set-up for event %s", eventType.toString()));
      return null;
    }
    for (String className : classNames) {
      try {
        Class<?> aClass = Class.forName(className);
        classes.add(aClass);
      } catch (ClassNotFoundException ex) {
        logger.error(String.format("TriggerEvent %s - class %s cannot be found:", eventType.toString(), className));
      }
    }
    List<Object> beans = getBeans(classes);
    return beans;
  }


  public List<Object> getBeans(List<Class<?>> classes) {
    ArrayList<Object> beans = new ArrayList<>();
    DefaultListableBeanFactory beanFactory = (DefaultListableBeanFactory) ((ConfigurableApplicationContext) this.context).getBeanFactory();

    for (Class<?> beanClass : classes) {
      try {
        beans.add(this.context.getBean(beanClass));
      } catch (NoSuchBeanDefinitionException ex) {
        logger.info(String.format("Bean %s not found: in the context", beanClass));
        GenericBeanDefinition gbd = new GenericBeanDefinition();
        gbd.setBeanClass(beanClass);
        if (!beanFactory.containsBeanDefinition(beanClass.getName())) {
          beanFactory.registerBeanDefinition(beanClass.getName(), gbd);
        }
        beans.add(this.context.getBean(beanClass.getName()));
      }
    }
    return beans;
  }
}
