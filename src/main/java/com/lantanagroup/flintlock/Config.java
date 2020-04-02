package com.lantanagroup.flintlock;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class Config {
  private static Config config;

  public static Config getInstance() {
    if (config == null) {
      config = new Config();
    }
    return config;
  }

  public static void setInstance(Config instance) {
    config = instance;
  }

  @Value("${google.api.key}")
  public String googleApiKey;
}
