package com.lantanagroup.link.cli;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {
        "com.lantanagroup.link.cli"
})
public class ShellApplication {

  public static void main(String[] args) {
    System.getProperties().put("server.port", 8090);
    SpringApplication.run(ShellApplication.class, args);
  }
}
