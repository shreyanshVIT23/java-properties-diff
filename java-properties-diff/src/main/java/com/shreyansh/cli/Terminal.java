package com.shreyansh.cli;

import java.util.Map;

/**
 * What the CLI knows about where its output goes. Injected so tests never depend on a real tty.
 *
 * @param interactive whether stdout is an interactive terminal
 * @param env the environment variables to consult ({@code NO_COLOR}, {@code TERM},
 *     {@code COLUMNS})
 */
public record Terminal(boolean interactive, Map<String, String> env) {

  /** Width used when neither {@code --width} nor {@code COLUMNS} gives a usable value. */
  public static final int DEFAULT_WIDTH = 120;

  /** Narrowest width the renderers accept. */
  public static final int MIN_WIDTH = 20;

  /**
   * Stores an unmodifiable copy of the environment.
   *
   * @param interactive whether stdout is an interactive terminal
   * @param env the environment variables to consult
   */
  public Terminal {
    env = Map.copyOf(env);
  }

  /**
   * Describes the real process: on JDK 17, {@link System#console()} is {@code null} when stdin
   * or stdout is redirected.
   *
   * @return the terminal of the running JVM
   */
  public static Terminal system() {
    return new Terminal(System.console() != null, System.getenv());
  }

  /**
   * Decides whether to emit ANSI colors.
   *
   * @param mode the {@code --color} option
   * @return {@code true} if colors should be used
   */
  public boolean useColor(ColorMode mode) {
    return switch (mode) {
      case ALWAYS -> true;
      case NEVER -> false;
      // https://no-color.org: a present, non-empty NO_COLOR disables color
      case AUTO -> interactive
          && env.getOrDefault("NO_COLOR", "").isEmpty()
          && !"dumb".equals(env.get("TERM"));
    };
  }

  /**
   * Picks the output width: the option, then {@code COLUMNS}, then {@link #DEFAULT_WIDTH}, and
   * never less than {@link #MIN_WIDTH}.
   *
   * @param option the {@code --width} value, or {@code null} if not given
   * @return the width to render at
   */
  public int width(Integer option) {
    int width = option != null && option > 0 ? option : columns();
    return Math.max(MIN_WIDTH, width);
  }

  private int columns() {
    try {
      int columns = Integer.parseInt(env.getOrDefault("COLUMNS", ""));
      return columns > 0 ? columns : DEFAULT_WIDTH;
    } catch (NumberFormatException e) {
      return DEFAULT_WIDTH;
    }
  }
}
