package com.lantanagroup.link.cli;

import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.shell.Shell;

@SpringBootApplication
public class ShellApplication {

  public static void main(String[] args) {
    System.getProperties().put("server.port", 8090);
    SpringApplication.run(ShellApplication.class, args).close();
    System.exit(0);
  }

  @Bean
  public ApplicationRunner shellRunner(Shell shell, ConfigurableEnvironment environment) {
    return new SingleCommandShellRunner(shell, environment);
  }
}
