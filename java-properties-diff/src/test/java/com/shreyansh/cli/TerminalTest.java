package com.shreyansh.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Unit tests for {@link Terminal}: when to use color and how wide the output is.
 */
public class TerminalTest {

  private static final Map<String, String> XTERM = Map.of("TERM", "xterm-256color");

  @Test
  void alwaysAndNeverIgnoreTheTerminal() {
    assertTrue(new Terminal(false, Map.of()).useColor(ColorMode.ALWAYS));
    assertFalse(new Terminal(true, XTERM).useColor(ColorMode.NEVER));
  }

  @Test
  void autoUsesColorOnlyOnAnInteractiveTerminal() {
    assertTrue(new Terminal(true, XTERM).useColor(ColorMode.AUTO));
    assertFalse(new Terminal(false, XTERM).useColor(ColorMode.AUTO));
  }

  @Test
  void autoRespectsNoColorAndDumbTerminals() {
    // https://no-color.org: only a non-empty NO_COLOR disables color
    assertFalse(new Terminal(true, Map.of("NO_COLOR", "1")).useColor(ColorMode.AUTO));
    assertTrue(new Terminal(true, Map.of("NO_COLOR", "")).useColor(ColorMode.AUTO));
    assertFalse(new Terminal(true, Map.of("TERM", "dumb")).useColor(ColorMode.AUTO));
    // An explicit --color always still wins over NO_COLOR
    assertTrue(new Terminal(true, Map.of("NO_COLOR", "1")).useColor(ColorMode.ALWAYS));
  }

  @Test
  void widthPrefersOptionThenColumnsThenDefault() {
    Terminal withColumns = new Terminal(true, Map.of("COLUMNS", "100"));

    assertEquals(80, withColumns.width(80));
    assertEquals(100, withColumns.width(null));
    assertEquals(Terminal.DEFAULT_WIDTH, new Terminal(true, Map.of()).width(null));
  }

  @ParameterizedTest
  @ValueSource(strings = {"abc", "0", "-5", "", " 90"})
  void invalidColumnsFallsBackToDefault(String columns) {
    assertEquals(
        Terminal.DEFAULT_WIDTH, new Terminal(true, Map.of("COLUMNS", columns)).width(null));
  }

  @Test
  void tooNarrowWidthIsRaisedToMinimum() {
    assertEquals(Terminal.MIN_WIDTH, new Terminal(true, Map.of()).width(5));
    assertEquals(Terminal.MIN_WIDTH, new Terminal(true, Map.of("COLUMNS", "10")).width(null));
  }
}
