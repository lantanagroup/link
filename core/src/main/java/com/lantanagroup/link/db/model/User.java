package com.lantanagroup.link.db.model;

import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.bson.types.ObjectId;

@Getter
@Setter
public class User {
  private String id = (new ObjectId()).toString();
  private String name;
  private String email;
  private String password;

  public boolean hasEmail() {
    return StringUtils.isNotEmpty(this.email);
  }

  public boolean hasPassword() {
    return StringUtils.isNotEmpty(this.password);
  }
}
