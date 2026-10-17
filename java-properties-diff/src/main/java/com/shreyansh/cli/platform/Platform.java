package com.shreyansh.cli.platform;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The OS-specific rules the CLI depends on: reading environment variables, finding a program
 * on {@code PATH} and running a user-supplied pager command. Each OS has its own
 * implementation in this package, so the rest of the CLI has no OS checks.
 */
public interface Platform {

  /**
   * The platform of the running JVM.
   *
   * @return the implementation for this OS
   */
  static Platform current() {
    return System.getProperty("os.name", "").startsWith("Windows")
        ? new WindowsPlatform()
        : new UnixPlatform();
  }

  /**
   * Reads an environment variable using this OS's rules for variable names.
   *
   * @param env the environment to read
   * @param name the variable name, e.g. "PATH"
   * @return the value, if set
   */
  Optional<String> env(Map<String, String> env, String name);

  /**
   * Every file that could be the given command, in the order the shell would try them.
   *
   * @param name the command name without extension, e.g. "less"
   * @param env the environment providing {@code PATH}
   * @return the candidate files; they may not exist
   */
  List<Path> candidates(String name, Map<String, String> env);

  /**
   * The command line that runs a user's command string through this OS's shell, so it may
   * contain arguments.
   *
   * @param command the command string, e.g. the value of {@code $PAGER}
   * @return the program and arguments to start
   */
  List<String> shell(String command);

  /**
   * Tells whether the shell's exit code means the command could not run at all.
   *
   * @param exitCode the exit code of the process started with {@link #shell(String)}
   * @return {@code true} for "not found" or "not executable"
   */
  boolean couldNotRun(int exitCode);

  /**
   * Finds a command on {@code PATH}: the first candidate that is a regular, executable file.
   *
   * @param name the command name without extension, e.g. "less"
   * @param env the environment providing {@code PATH}
   * @return the program to run, if found
   */
  default Optional<Path> findOnPath(String name, Map<String, String> env) {
    return candidates(name, env).stream()
        .filter(file -> Files.isRegularFile(file) && Files.isExecutable(file))
        .findFirst();
  }
}
