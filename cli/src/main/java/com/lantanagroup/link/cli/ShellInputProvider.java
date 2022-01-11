package com.lantanagroup.link.cli;

import org.springframework.shell.Input;
import org.springframework.shell.InputProvider;

public class ShellInputProvider implements InputProvider {

  private final Input input;
  private boolean commandExecuted = false;

  public ShellInputProvider(String[] args){
    this.input = new ShellInput(args);
  }

  @Override
  public Input readInput() {
    if (!commandExecuted){
      commandExecuted=true;
      return input;
    }
    return new ShellInput(new String[]{"exit"});
  }
}
