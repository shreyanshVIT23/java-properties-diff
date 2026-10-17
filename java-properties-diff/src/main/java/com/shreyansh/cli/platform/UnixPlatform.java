package com.shreyansh.cli.platform;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Linux, macOS and other Unix-like systems: {@code PATH} entries separated by ':',
 * case-sensitive variable names, and {@code sh -c} for user commands.
 */
public final class UnixPlatform implements Platform {

  /** Exit codes of sh for "not executable" (126) and "not found" (127). */
  private static final Set<Integer> COULD_NOT_RUN = Set.of(126, 127);

  @Override
  public Optional<String> env(Map<String, String> env, String name) {
    return Optional.ofNullable(env.get(name));
  }

  @Override
  public List<Path> candidates(String name, Map<String, String> env) {
    List<Path> files = new ArrayList<>();
    for (String directory : env(env, "PATH").orElse("").split(":")) {
      // An empty entry means the current directory to sh; it is skipped on purpose
      if (directory.isEmpty()) {
        continue;
      }
      try {
        files.add(Path.of(directory, name));
      } catch (InvalidPathException e) {
        // Not a valid path on this file system: skip it, as the shell would
      }
    }
    return files;
  }

  @Override
  public List<String> shell(String command) {
    return List.of("sh", "-c", command);
  }

  @Override
  public boolean couldNotRun(int exitCode) {
    return COULD_NOT_RUN.contains(exitCode);
  }
}
