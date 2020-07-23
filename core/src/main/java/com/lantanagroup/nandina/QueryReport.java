package com.lantanagroup.nandina;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class QueryReport {
  private String date;
  private List<String> questions = new ArrayList<>();
  private Map<String, Object> answers = new HashMap<>();

  public String getDate() {
    return date;
  }

  public void setDate(String date) {
    this.date = date;
  }

  public List<String> getQuestions() {
    return questions;
  }

  public void setQuestions(List<String> questions) {
    this.questions = questions;
  }

  public Map<String, Object> getAnswers() {
    return answers;
  }

  public void setAnswers(Map<String, Object> answers) {
    this.answers = answers;
  }

  public void setAnswer(String questionId, Object value) {
    this.answers.put(questionId, value);
  }

  public Object getAnswer(String questionId) {
    return this.answers.get(questionId);
  }
}
