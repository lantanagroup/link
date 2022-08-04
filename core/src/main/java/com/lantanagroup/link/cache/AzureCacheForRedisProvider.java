package com.lantanagroup.link.cache;

import com.google.gson.Gson;
import com.lantanagroup.link.FhirContextProvider;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r4.model.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisClientConfig;

import java.util.List;

@Component
public class AzureCacheForRedisProvider implements ICacheProvider {
  private Jedis jedis;
  private Gson gson = new Gson();

  @Autowired
  private AzureCacheForRedisConfig config;

  @Override
  public void init() {
    if (StringUtils.isEmpty(this.config.getKey()) || StringUtils.isEmpty(this.config.getHostname())) {
      return;
    }

    JedisClientConfig jedisClientConfig = DefaultJedisClientConfig.builder()
            .password(this.config.getKey())
            .ssl(this.config.getUseSsl())
            .build();

    this.jedis = new Jedis(this.config.getHostname(), this.config.getPort(), jedisClientConfig);
  }

  private String getKey(String prefix, String key) {
    if (StringUtils.isNotEmpty(prefix)) {
      return prefix + "-" + key;
    }
    return key;
  }

  private String serialize(Object obj) {
    if (obj instanceof Resource) {
      return FhirContextProvider.getFhirContext().newJsonParser().encodeResourceToString((Resource) obj);
    } else {
      return this.gson.toJson(obj);
    }
  }

  private <T> T deserialize(String json, Class<T> type) {
    if (type.isAssignableFrom(Resource.class)) {
      return (T) FhirContextProvider.getFhirContext().newJsonParser().parseResource(json);
    } else {
      return this.gson.fromJson(json, type);
    }
  }

  public void put(String prefix, String key, String value) {
    if (value == null) {
      this.jedis.del(key);
    } else {
      this.jedis.set(this.getKey(prefix, key), value);
    }
  }

  @Override
  public void putObject(String prefix, String key, Object obj) {
    try {
      if (obj != null) {
        String json = this.serialize(obj);
        this.put(this.getKey(prefix, key), json);
      } else {
        this.put(this.getKey(prefix, key), null);
      }
    } catch (Exception ex) {
      // Ignore errors serializing to JSON
    }
  }

  public String get(String prefix, String key) {
    return this.jedis.get(this.getKey(prefix, key));
  }

  @Override
  public <T> T getObject(String prefix, String key, Class<T> type) {
    String json = this.get(this.getKey(prefix, key));

    if (StringUtils.isNotEmpty(json)) {
      try {
        return this.deserialize(json, type);
      } catch (Exception e) {
        // Ignore errors deserializing from JSON
        return null;
      }
    }

    return null;
  }

  @Override
  public List<String> getKeys(String prefix) {
    return List.copyOf(this.jedis.keys(prefix + "*"));
  }

  @Override
  public void deleteKeysWithPrefix(String prefix) {
    List<String> keys = this.getKeys(prefix);
    this.deleteKeys(keys.toArray(new String[keys.size()]));
  }

  @Override
  public void deleteKeys(String... keys) {
    this.jedis.del(keys);
  }
}
