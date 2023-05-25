package com.lantanagroup.link.db.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.lantanagroup.link.model.BulkOutput;
import com.lantanagroup.link.model.BulkResponse;
import lombok.Getter;
import lombok.Setter;
import org.bson.types.ObjectId;

@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BulkStatusResult {
  private String id = (new ObjectId()).toString();
  private String statusId;
  private BulkResponse result;
}
