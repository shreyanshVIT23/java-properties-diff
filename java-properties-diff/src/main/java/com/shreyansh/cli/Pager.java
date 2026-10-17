package com.shreyansh.cli;

import com.shreyansh.cli.platform.Platform;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

/**
 * Sends output through a pager the way git does: {@code $PAGER}, or {@code less}, run with
 * {@code LESS=FRX} unless the user set {@code LESS}. FRX keeps colors (R), quits when the output
 * fits on one screen (F) and leaves it on screen afterwards (X).
 *
 * <p>OS differences (PATH format, the shell for {@code $PAGER}, exit codes) come from a
 * {@link Platform}. When {@code less} is not installed, the output is printed directly.
 */
final class Pager {

  private static final String DEFAULT_PAGER = "less";
  private static final String DEFAULT_LESS = "FRX";

  private final Terminal terminal;
  private final Platform platform;

  Pager(Terminal terminal) {
    this(terminal, Platform.current());
  }

  /** Lets tests choose the platform. */
  Pager(Terminal terminal, Platform platform) {
    this.terminal = terminal;
    this.platform = platform;
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
    Optional<String> custom =
        platform.env(terminal.env(), "PAGER").filter(value -> !value.isBlank());
    List<String> command;
    if (custom.isPresent()) {
      // A user's PAGER may hold arguments, so the platform's shell parses it
      command = platform.shell(custom.get());
    } else {
      Optional<Path> less = platform.findOnPath(DEFAULT_PAGER, terminal.env());
      if (less.isEmpty()) {
        // less is not installed: print directly instead of showing "command not found"
        return false;
      }
      command = List.of(less.get().toString());
    }
    ProcessBuilder builder = new ProcessBuilder(command)
        .redirectOutput(ProcessBuilder.Redirect.INHERIT)
        .redirectError(ProcessBuilder.Redirect.INHERIT);
    builder.environment()
        .put("LESS", platform.env(terminal.env(), "LESS").orElse(DEFAULT_LESS));

    Process process;
    try {
      process = builder.start();
    } catch (IOException e) {
      // No shell to run a custom PAGER: print directly
      return false;
    }
    try (Writer writer =
        new OutputStreamWriter(process.getOutputStream(), StandardCharsets.UTF_8)) {
      writer.write(text);
    } catch (IOException e) {
      // Broken pipe: the user quit the pager early, or the command does not exist
    }
    try {
      return !platform.couldNotRun(process.waitFor());
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      return true;
    }
  }
}
