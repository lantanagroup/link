package com.lantanagroup.link.model;

import com.lantanagroup.link.db.model.SubmissionStatus;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class SubmissionStatusResponse {
  private List<SubmissionStatus> inProgressSubmissions = new ArrayList<>();
  private List<SubmissionStatus> completedSubmissions = new ArrayList<>();
  private List<SubmissionStatus> failedSubmissions = new ArrayList<>();
}
