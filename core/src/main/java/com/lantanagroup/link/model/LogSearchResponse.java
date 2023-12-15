package com.lantanagroup.link.model;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class LogSearchResponse {
  private int total;
  private List<LogMessage> messages = new ArrayList<>();
}
