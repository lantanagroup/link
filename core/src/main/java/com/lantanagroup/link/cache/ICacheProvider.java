package com.lantanagroup.link.cache;

import java.util.List;

public interface ICacheProvider {
  void init();

  /** PUTTERS **/
  void put(String prefix, String key, String value);

  default void put(String key, String value) {
    this.put(null, key, value);
  }

  void putObject(String prefix, String key, Object obj);

  default void putObject(String key, Object obj) {
    this.putObject(null, key, obj);
  }

  /** GETTERS **/
  String get(String prefix, String key);

  default String get(String key) {
    return this.get(null, key);
  }

  <T> T getObject(String prefix, String key, Class<T> type);

  default <T> T getObject(String key, Class<T> type) {
    return this.getObject(null, key, type);
  }

  /** COLLECTION **/
  List<String> getKeys(String prefix);

  /** DELETE **/
  void deleteKeysWithPrefix(String prefix);
  void deleteKeys(String... keys);
}
