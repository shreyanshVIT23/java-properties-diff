package com.shreyansh.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;

/**
 * End-to-end tests of the {@code propdiff} command: real fixture files, real argument parsing,
 * output captured through picocli's {@link CommandLine#setOut} and {@link CommandLine#setErr}.
 */
public class DiffCommandTest {

  private static final String LEFT =
      Path.of("src", "test", "resources", "fixtures", "left.properties").toString();
  private static final String RIGHT =
      Path.of("src", "test", "resources", "fixtures", "right.properties").toString();

  /** Not interactive and no environment: no color, no pager, default width. */
  private static final Terminal PIPE = new Terminal(false, Map.of());

  @Test
  void differencesExitWithOneAndPrintUnifiedDiff() {
    Result result = run(LEFT, RIGHT, "--width", "40");

    assertEquals(1, result.exit());
    assertTrue(result.out().startsWith("Δ " + LEFT + " ⟶ " + RIGHT + "\n"));
    // CUSTOM mode is the default, so line numbers come from the files
    assertTrue(result.out().contains("\n   3 ⋮     │ - app.version=1.0\n"));
    assertTrue(result.out().contains("\n     ⋮   4 │ + app.version=1.1\n"));
    assertTrue(result.out().endsWith(
        "\n2 changed · 1 only in left · 2 only in right · 4 unchanged\n"));
    assertFalse(result.out().contains("\u001b"));
    assertEquals("", result.err());
  }

  @Test
  void identicalFilesExitWithZero() {
    Result result = run(LEFT, LEFT);

    assertEquals(0, result.exit());
    assertTrue(result.out().endsWith("\nno differences\n"));
    assertEquals(0, run(LEFT, LEFT, "--no-pager").exit());
  }

  @Test
  void unreadableFileExitsWithTwoAndReportsOnStderr() {
    Result result = run(LEFT, "missing.properties");

    assertEquals(2, result.exit());
    assertEquals("", result.out());
    assertTrue(result.err().contains("missing.properties"));
  }

  @Test
  void usageErrorsExitWithTwo() {
    assertEquals(2, run().exit());
    assertEquals(2, run(LEFT).exit());
    assertEquals(2, run(LEFT, RIGHT, "--bogus").exit());
    assertEquals(2, run(LEFT, RIGHT, "--mode", "xml").exit());
    assertEquals(2, run(LEFT, RIGHT, "--only", "nope").exit());

    Result badColor = run(LEFT, RIGHT, "--color", "rainbow");
    assertEquals(2, badColor.exit());
    assertTrue(badColor.err().contains("rainbow"));
  }

  @Test
  void onlyTakesKebabCaseListAndModeIsCaseInsensitive() {
    Result result =
        run(LEFT, RIGHT, "--only", "changed,only-in-left", "--mode", "DEFAULT", "--width", "40");

    assertEquals(1, result.exit());
    assertTrue(result.out().contains("\nchanged (2)\n"));
    assertTrue(result.out().contains("\nonly in left (1)\n"));
    assertFalse(result.out().contains("only in right (2)"));
    // DEFAULT mode (java.util.Properties) has no line numbers
    assertTrue(result.out().contains("\n     ⋮     │ - app.version=1.0\n"));
  }

  @Test
  void sideBySideWithUnchangedAndForcedColor() {
    Result result =
        run(LEFT, RIGHT, "-s", "--show-unchanged", "--color", "always", "--width", "60");

    assertEquals(1, result.exit());
    assertTrue(result.out().contains(Style.HEADER.paint("unchanged (4)", true)));
    assertTrue(result.out().contains(Style.DIM.paint("│   2 │ ", true) + "app.name=demo"));
  }

  @Test
  void helpAndVersionExitWithZero() {
    Result help = run("--help");

    assertEquals(0, help.exit());
    assertTrue(help.out().contains("Usage: propdiff"));
    assertTrue(help.out().contains("--mode=MODE"));
    assertTrue(help.out().contains("--no-pager"));
    assertFalse(help.out().contains("Default: []"));
    assertEquals(0, run("--version").exit());
  }

  private static Result run(String... args) {
    StringWriter out = new StringWriter();
    StringWriter err = new StringWriter();
    CommandLine commandLine = DiffCommand.commandLine(PIPE);
    commandLine.setOut(new PrintWriter(out, true));
    commandLine.setErr(new PrintWriter(err, true));
    int exit = commandLine.execute(args);
    return new Result(exit, out.toString(), err.toString());
  }

  /** Exit code and captured output of one run. */
  private record Result(int exit, String out, String err) {
  }
}
