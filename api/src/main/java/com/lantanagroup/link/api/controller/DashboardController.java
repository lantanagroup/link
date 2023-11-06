package com.lantanagroup.link.api.controller;

import com.lantanagroup.link.db.model.SubmissionStatus;
import com.lantanagroup.link.db.model.SubmissionStatusTypes;
import com.lantanagroup.link.model.SubmissionStatusResponse;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@RestController
@RequestMapping("api/dashboard")
public class DashboardController extends BaseController {
  @GetMapping("recentSubmissions")
  public SubmissionStatusResponse getRecentSubmissionStatuses() {
    //TODO: Daniel - Add count parameter and default to 5?

    SubmissionStatusResponse response = new SubmissionStatusResponse();

    SubmissionStatus inProgressStatus = new SubmissionStatus();
    inProgressStatus.setTenantId("Tenant 1");
    inProgressStatus.setStatus(SubmissionStatusTypes.InProgress);
    inProgressStatus.setBundleId("1");
    inProgressStatus.setStartDate(new Date());

    List<SubmissionStatus> inProgressSubmissionStatuses = new ArrayList<>();
    inProgressSubmissionStatuses.add(inProgressStatus);

    SubmissionStatus completedStatus = new SubmissionStatus();
    completedStatus.setTenantId("Tenant 2");
    completedStatus.setStatus(SubmissionStatusTypes.Success);
    completedStatus.setBundleId("2");
    completedStatus.setStartDate(new Date());
    completedStatus.setEndDate(new Date());

    List<SubmissionStatus> completedSubmissionStatuses = new ArrayList<>();
    completedSubmissionStatuses.add(completedStatus);

    SubmissionStatus failedStatus = new SubmissionStatus();
    failedStatus.setTenantId("Tenant 3");
    failedStatus.setStatus(SubmissionStatusTypes.Failed);
    failedStatus.setBundleId("3");
    failedStatus.setStartDate(new Date());
    failedStatus.setEndDate(new Date());

    List<SubmissionStatus> failedSubmissionStatuses = new ArrayList<>();
    failedSubmissionStatuses.add(failedStatus);

    response.setInProgressSubmissions(inProgressSubmissionStatuses);
    response.setCompletedSubmissions(completedSubmissionStatuses);
    response.setFailedSubmissions(failedSubmissionStatuses);

    return response;
  }
}
