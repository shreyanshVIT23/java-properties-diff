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
 * {@link Platform}. When {@code less} is not installed, a {@link BuiltinPager} running inside
 * the JVM is used instead.
 */
final class Pager {

  private static final String DEFAULT_PAGER = "less";
  private static final String DEFAULT_LESS = "FRX";

  private final Terminal terminal;
  private final Platform platform;
  private final BuiltinPager builtin;

  Pager(Terminal terminal) {
    this(terminal, Platform.current(), new JlineLess());
  }

  /** Lets tests choose the platform and the built-in pager. */
  Pager(Terminal terminal, Platform platform, BuiltinPager builtin) {
    this.terminal = terminal;
    this.platform = platform;
    this.builtin = builtin;
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

  /**
   * Pages the text, in this order: the user's {@code $PAGER}, the system {@code less}, then the
   * built-in pager. Returns false if nothing could show it, so the caller prints directly.
   */
  private boolean page(String text) {
    Optional<String> custom =
        platform.env(terminal.env(), "PAGER").filter(value -> !value.isBlank());
    if (custom.isPresent()) {
      // A user's PAGER may hold arguments, so the platform's shell parses it. If it cannot
      // run, print directly rather than silently switching to another pager.
      return run(platform.shell(custom.get()), text);
    }
    Optional<Path> less = platform.findOnPath(DEFAULT_PAGER, terminal.env());
    if (less.isPresent()) {
      return run(List.of(less.get().toString()), text);
    }
    try {
      builtin.page(text);
      return true;
    } catch (IOException e) {
      // No usable terminal for the built-in pager either
      return false;
    }
  }

  /** Runs a pager process fed with the text; returns false if the command could not run. */
  private boolean run(List<String> command, String text) {
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
