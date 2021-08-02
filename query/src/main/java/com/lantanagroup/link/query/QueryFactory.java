package com.lantanagroup.link.query;

import org.springframework.context.ApplicationContext;

public class QueryFactory {
  public static IQuery getQueryInstance(ApplicationContext context, String queryClass) throws Exception {
    Class<?> queryClazz = Class.forName(queryClass);
    IQuery query = (IQuery) context.getBean(queryClazz);
    query.setApplicationContext(context);

    return query;
  }
}
