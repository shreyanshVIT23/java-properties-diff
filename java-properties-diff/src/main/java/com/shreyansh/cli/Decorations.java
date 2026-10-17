package com.shreyansh.cli;

import com.shreyansh.core.diff.DiffResult;
import com.shreyansh.core.diff.DiffType;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Header, section titles, line-number gutter and summary shared by both layouts.
 */
final class Decorations {

  /** Sections are always printed in this order. */
  private static final List<DiffType> SECTION_ORDER = List.of(
      DiffType.CHANGED, DiffType.ONLY_IN_LEFT, DiffType.ONLY_IN_RIGHT, DiffType.UNCHANGED);

  private Decorations() {
  }

  /** The "Δ left ⟶ right" title followed by a rule across the full width. */
  static String header(RenderOptions options) {
    String title = "Δ " + options.leftLabel() + " ⟶ " + options.rightLabel();
    String rule = "─".repeat(options.width());
    return Style.HEADER.paint(title, options.color()) + "\n"
        + Style.HEADER.paint(rule, options.color()) + "\n";
  }

  /**
   * The section types to print, in order. UNCHANGED appears only when asked for, either with
   * {@code --show-unchanged} or by naming it in {@code --only}.
   */
  static List<DiffType> visibleTypes(RenderOptions options) {
    return SECTION_ORDER.stream()
        .filter(type -> options.only().isEmpty() || options.only().contains(type))
        .filter(type -> type != DiffType.UNCHANGED
            || options.showUnchanged()
            || options.only().contains(type))
        .toList();
  }

  /** A section title such as "changed (2)". */
  static String sectionTitle(DiffType type, int count, boolean color) {
    return Style.HEADER.paint(label(type) + " (" + count + ")", color);
  }

  /** Non-zero counts of the whole result (filters ignored), or "no differences". */
  static String summary(DiffResult result, boolean color) {
    if (!result.hasDifferences()) {
      return Style.DIM.paint("no differences", color);
    }
    String counts = SECTION_ORDER.stream()
        .filter(type -> !result.byType(type).isEmpty())
        .map(type -> result.byType(type).size() + " " + label(type))
        .collect(Collectors.joining(" · "));
    return Style.DIM.paint(counts, color);
  }

  /** Delta-style line-number gutter such as "   3 ⋮   4 │ "; unknown numbers are blank. */
  static String gutter(Integer leftLine, Integer rightLine, boolean color) {
    String text = String.format("%4s ⋮%4s │ ",
        leftLine == null ? "" : leftLine, rightLine == null ? "" : rightLine);
    return Style.DIM.paint(text, color);
  }

  private static String label(DiffType type) {
    return switch (type) {
      case CHANGED -> "changed";
      case ONLY_IN_LEFT -> "only in left";
      case ONLY_IN_RIGHT -> "only in right";
      case UNCHANGED -> "unchanged";
    };
  }
}
