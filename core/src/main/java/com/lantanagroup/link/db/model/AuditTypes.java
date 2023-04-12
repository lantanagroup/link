package com.lantanagroup.link.db.model;

public enum AuditTypes {
  /**
   * Generation of report
   */
  Generate,

  /**
   * Submission of report
   */
  Submit,

  /**
   * Deletion of patient lists as a result of retention check
   */
  PatientListDelete,

  /**
   * Deletion of patient data as a result of retention check
   */
  PatientDataDelete
}
