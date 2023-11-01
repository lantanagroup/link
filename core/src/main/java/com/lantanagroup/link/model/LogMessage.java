package com.lantanagroup.link.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class LogMessage {
  private Date timestamp;
  private String severity;
  private String message;
  private String caller;
}
