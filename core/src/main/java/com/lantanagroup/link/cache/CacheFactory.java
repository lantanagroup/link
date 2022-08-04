package com.lantanagroup.link.cache;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

public class CacheFactory {
  private static final Logger logger = LoggerFactory.getLogger(CacheFactory.class);

  public static ICacheProvider instance;

  public static void init(ApplicationContext context) {
    CacheConfig cacheConfig = (CacheConfig) context.getBean(CacheConfig.class);

    if (cacheConfig == null || StringUtils.isEmpty(cacheConfig.getProvider())) {
      return;
    }

    try {
      Class clazz = Class.forName(cacheConfig.getProvider());
      CacheFactory.instance = (ICacheProvider) context.getBean(clazz);
      CacheFactory.instance.init();
    } catch (ClassNotFoundException e) {
      logger.error("Configured cache provider cannot be found: " + e.getMessage(), e);
    }
  }
}
