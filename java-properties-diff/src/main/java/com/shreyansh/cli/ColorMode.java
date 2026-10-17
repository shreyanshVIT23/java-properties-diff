package com.shreyansh.cli;

/**
 * Value of the {@code --color} option.
 */
public enum ColorMode {

  /** Color only on an interactive terminal that allows it. */
  AUTO,

  /** Always emit ANSI colors, even when piped. */
  ALWAYS,

  /** Never emit ANSI colors. */
  NEVER
}
