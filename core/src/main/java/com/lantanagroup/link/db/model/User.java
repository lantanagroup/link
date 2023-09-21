package com.lantanagroup.link.db.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class User {
  private UUID id;
  private String name;
  private String email;
  private byte[] passwordSalt;
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
    user.setId(rs.getObject("id", UUID.class));
    user.setEmail(rs.getString("email"));
    user.setName(rs.getString("name"));
    user.setEnabled(rs.getBoolean("enabled"));
    user.setPasswordHash(rs.getString("passwordHash"));
    user.setPasswordSalt(rs.getBytes("passwordSalt"));
    return user;
  }
}
