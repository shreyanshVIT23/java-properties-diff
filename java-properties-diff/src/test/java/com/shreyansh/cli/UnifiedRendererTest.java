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
 * Unit tests for {@link UnifiedRenderer}, the default delta-like single-column view.
 */
public class UnifiedRendererTest {

  private static final DiffResult RESULT = new DiffResult(List.of(
      new DiffEntry("a.changed", "1.0", "1.1", DiffType.CHANGED),
      new DiffEntry("b.left", "x", null, DiffType.ONLY_IN_LEFT),
      new DiffEntry("c.right", null, "y", DiffType.ONLY_IN_RIGHT),
      new DiffEntry("d.same", "z", "z", DiffType.UNCHANGED)));

  private static final Map<String, Integer> LEFT_LINES =
      Map.of("a.changed", 3, "b.left", 5, "d.same", 7);
  private static final Map<String, Integer> RIGHT_LINES =
      Map.of("a.changed", 3, "c.right", 6, "d.same", 8);

  private final UnifiedRenderer renderer = new UnifiedRenderer();

  @Test
  void rendersHeaderSectionsGutterAndSummary() {
    String expected = String.join("\n",
        "Δ left.properties ⟶ right.properties",
        "─".repeat(30),
        "",
        "changed (1)",
        "   3 ⋮     │ - a.changed=1.0",
        "     ⋮   3 │ + a.changed=1.1",
        "",
        "only in left (1)",
        "   5 ⋮     │ - b.left=x",
        "",
        "only in right (1)",
        "     ⋮   6 │ + c.right=y",
        "",
        "1 changed · 1 only in left · 1 only in right · 1 unchanged",
        "");

    assertEquals(expected,
        renderer.render(RESULT, options(false, false, Set.of(), LEFT_LINES, RIGHT_LINES)));
  }

  @Test
  void lineNumbersAreBlankWithoutLineMaps() {
    String output = renderer.render(RESULT, options(false, false, Set.of(), Map.of(), Map.of()));

    assertTrue(output.contains("\n     ⋮     │ - a.changed=1.0\n"));
    assertTrue(output.contains("\n     ⋮     │ + c.right=y\n"));
  }

  @Test
  void showUnchangedAddsUnchangedSectionLast() {
    String output =
        renderer.render(RESULT, options(false, true, Set.of(), LEFT_LINES, RIGHT_LINES));

    assertTrue(output.contains(
        "     ⋮   6 │ + c.right=y\n\nunchanged (1)\n   7 ⋮   8 │   d.same=z\n\n1 changed"));
  }

  @Test
  void onlyFilterKeepsSelectedSectionsButSummaryCountsEverything() {
    String output = renderer.render(
        RESULT, options(false, false, Set.of(DiffType.ONLY_IN_RIGHT), LEFT_LINES, RIGHT_LINES));

    assertFalse(output.contains("changed (1)"));
    assertFalse(output.contains("only in left (1)"));
    assertTrue(output.contains("only in right (1)\n     ⋮   6 │ + c.right=y\n"));
    assertTrue(output.endsWith(
        "1 changed · 1 only in left · 1 only in right · 1 unchanged\n"));
  }

  @Test
  void identicalFilesSayNoDifferences() {
    DiffResult same = new DiffResult(List.of(new DiffEntry("k", "v", "v", DiffType.UNCHANGED)));

    assertEquals(
        "Δ left.properties ⟶ right.properties\n" + "─".repeat(30) + "\n\nno differences\n",
        renderer.render(same, options(false, false, Set.of(), Map.of(), Map.of())));
  }

  @Test
  void colorEmphasizesOnlyTheChangedPartAndIsAbsentWhenOff() {
    String colored = renderer.render(RESULT, options(true, false, Set.of(), Map.of(), Map.of()));

    // "1.0" -> "1.1": common prefix "1.", only the last character is emphasized
    assertTrue(colored.contains(Style.MINUS_EMPH.paint("0", true)));
    assertTrue(colored.contains(Style.PLUS_EMPH.paint("1", true)));
    assertTrue(colored.contains(Style.MINUS.paint("- a.changed=1.", true)));

    String plain = renderer.render(RESULT, options(false, false, Set.of(), Map.of(), Map.of()));
    assertFalse(plain.contains("\u001b"));
  }

  @Test
  void controlCharactersInKeysAndValuesAreShownEscaped() {
    DiffResult tabs =
        new DiffResult(List.of(new DiffEntry("k\tey", "a\tb", null, DiffType.ONLY_IN_LEFT)));

    String output = renderer.render(tabs, options(false, false, Set.of(), Map.of(), Map.of()));

    assertTrue(output.contains("│ - k\\tey=a\\tb\n"));
  }

  private static RenderOptions options(boolean color, boolean showUnchanged, Set<DiffType> only,
      Map<String, Integer> leftLines, Map<String, Integer> rightLines) {
    return new RenderOptions("left.properties", "right.properties", 30, color, showUnchanged,
        only, leftLines, rightLines);
  }
}
