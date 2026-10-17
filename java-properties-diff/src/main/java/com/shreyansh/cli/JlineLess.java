package com.shreyansh.cli;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import org.jline.builtins.Less;
import org.jline.builtins.Source;
import org.jline.reader.EndOfFileException;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.utils.InfoCmp.Capability;

/**
 * The built-in pager: JLine's pure-Java {@code less}, so paging works the same on every OS even
 * when no {@code less} is installed. It behaves like {@code less -FRX}: it quits at once when
 * the text fits on one screen, keeps ANSI colors and leaves the text on screen.
 */
final class JlineLess implements BuiltinPager {

  /** Opens the terminal the pager draws on; tests supply one backed by in-memory streams. */
  @FunctionalInterface
  interface TerminalSource {

    /**
     * Opens a terminal.
     *
     * @return the terminal, closed by the pager when it is done
     * @throws IOException if no usable terminal exists
     */
    Terminal open() throws IOException;
  }

  private final TerminalSource terminals;

  /**
   * Uses the real terminal. {@code dumb(false)} makes JLine fail instead of falling back to a
   * dumb terminal (and printing a warning), so the caller can print directly.
   */
  JlineLess() {
    this(() -> TerminalBuilder.builder().system(true).dumb(false).build());
  }

  /** Lets tests supply the terminal. */
  JlineLess(TerminalSource terminals) {
    this.terminals = terminals;
  }

  @Override
  public void page(String text) throws IOException {
    try (Terminal terminal = terminals.open()) {
      Less less = new Less(terminal, Path.of("").toAbsolutePath());
      // The same behaviour as LESS=FRX for the system less
      less.quitIfOneScreen = true;
      less.noInit = true;
      less.run(new Source.InputStreamSource(
          new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8)), true, "propdiff"));
      // Like less -X, remove the ":" prompt line when quitting. Short text was printed without
      // paging, and clearing would erase its last line (the summary), so only clear after paging.
      if (text.lines().count() >= terminal.getRows()) {
        terminal.puts(Capability.carriage_return);
        terminal.puts(Capability.clr_eol);
        terminal.flush();
      }
    } catch (EndOfFileException e) {
      // The keyboard input ended (stdin closed): nothing more to page
    } catch (InterruptedException e) {
      // Ctrl+C while paging: stop paging and keep the interrupt for the caller
      Thread.currentThread().interrupt();
    }
  }
}
