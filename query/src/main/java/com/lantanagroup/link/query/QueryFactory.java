package com.lantanagroup.link.query;

import com.lantanagroup.link.config.QueryConfig;
import org.springframework.context.ApplicationContext;

import java.lang.reflect.Constructor;

public class QueryFactory {
  public static IQuery getQueryInstance(ApplicationContext context, QueryConfig config) throws Exception {
    Class queryClass = Class.forName(config.getQueryClass());
    Constructor<?> constructor = queryClass.getConstructor();

    IQuery query = (IQuery) constructor.newInstance();
    query.setApplicationContext(context);
    query.setConfig(config);

    return query;
  }
}
