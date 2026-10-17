package com.shreyansh.cli;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.jline.terminal.Size;
import org.jline.terminal.Terminal;
import org.jline.terminal.impl.ExternalTerminal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/**
 * Tests for {@link JlineLess}, the Java {@code less} used when no system pager exists. Each test
 * drives a JLine terminal backed by in-memory streams instead of a real TTY; the timeout turns
 * a pager waiting for input that never comes into a failure.
 */
@Timeout(10)
public class JlineLessTest {

  @Test
  void textThatFitsOneScreenIsShownWithoutWaitingForInput() throws Exception {
    ByteArrayOutputStream out = new ByteArrayOutputStream();

    // No input at all: like less -F, it must return as soon as the text is shown
    new JlineLess(() -> terminal("", out)).page("Δ left ⟶ right\nchanged (1)\n");

    String screen = out.toString(StandardCharsets.UTF_8);
    assertTrue(screen.contains("changed (1)"), screen);
  }

  @Test
  void longTextShowsTheFirstScreenAndQuitsOnQ() throws Exception {
    String text = IntStream.rangeClosed(1, 200)
        .mapToObj(i -> "line " + i)
        .collect(Collectors.joining("\n", "", "\n"));
    ByteArrayOutputStream out = new ByteArrayOutputStream();

    new JlineLess(() -> terminal("q", out)).page(text);

    String screen = out.toString(StandardCharsets.UTF_8);
    assertTrue(screen.contains("line 1"), screen);
    assertFalse(screen.contains("line 200"), "only the first screen should be drawn");
  }

  @Test
  void quittingClearsThePromptLineLikeLess() throws Exception {
    String text = IntStream.rangeClosed(1, 200)
        .mapToObj(i -> "line " + i)
        .collect(Collectors.joining("\n", "", "\n"));
    ByteArrayOutputStream out = new ByteArrayOutputStream();

    new JlineLess(() -> terminal("q", out)).page(text);

    // The ":" prompt must not stay behind: carriage return, then clear to end of line (xterm)
    String screen = out.toString(StandardCharsets.UTF_8);
    assertTrue(screen.endsWith("\r\u001b[K"), "screen ends with: " + tail(screen));
  }

  @Test
  void ansiColorsInTheTextAreKept() throws Exception {
    ByteArrayOutputStream out = new ByteArrayOutputStream();

    new JlineLess(() -> terminal("", out)).page(Style.MINUS.paint("- key=old", true) + "\n");

    String screen = out.toString(StandardCharsets.UTF_8);
    assertTrue(screen.contains("- key=old"), screen);
    // The 256-color background 52 of Style.MINUS survives JLine's re-rendering
    assertTrue(screen.contains("48;5;52"), screen);
  }

  @Test
  void noUsableTerminalIsReportedAsIoException() {
    JlineLess pager = new JlineLess(() -> {
      throw new IOException("no tty");
    });

    assertThrows(IOException.class, () -> pager.page("text\n"));
  }

  /** The last characters of the screen, with escape characters made visible for messages. */
  private static String tail(String screen) {
    return screen.substring(Math.max(0, screen.length() - 40)).replace("\u001b", "ESC");
  }

  /**
   * An 80x24 xterm-256color terminal reading the given keys and writing to {@code out}.
   * ExternalTerminal works only on the given streams: TerminalBuilder with system(false) still
   * attached to the real TTY and sent capability queries to it.
   */
  private static Terminal terminal(String keys, ByteArrayOutputStream out) throws IOException {
    Terminal terminal = new ExternalTerminal("test", "xterm-256color",
        new ByteArrayInputStream(keys.getBytes(StandardCharsets.UTF_8)), out,
        StandardCharsets.UTF_8);
    terminal.setSize(Size.of(80, 24));
    return terminal;
  }
}
