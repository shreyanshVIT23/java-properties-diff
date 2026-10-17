package com.shreyansh;

import com.shreyansh.cli.DiffCommand;
import com.shreyansh.cli.Terminal;

/**
 * Entry point of {@code java -jar java-properties-diff.jar}: runs the {@code propdiff} command.
 */
public class App {

  /**
   * Runs the command and exits with its exit code.
   *
   * @param args command-line arguments, see {@code --help}
   */
  public static void main(String[] args) {
    System.exit(run(args));
  }

  /** Runs the command against the real terminal and returns its exit code. */
  static int run(String[] args) {
    return DiffCommand.commandLine(Terminal.system()).execute(args);
  }
}
