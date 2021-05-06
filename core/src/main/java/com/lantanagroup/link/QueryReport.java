package com.lantanagroup.link;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
@Setter
public class QueryReport {
  private String date;
  private String measureId;
  private List<String> questions = new ArrayList<>();
  private Map<String, Object> answers = new HashMap<>();

  public void setAnswer(String questionId, Object value) {
    this.answers.put(questionId, value);
  }

  public Object getAnswer(String questionId) {
    return this.answers.get(questionId);
  }
}
