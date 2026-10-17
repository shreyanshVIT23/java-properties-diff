package com.shreyansh.cli;

import com.shreyansh.core.diff.DiffEntry;
import com.shreyansh.core.diff.DiffResult;
import com.shreyansh.core.diff.DiffType;
import com.shreyansh.core.diff.WordDiff;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Two-column layout, {@code --side-by-side}: the left file's value on the left,
 * the
 * right file's on the right. Long values wrap onto extra rows, marked with
 * {@code ↵}.
 */
public class SideBySideRenderer {

  /**
   * Narrowest cell width, used when the terminal is too narrow for two real
   * columns.
   */
  static final int MIN_COLUMN = 10;

  /** Characters in one gutter besides the number itself: "│", " │ ". */
  private static final int GUTTER_EXTRA = 4;

  private static final String WRAP = "↵";

  /**
   * Renders the diff as two columns.
   *
   * @param result  the diff to show
   * @param options labels, width, colors, filters and line numbers
   * @return the full output, ending with a newline
   */
  public String render(DiffResult result, RenderOptions options) {
    int digits = options.numberWidth();
    // Two gutters plus the space between the cells; with 4 digits this is 17
    int chrome = 2 * (digits + GUTTER_EXTRA) + 1;
    int column = Math.max(MIN_COLUMN, (options.width() - chrome) / 2);
    StringBuilder out = new StringBuilder(Decorations.header(options));
    for (DiffType type : Decorations.visibleTypes(options)) {
      List<DiffEntry> entries = Decorations.ordered(result.byType(type), options);
      if (entries.isEmpty()) {
        continue;
      }
      out.append('\n')
          .append(Decorations.sectionTitle(type, entries.size(), options.color()))
          .append('\n');
      for (DiffEntry entry : entries) {
        out.append(rows(entry, options, column, digits));
      }
    }
    out.append('\n').append(Decorations.summary(result, options.color())).append('\n');
    return out.toString();
  }

  /** All rows of one entry; only the first row carries line numbers. */
  private static String rows(DiffEntry entry, RenderOptions options, int column, int digits) {
    Cell left = cell(entry, true);
    Cell right = cell(entry, false);
    List<int[]> leftChunks = left == null ? List.of() : chunks(left.points().length, column);
    List<int[]> rightChunks = right == null ? List.of() : chunks(right.points().length, column);

    StringBuilder out = new StringBuilder();
    for (int i = 0; i < Math.max(leftChunks.size(), rightChunks.size()); i++) {
      boolean color = options.color();
      String leftText = cellRow(left, leftChunks, i, column, color);
      String rightText = cellRow(right, rightChunks, i, 0, color);
      Integer leftLine = i == 0 && left != null ? options.leftLines().get(entry.key()) : null;
      Integer rightLine = i == 0 && right != null ? options.rightLines().get(entry.key()) : null;
      String row = gutter(leftLine, digits, color) + leftText + " "
          + gutter(rightLine, digits, color) + rightText;
      out.append(row.stripTrailing()).append('\n');
    }
    return out.toString();
  }

  /**
   * The styled text of one side, or {@code null} when the key is missing on that
   * side. For a
   * changed key, the part that differs gets the emphasis style.
   */
  private static Cell cell(DiffEntry entry, boolean leftSide) {
    String value = leftSide ? entry.leftValue() : entry.rightValue();
    if (value == null) {
      return null;
    }
    String keyPart = Style.visible(entry.key()) + "=";
    Style base = entry.type() == DiffType.UNCHANGED ? null : leftSide ? Style.MINUS : Style.PLUS;
    if (entry.type() != DiffType.CHANGED) {
      return Cell.of(new String[] { keyPart + Style.visible(value) }, new Style[] { base });
    }
    WordDiff diff = WordDiff.of(entry.leftValue(), entry.rightValue());
    String prefix = keyPart + Style.visible(diff.prefix());
    String middle = Style.visible(leftSide ? diff.leftMiddle() : diff.rightMiddle());
    String suffix = Style.visible(diff.suffix());
    Style emphasis = leftSide ? Style.MINUS_EMPH : Style.PLUS_EMPH;
    return Cell.of(new String[] { prefix, middle, suffix }, new Style[] { base, emphasis, base });
  }

  /**
   * Splits a cell of the given length into rows of {start, end, wrapped}. A
   * wrapped row holds
   * column - 1 code points plus the wrap marker; the last row holds at most
   * column.
   */
  private static List<int[]> chunks(int length, int column) {
    List<int[]> chunks = new ArrayList<>();
    int start = 0;
    while (length - start > column) {
      chunks.add(new int[] { start, start + column - 1, 1 });
      start += column - 1;
    }
    chunks.add(new int[] { start, length, 0 });
    return chunks;
  }

  /** One row of a cell, styled and padded with spaces to the given width. */
  private static String cellRow(Cell cell, List<int[]> chunks, int index, int padTo,
      boolean color) {
    if (index >= chunks.size()) {
      return " ".repeat(padTo);
    }
    int[] chunk = chunks.get(index);
    StringBuilder out = new StringBuilder();
    int start = chunk[0];
    while (start < chunk[1]) {
      // Paint runs of code points that share a style together
      Style style = cell.styles()[start];
      int end = start;
      while (end < chunk[1] && cell.styles()[end] == style) {
        end++;
      }
      String text = new String(cell.points(), start, end - start);
      out.append(style == null ? text : style.paint(text, color));
      start = end;
    }
    int width = chunk[1] - chunk[0];
    if (chunk[2] == 1) {
      Style last = cell.styles()[chunk[1] - 1];
      out.append(last == null ? WRAP : last.paint(WRAP, color));
      width++;
    }
    return out.append(" ".repeat(Math.max(0, padTo - width))).toString();
  }

  /** One side's line-number gutter, right-aligned in the given width; unknown is blank. */
  private static String gutter(Integer line, int digits, boolean color) {
    String text = String.format("│%" + digits + "s │ ", line == null ? "" : line);
    return Style.DIM.paint(text, color);
  }

  /**
   * Code points of a cell, each with its style ({@code null} means unstyled).
   *
   * @param points the text as code points
   * @param styles the style of each code point
   */
  private record Cell(int[] points, Style[] styles) {

    static Cell of(String[] texts, Style[] textStyles) {
      int[] points = Arrays.stream(texts).flatMapToInt(String::codePoints).toArray();
      Style[] styles = new Style[points.length];
      int next = 0;
      for (int i = 0; i < texts.length; i++) {
        int count = (int) texts[i].codePoints().count();
        Arrays.fill(styles, next, next + count, textStyles[i]);
        next += count;
      }
      return new Cell(points, styles);
    }
  }
}
