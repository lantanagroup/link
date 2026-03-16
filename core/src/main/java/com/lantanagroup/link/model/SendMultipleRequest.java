package com.lantanagroup.link.model;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class SendMultipleRequest {
  private List<String> reportIds = new ArrayList<>();
}
