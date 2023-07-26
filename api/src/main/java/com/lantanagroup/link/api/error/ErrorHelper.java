package com.lantanagroup.link.api.error;

import org.json.JSONObject;

public class ErrorHelper {

  public static String generateJSONErrorMessage(String message, String code){
    JSONObject errorMessage = new JSONObject();
    errorMessage.put("code", code);
    errorMessage.put("message", message);

    return errorMessage.toString();
  }
}
