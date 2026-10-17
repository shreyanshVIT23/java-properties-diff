package com.shreyansh.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

/**
 * Tests for {@link Pager}. Real pager processes are started through {@code sh}, with commands
 * that write to a temp file instead of a screen.
 */
@DisabledOnOs(OS.WINDOWS)
public class PagerTest {

  private static final String TEXT = "Δ left ⟶ right\nchanged (1)\n";

  @TempDir
  Path tempDir;

  @Test
  void notInteractivePrintsDirectly() {
    StringWriter out = new StringWriter();

    new Pager(new Terminal(false, Map.of("PAGER", "false")))
        .show(TEXT, new PrintWriter(out), true);

    assertEquals(TEXT, out.toString());
  }

  @Test
  void disabledPagerPrintsDirectly() {
    StringWriter out = new StringWriter();

    new Pager(new Terminal(true, Map.of("PAGER", "false")))
        .show(TEXT, new PrintWriter(out), false);

    assertEquals(TEXT, out.toString());
  }

  @Test
  void pagerCommandReceivesTheText() throws Exception {
    Path paged = tempDir.resolve("paged.txt");
    StringWriter out = new StringWriter();

    new Pager(new Terminal(true, Map.of("PAGER", "cat > '" + paged + "'")))
        .show(TEXT, new PrintWriter(out), true);

    assertEquals(TEXT, Files.readString(paged));
    assertEquals("", out.toString());
  }

  @Test
  void missingPagerCommandFallsBackToDirectOutput() {
    StringWriter out = new StringWriter();

    new Pager(new Terminal(true, Map.of("PAGER", "propdiff-no-such-pager-command")))
        .show(TEXT, new PrintWriter(out), true);

    assertEquals(TEXT, out.toString());
  }

  @Test
  void lessOptionsDefaultToFrxButUserValueWins() throws Exception {
    Path defaulted = tempDir.resolve("default.txt");
    String recordLess = "cat > /dev/null; printf %s \"$LESS\" > ";

    new Pager(new Terminal(true, Map.of("PAGER", recordLess + "'" + defaulted + "'")))
        .show(TEXT, new PrintWriter(new StringWriter()), true);
    assertEquals("FRX", Files.readString(defaulted));

    Path custom = tempDir.resolve("custom.txt");
    new Pager(new Terminal(true, Map.of("PAGER", recordLess + "'" + custom + "'", "LESS", "-S")))
        .show(TEXT, new PrintWriter(new StringWriter()), true);
    assertEquals("-S", Files.readString(custom));
  }
}
