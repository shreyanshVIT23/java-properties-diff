package com.shreyansh.cli.platform;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Windows: {@code PATH} entries separated by ';' (optionally quoted), programs found by trying
 * each {@code PATHEXT} extension, case-insensitive variable names, and {@code cmd /c} for user
 * commands.
 */
public final class WindowsPlatform implements Platform {

  /** Used when {@code PATHEXT} is not set. */
  private static final String DEFAULT_PATHEXT = ".COM;.EXE;.BAT;.CMD";

  /** Exit code of cmd when a command is not recognized. */
  private static final int NOT_RECOGNIZED = 9009;

  @Override
  public Optional<String> env(Map<String, String> env, String name) {
    String exact = env.get(name);
    if (exact != null) {
      return Optional.of(exact);
    }
    // Windows treats "Path" and "PATH" as the same variable
    return env.entrySet().stream()
        .filter(entry -> entry.getKey().equalsIgnoreCase(name))
        .map(Map.Entry::getValue)
        .findFirst();
  }

  @Override
  public List<Path> candidates(String name, Map<String, String> env) {
    String pathext = env(env, "PATHEXT").orElse(DEFAULT_PATHEXT);
    List<String> extensions = Arrays.stream(pathext.split(";"))
        .filter(extension -> !extension.isBlank())
        .toList();
    List<Path> files = new ArrayList<>();
    for (String entry : env(env, "PATH").orElse("").split(";")) {
      String directory = entry.strip().replace("\"", "");
      if (directory.isEmpty()) {
        continue;
      }
      for (String extension : extensions) {
        try {
          files.add(Path.of(directory, name + extension));
        } catch (InvalidPathException e) {
          // Not a valid path on this file system: skip it, as cmd would
        }
      }
    }
    return files;
  }

  @Override
  public List<String> shell(String command) {
    return List.of("cmd", "/c", command);
  }

  @Override
  public boolean couldNotRun(int exitCode) {
    return exitCode == NOT_RECOGNIZED;
  }
}
