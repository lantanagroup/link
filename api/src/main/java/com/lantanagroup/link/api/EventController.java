package com.lantanagroup.link.api;

import com.lantanagroup.link.EventTypes;
import com.lantanagroup.link.IReportGenerationEvent;
import com.lantanagroup.link.config.api.ApiConfigEvents;
import com.lantanagroup.link.model.ReportContext;
import com.lantanagroup.link.model.ReportCriteria;
import lombok.Setter;
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
public class EventController {

  private static final Logger logger = LoggerFactory.getLogger(EventController.class);

  @Autowired
  @Setter
  protected ApplicationContext context;

  @Autowired
  @Setter
  private ApiConfigEvents apiConfigEvents;

  public void triggerEvent(EventTypes eventType, ReportCriteria criteria, ReportContext reportContext) throws Exception {
    Method eventMethodInvoked = ApiConfigEvents.class.getMethod("get" + eventType.toString());
    List<String> classNames = (List<String>) eventMethodInvoked.invoke(apiConfigEvents);
    List<Object> beans = getBeans(eventType, classNames);
    if (beans == null) return;
    for (Object bean : beans) {
      ((IReportGenerationEvent) bean).execute(criteria, reportContext);
    }
  }


  public List<Object> getBeans(EventTypes eventType, List<String> classNames) {
    List<Class<?>> classes = new ArrayList();
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
    ArrayList beans = new ArrayList();
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
