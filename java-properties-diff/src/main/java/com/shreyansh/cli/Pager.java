package com.shreyansh.cli;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;

/**
 * Sends output through a pager the way git does: {@code $PAGER}, or {@code less}, run with
 * {@code LESS=FRX} unless the user set {@code LESS}. FRX keeps colors (R), quits when the output
 * fits on one screen (F) and leaves it on screen afterwards (X).
 */
final class Pager {

  private static final String DEFAULT_PAGER = "less";
  private static final String DEFAULT_LESS = "FRX";

  /** Exit code of sh when the pager command is not executable. */
  private static final int NOT_EXECUTABLE = 126;

  /** Exit code of sh when the pager command is not found. */
  private static final int NOT_FOUND = 127;

  private final Terminal terminal;

  Pager(Terminal terminal) {
    this.terminal = terminal;
  }

  /**
   * Shows the text through the pager when enabled and on an interactive terminal; otherwise,
   * or when the pager cannot run, prints it to {@code out}.
   */
  void show(String text, PrintWriter out, boolean enabled) {
    if (!enabled || !terminal.interactive() || !page(text)) {
      out.print(text);
      out.flush();
    }
  }

  /** Runs the pager; returns false if it could not run, so the caller prints directly. */
  private boolean page(String text) {
    String command = terminal.env().getOrDefault("PAGER", "");
    ProcessBuilder builder =
        new ProcessBuilder("sh", "-c", command.isBlank() ? DEFAULT_PAGER : command)
            .redirectOutput(ProcessBuilder.Redirect.INHERIT)
            .redirectError(ProcessBuilder.Redirect.INHERIT);
    builder.environment().put("LESS", terminal.env().getOrDefault("LESS", DEFAULT_LESS));

    Process process;
    try {
      process = builder.start();
    } catch (IOException e) {
      // No sh (e.g. Windows): print directly
      return false;
    }
    try (Writer writer =
        new OutputStreamWriter(process.getOutputStream(), StandardCharsets.UTF_8)) {
      writer.write(text);
    } catch (IOException e) {
      // Broken pipe: the user quit the pager early, or the command does not exist
    }
    try {
      int exit = process.waitFor();
      return exit != NOT_EXECUTABLE && exit != NOT_FOUND;
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      return true;
    }
  }
}
