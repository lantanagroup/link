package com.lantanagroup.link;

import com.lantanagroup.link.db.model.tenant.Events;
import com.lantanagroup.link.model.ReportContext;
import com.lantanagroup.link.model.ReportCriteria;
import com.lantanagroup.link.time.Stopwatch;
import com.lantanagroup.link.time.StopwatchManager;
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

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;


@Component
public class EventService {
  private static final Logger logger = LoggerFactory.getLogger(EventService.class);

  @Autowired
  @Setter
  protected ApplicationContext context;

  @Autowired
  private StopwatchManager stopwatchManager;

  public void triggerEvent(TenantService tenantService, EventTypes eventType, ReportCriteria criteria, ReportContext reportContext, ReportContext.MeasureContext measureContext) throws Exception {
    List<Object> beans = this.getBeans(tenantService, eventType);
    if (beans == null || beans.size() == 0) return;
    for (Object bean : beans) {
      if (bean instanceof IReportGenerationEvent) {
        logger.info("Executing event " + eventType.toString() + " for bean " + bean);
        //noinspection unused
        try (Stopwatch stopwatch = this.stopwatchManager.start(String.format("event-%s", bean.getClass().getSimpleName()))) {
          ((IReportGenerationEvent) bean).execute(criteria, reportContext, measureContext);
        }
      } else {
        logger.warn(bean.toString() + " does not implement the IReportGenerationEvent interface");
      }
    }
  }

  public void triggerEvent(TenantService tenantService, EventTypes eventType, ReportCriteria criteria, ReportContext context) throws Exception {
    triggerEvent(tenantService, eventType, criteria, context, null);
  }

  public void triggerDataEvent(TenantService tenantService, EventTypes eventType, Bundle bundle, ReportCriteria criteria, ReportContext reportContext, ReportContext.MeasureContext measureContext) throws Exception {
    List<Object> beans = this.getBeans(tenantService, eventType);
    if (beans == null || beans.size() == 0) return;
    for (Object bean : beans) {
      if (bean instanceof IReportGenerationDataEvent) {
        logger.info("Executing event " + eventType.toString() + " for bean " + bean);
        //noinspection unused
        try (Stopwatch stopwatch = this.stopwatchManager.start(String.format("event-%s", bean.getClass().getSimpleName()))) {
          ((IReportGenerationDataEvent) bean).execute(tenantService, bundle, criteria, reportContext, measureContext);
        }
      } else {
        logger.warn(bean.toString() + " does not implement the IReportGenerationDataEvent interface");
      }
    }
  }

  public List<Object> getBeans(TenantService tenantService, EventTypes eventType) throws Exception {
    if (tenantService == null || tenantService.getConfig() == null || tenantService.getConfig().getEvents() == null) {
      return new ArrayList<>();
    }

    List<Class<?>> classes = new ArrayList<>();
    Method eventMethodInvoked = Events.class.getMethod("get" + eventType.toString());
    List<String> classNames = (List<String>) eventMethodInvoked.invoke(tenantService.getConfig().getEvents());
    if (classNames != null) {
      for (String className : classNames) {
        try {
          Class<?> aClass = Class.forName(className);
          classes.add(aClass);
        } catch (ClassNotFoundException ex) {
          logger.error(String.format("TriggerEvent %s - class %s cannot be found:", eventType, className));
        }
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
