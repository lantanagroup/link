package com.lantanagroup.link.db.model;

import lombok.Getter;
import lombok.Setter;
import org.bson.types.ObjectId;

@Getter
@Setter
public class User {
  private String id = (new ObjectId()).toString();
  private String name;
  private String email;
}
