package com.lantanagroup.link.cli;

import lombok.AllArgsConstructor;
import org.springframework.shell.Input;

@AllArgsConstructor
public class ShellInput implements Input {

  private final String[] args;

  @Override
  public String rawText() {
    String raw = "";
    for (String arg : args) {
      if(raw.equals("")) raw = arg;
      else raw = raw + ' ' + arg;
    }
    return raw;
  }

}
