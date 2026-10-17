package com.shreyansh.cli;

import com.shreyansh.core.diff.DiffEntry;
import com.shreyansh.core.diff.DiffResult;
import com.shreyansh.core.diff.DiffType;
import java.util.Comparator;
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

  /**
   * Delta-style line-number gutter such as "   3 ⋮   4 │ ", each number right-aligned in
   * the given width; unknown numbers are blank.
   */
  static String gutter(Integer leftLine, Integer rightLine, int digits, boolean color) {
    String number = "%" + digits + "s";
    String text = String.format(number + " ⋮" + number + " │ ",
        leftLine == null ? "" : leftLine, rightLine == null ? "" : rightLine);
    return Style.DIM.paint(text, color);
  }

  /**
   * The entries of one section in file order: by the line of the side the entry is shown on
   * (left, or right for keys only in the right file). Entries without a known line come last,
   * in natural key order, which is all of them when line numbers are unknown.
   */
  static List<DiffEntry> ordered(List<DiffEntry> entries, RenderOptions options) {
    Comparator<DiffEntry> byLine = Comparator.comparing(
        (DiffEntry entry) -> lineOf(entry, options),
        Comparator.nullsLast(Comparator.<Integer>naturalOrder()));
    return entries.stream()
        .sorted(byLine.thenComparing(DiffEntry::key, Decorations::naturalCompare))
        .toList();
  }

  /**
   * Compares keys so that runs of ASCII digits count as numbers: key.9 before key.10. Equal
   * numbers with more leading zeros sort later; digit runs are compared by length, then
   * digit by digit, so they never overflow.
   */
  static int naturalCompare(String a, String b) {
    int i = 0;
    int j = 0;
    while (i < a.length() && j < b.length()) {
      if (!isDigit(a.charAt(i)) || !isDigit(b.charAt(j))) {
        if (a.charAt(i) != b.charAt(j)) {
          return Character.compare(a.charAt(i), b.charAt(j));
        }
        i++;
        j++;
        continue;
      }
      // Skip leading zeros, but keep one digit so "0" stays a run
      int endA = digitRunEnd(a, i);
      int startA = i;
      while (startA < endA - 1 && a.charAt(startA) == '0') {
        startA++;
      }
      int endB = digitRunEnd(b, j);
      int startB = j;
      while (startB < endB - 1 && b.charAt(startB) == '0') {
        startB++;
      }
      // Without leading zeros, a longer run is a bigger number
      if (endA - startA != endB - startB) {
        return Integer.compare(endA - startA, endB - startB);
      }
      int digits = a.substring(startA, endA).compareTo(b.substring(startB, endB));
      if (digits != 0) {
        return digits;
      }
      int zeros = Integer.compare(startA - i, startB - j);
      if (zeros != 0) {
        return zeros;
      }
      i = endA;
      j = endB;
    }
    int rest = Integer.compare(a.length() - i, b.length() - j);
    return rest != 0 ? rest : a.compareTo(b);
  }

  private static Integer lineOf(DiffEntry entry, RenderOptions options) {
    return entry.leftValue() != null
        ? options.leftLines().get(entry.key())
        : options.rightLines().get(entry.key());
  }

  private static boolean isDigit(char c) {
    return c >= '0' && c <= '9';
  }

  private static int digitRunEnd(String s, int from) {
    int end = from;
    while (end < s.length() && isDigit(s.charAt(end))) {
      end++;
    }
    return end;
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
