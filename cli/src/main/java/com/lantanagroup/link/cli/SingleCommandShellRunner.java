package com.lantanagroup.link.cli;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.shell.Shell;
import org.springframework.shell.jline.InteractiveShellApplicationRunner;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Order(0)
public class SingleCommandShellRunner implements ApplicationRunner {
  private final ConfigurableEnvironment environment;
  private final Shell shell;
  private final List<String> commands = List.of(
          "query",
          "generate-and-submit",
          "knox-measure-report-transfer",
          "refresh-patient-list",
          "parkland-inventory-import",
          "manual-bed-inventory");

  public SingleCommandShellRunner(Shell shell, ConfigurableEnvironment environment) {
    this.shell = shell;
    this.environment = environment;
  }

  @Override
  public void run(ApplicationArguments args) throws Exception {
    Optional<String> command = args.getNonOptionArgs().stream().filter(a -> commands.indexOf(a) >= 0).findFirst();

    if (!command.isPresent()) {
      return;
    }

    List<String> commandAndArgs = new ArrayList<>();
    boolean afterCommand = false;

    for (String sourceArg : args.getSourceArgs()) {
      if (afterCommand) {
        commandAndArgs.add(sourceArg);
      } else if (sourceArg.equals(command.get())) {
        afterCommand = true;
        commandAndArgs.add(command.get());
      }
    }


    InteractiveShellApplicationRunner.disable(this.environment);
    String[] commands = new String[commandAndArgs.size()];
    shell.run(new ShellInputProvider(commandAndArgs.toArray(commands)));
    shell.run(new ShellInputProvider(new String[]{"exit"}));
  }
}
