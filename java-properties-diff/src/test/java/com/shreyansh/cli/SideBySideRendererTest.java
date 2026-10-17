package com.shreyansh.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.shreyansh.core.diff.DiffEntry;
import com.shreyansh.core.diff.DiffResult;
import com.shreyansh.core.diff.DiffType;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link SideBySideRenderer}, the two-column view enabled by {@code -s}.
 */
public class SideBySideRendererTest {

  /** Width 43 gives two 13-column cells: 43 = 8 + 13 + 1 + 8 + 13. */
  private static final int WIDTH = 43;

  private static final DiffResult RESULT = new DiffResult(List.of(
      new DiffEntry("a.changed", "1.0", "1.1", DiffType.CHANGED),
      new DiffEntry("b.left", "x", null, DiffType.ONLY_IN_LEFT),
      new DiffEntry("c.right", null, "y", DiffType.ONLY_IN_RIGHT),
      new DiffEntry("d.same", "z", "z", DiffType.UNCHANGED)));

  private static final Map<String, Integer> LEFT_LINES =
      Map.of("a.changed", 3, "b.left", 5, "d.same", 7, "u", 2);
  private static final Map<String, Integer> RIGHT_LINES =
      Map.of("a.changed", 3, "c.right", 6, "d.same", 8, "u", 2);

  private final SideBySideRenderer renderer = new SideBySideRenderer();

  @Test
  void rendersTwoColumnsWithBlankSideForMissingKeys() {
    String expected = String.join("\n",
        "Δ left.properties ⟶ right.properties",
        "─".repeat(WIDTH),
        "",
        "changed (1)",
        "│   3 │ a.changed=1.0 │   3 │ a.changed=1.1",
        "",
        "only in left (1)",
        "│   5 │ b.left=x" + " ".repeat(6) + "│     │",
        "",
        "only in right (1)",
        "│     │ " + " ".repeat(13) + " │   6 │ c.right=y",
        "",
        "1 changed · 1 only in left · 1 only in right · 1 unchanged",
        "");

    assertEquals(expected, renderer.render(RESULT, options(WIDTH, false, false, true)));
  }

  @Test
  void lineNumbersAreBlankWithoutLineMaps() {
    String output = renderer.render(RESULT, options(WIDTH, false, false, false));

    assertTrue(output.contains("\n│     │ a.changed=1.0 │     │ a.changed=1.1\n"));
  }

  @Test
  void longValueWrapsWithMarkerAndContinuationRowsHaveNoNumbers() {
    DiffResult longValue = new DiffResult(List.of(new DiffEntry(
        "u", "jdbc:postgresql://localhost:5432/demo", null, DiffType.ONLY_IN_LEFT)));

    String output = renderer.render(longValue, options(WIDTH, false, false, true));

    assertTrue(output.contains(String.join("\n",
        "│   2 │ u=jdbc:postg↵ │     │",
        "│     │ resql://loca↵ │     │",
        "│     │ lhost:5432/d↵ │     │",
        "│     │ emo" + " ".repeat(11) + "│     │",
        "")));
  }

  @Test
  void sidesWrapIndependentlyAndShorterSideIsPadded() {
    DiffResult changed = new DiffResult(List.of(new DiffEntry(
        "u", "short", "jdbc:postgresql://db", DiffType.CHANGED)));

    String output = renderer.render(changed, options(WIDTH, false, false, true));

    assertTrue(output.contains(String.join("\n",
        "│   2 │ u=short" + " ".repeat(7) + "│   2 │ u=jdbc:postg↵",
        "│     │ " + " ".repeat(13) + " │     │ resql://db",
        "")));
  }

  @Test
  void tinyWidthStillGivesMinimumColumn() {
    String output = renderer.render(RESULT, options(Terminal.MIN_WIDTH, false, false, true));

    // MIN_COLUMN is 10, so "a.changed=1.0" (13) wraps after 9 characters
    assertTrue(output.contains("│   3 │ a.changed↵ │   3 │ a.changed↵\n"));
    assertTrue(output.contains("│     │ =1.0" + " ".repeat(7) + "│     │ =1.1\n"));
  }

  @Test
  void colorEmphasizesChangedPartOnEachSide() {
    String colored = renderer.render(RESULT, options(WIDTH, true, false, false));

    assertTrue(colored.contains(Style.MINUS_EMPH.paint("0", true)));
    assertTrue(colored.contains(Style.PLUS_EMPH.paint("1", true)));
    assertTrue(colored.contains(Style.MINUS.paint("a.changed=1.", true)));

    String plain = renderer.render(RESULT, options(WIDTH, false, false, false));
    assertFalse(plain.contains("\u001b"));
  }

  @Test
  void unchangedRowsShowBothSidesWhenRequested() {
    String output = renderer.render(RESULT, options(WIDTH, false, true, true));

    assertTrue(output.contains(
        "unchanged (1)\n│   7 │ d.same=z" + " ".repeat(6) + "│   8 │ d.same=z\n"));
  }

  private static RenderOptions options(int width, boolean color, boolean showUnchanged,
      boolean withLines) {
    return new RenderOptions("left.properties", "right.properties", width, color, showUnchanged,
        Set.of(), withLines ? LEFT_LINES : Map.of(), withLines ? RIGHT_LINES : Map.of());
  }
}
