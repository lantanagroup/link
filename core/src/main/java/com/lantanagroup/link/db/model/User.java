package com.lantanagroup.link.db.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.bson.types.ObjectId;

@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class User {
  private String id = (new ObjectId()).toString();
  private String name;
  private String email;
  private String password;
  private Boolean enabled = true;

  public boolean hasEmail() {
    return StringUtils.isNotEmpty(this.email);
  }

  public boolean hasPassword() {
    return StringUtils.isNotEmpty(this.password);
  }
}
