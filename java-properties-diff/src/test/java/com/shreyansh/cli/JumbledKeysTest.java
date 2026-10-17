package com.shreyansh.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.shreyansh.core.diff.DiffEntry;
import com.shreyansh.core.diff.DiffResult;
import com.shreyansh.core.diff.DiffType;
import com.shreyansh.core.diff.PropertiesDiff;
import com.shreyansh.core.parser.ParserMode;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import picocli.CommandLine;

/**
 * Two files with the same keys and values in a different order (so at different line numbers)
 * must compare as equal: a key's position is not part of the diff, only its value.
 *
 * <p>Lines in the fixtures: alpha 2/6, beta 3/8, gamma 4/7, delta 5/4, epsilon 6/2 (left/right).
 */
public class JumbledKeysTest {

  private static final Path LEFT =
      Path.of("src", "test", "resources", "fixtures", "jumbled-left.properties");
  private static final Path RIGHT =
      Path.of("src", "test", "resources", "fixtures", "jumbled-right.properties");

  /** Not interactive: no color, no pager. */
  private static final Terminal PIPE = new Terminal(false, Map.of());

  @TempDir
  Path tempDir;

  @ParameterizedTest
  @EnumSource(ParserMode.class)
  void sameKeysInDifferentOrderHaveNoDifferencesInEitherMode(ParserMode mode) throws Exception {
    DiffResult result = new PropertiesDiff().compare(LEFT, RIGHT, mode);

    assertFalse(result.hasDifferences());
    assertEquals(5, result.byType(DiffType.UNCHANGED).size());
    assertTrue(result.entries().stream().allMatch(e -> e.leftValue().equals(e.rightValue())));
  }

  @Test
  void commandExitsWithZeroAndSaysNoDifferences() {
    StringWriter out = new StringWriter();

    assertEquals(0, run(out, LEFT.toString(), RIGHT.toString()));
    assertTrue(out.toString().endsWith("\nno differences\n"));
    assertFalse(out.toString().contains("changed ("));
  }

  @Test
  void showUnchangedListsBothLineNumbersInLeftFileOrder() {
    StringWriter out = new StringWriter();

    assertEquals(0, run(out, "--show-unchanged", LEFT.toString(), RIGHT.toString()));

    // Ordered by the left file's lines (2..6), even though the right lines are jumbled
    String expected = String.join("\n",
        "unchanged (5)",
        "   2 ⋮   6 │   alpha=1",
        "   3 ⋮   8 │   beta=2",
        "   4 ⋮   7 │   gamma=3",
        "   5 ⋮   4 │   delta=4",
        "   6 ⋮   2 │   epsilon=5",
        "");
    assertTrue(out.toString().contains("\n" + expected));
  }

  @Test
  void sideBySideShowsEachSidesOwnLineNumber() {
    StringWriter out = new StringWriter();

    // Width 43 gives 13-column cells; "delta=4" is 7 wide, so 6 spaces of padding + 1 separator
    assertEquals(0, run(out, "-s", "--show-unchanged", "--width", "43",
        LEFT.toString(), RIGHT.toString()));

    String delta = "│   5 │ delta=4" + " ".repeat(7) + "│   4 │ delta=4";
    String epsilon = "│   6 │ epsilon=5" + " ".repeat(5) + "│   2 │ epsilon=5";
    assertTrue(out.toString().contains("\n" + delta + "\n"));
    assertTrue(out.toString().contains("\n" + epsilon + "\n"));
  }

  @Test
  void jumbledFileWithOneRealChangeReportsOnlyThatChange() throws Exception {
    Path changed = Files.writeString(tempDir.resolve("changed.properties"),
        Files.readString(RIGHT).replace("gamma 3", "gamma 33"));
    StringWriter out = new StringWriter();

    assertEquals(1, run(out, LEFT.toString(), changed.toString()));

    assertTrue(out.toString().contains(String.join("\n",
        "changed (1)",
        "   4 ⋮     │ - gamma=3",
        "     ⋮   7 │ + gamma=33",
        "")));
    assertTrue(out.toString().endsWith("\n1 changed · 4 unchanged\n"));
    assertEquals(
        new DiffEntry("gamma", "3", "33", DiffType.CHANGED),
        new PropertiesDiff().compare(LEFT, changed, ParserMode.CUSTOM)
            .byType(DiffType.CHANGED).get(0));
  }

  private static int run(StringWriter out, String... args) {
    CommandLine commandLine = DiffCommand.commandLine(PIPE);
    commandLine.setOut(new PrintWriter(out, true));
    commandLine.setErr(new PrintWriter(new StringWriter(), true));
    return commandLine.execute(args);
  }
}
