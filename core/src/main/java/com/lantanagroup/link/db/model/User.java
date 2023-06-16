package com.lantanagroup.link.db.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.lantanagroup.link.db.SQLHelper;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.bson.types.ObjectId;

import java.sql.ResultSet;
import java.sql.SQLException;

@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class User {
  private String id = (new ObjectId()).toString();
  private String name;
  private String email;
  private String passwordSalt;
  private String passwordHash;
  private Boolean enabled = true;

  public boolean hasEmail() {
    return StringUtils.isNotEmpty(this.email);
  }

  public boolean hasPassword() {
    return StringUtils.isNotEmpty(this.passwordHash);
  }

  public static User create(ResultSet rs) throws SQLException {
    User user = new User();
    user.setId(SQLHelper.getString(rs, "id"));
    user.setEmail(SQLHelper.getNString(rs, "email"));
    user.setName(SQLHelper.getNString(rs, "name"));
    user.setEnabled(SQLHelper.getBoolean(rs, "enabled"));
    user.setPasswordHash(SQLHelper.getNString(rs, "passwordHash"));
    user.setPasswordSalt(SQLHelper.getNString(rs, "passwordSalt"));
    return user;
  }
}
