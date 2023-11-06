package com.lantanagroup.link.api.controller;

import com.lantanagroup.link.db.model.SubmissionStatus;
import com.lantanagroup.link.db.model.SubmissionStatusTypes;
import com.lantanagroup.link.model.SubmissionStatusResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@RestController
@RequestMapping("api/submissionStatus")
public class SubmissionStatusController extends BaseController {
  @GetMapping
  public SubmissionStatusResponse getSubmissionStatuses(
          @RequestParam(required = false) SubmissionStatusTypes status,
          @RequestParam(required = false) Integer limit,
          @RequestParam(required = false) Integer start
  )
  {
    SubmissionStatusResponse response = new SubmissionStatusResponse();
    List<SubmissionStatus> submissionStatuses = new ArrayList<>();

    //TODO - Daniel: When querying, we can pass the status as part of the search
    if (status == SubmissionStatusTypes.InProgress || status == null) {
      SubmissionStatus inProgressStatus = new SubmissionStatus();
      inProgressStatus.setTenantId("Tenant 1");
      inProgressStatus.setStatus(SubmissionStatusTypes.InProgress);
      inProgressStatus.setReportId("1");
      inProgressStatus.setStartDate(new Date());
      submissionStatuses.add(inProgressStatus);
    }

    if (status == SubmissionStatusTypes.Success || status == null) {
      SubmissionStatus completedStatus = new SubmissionStatus();
      completedStatus.setTenantId("Tenant 2");
      completedStatus.setStatus(SubmissionStatusTypes.Success);
      completedStatus.setReportId("2");
      completedStatus.setStartDate(new Date());
      completedStatus.setEndDate(new Date());
      submissionStatuses.add(completedStatus);
    }

    if (status == SubmissionStatusTypes.Failed || status == null) {
      SubmissionStatus failedStatus = new SubmissionStatus();
      failedStatus.setTenantId("Tenant 3");
      failedStatus.setStatus(SubmissionStatusTypes.Failed);
      failedStatus.setReportId("3");
      failedStatus.setStartDate(new Date());
      failedStatus.setEndDate(new Date());
      submissionStatuses.add(failedStatus);
    }

    response.setSubmissionStatuses(submissionStatuses);
    return response;
  }
}
