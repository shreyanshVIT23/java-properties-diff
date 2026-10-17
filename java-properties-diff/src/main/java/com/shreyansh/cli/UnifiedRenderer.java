package com.shreyansh.cli;

import com.shreyansh.core.diff.DiffEntry;
import com.shreyansh.core.diff.DiffResult;
import com.shreyansh.core.diff.DiffType;
import com.shreyansh.core.diff.WordDiff;
import java.util.List;

/**
 * Default single-column layout, like delta's: a removed row followed by an added row, each
 * behind a line-number gutter.
 */
public class UnifiedRenderer {

  /**
   * Renders the diff as text.
   *
   * @param result the diff to show
   * @param options labels, width, colors, filters and line numbers
   * @return the full output, ending with a newline
   */
  public String render(DiffResult result, RenderOptions options) {
    int digits = options.numberWidth();
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
        out.append(rows(entry, options, digits));
      }
    }
    out.append('\n').append(Decorations.summary(result, options.color())).append('\n');
    return out.toString();
  }

  /** The one or two rows of an entry, each ending with a newline. */
  private static String rows(DiffEntry entry, RenderOptions options, int digits) {
    Integer leftLine = options.leftLines().get(entry.key());
    Integer rightLine = options.rightLines().get(entry.key());
    boolean color = options.color();
    String keyPart = Style.visible(entry.key()) + "=";

    // A switch expression must cover every DiffType, so a new type fails to compile here
    return switch (entry.type()) {
      case CHANGED -> {
        WordDiff diff = WordDiff.of(entry.leftValue(), entry.rightValue());
        yield Decorations.gutter(leftLine, null, digits, color)
            + highlighted("- " + keyPart, diff.prefix(), diff.leftMiddle(), diff.suffix(),
                Style.MINUS, Style.MINUS_EMPH, color)
            + "\n"
            + Decorations.gutter(null, rightLine, digits, color)
            + highlighted("+ " + keyPart, diff.prefix(), diff.rightMiddle(), diff.suffix(),
                Style.PLUS, Style.PLUS_EMPH, color)
            + "\n";
      }
      case ONLY_IN_LEFT -> Decorations.gutter(leftLine, null, digits, color)
          + Style.MINUS.paint("- " + keyPart + Style.visible(entry.leftValue()), color) + "\n";
      case ONLY_IN_RIGHT -> Decorations.gutter(null, rightLine, digits, color)
          + Style.PLUS.paint("+ " + keyPart + Style.visible(entry.rightValue()), color) + "\n";
      case UNCHANGED -> Decorations.gutter(leftLine, rightLine, digits, color)
          + "  " + keyPart + Style.visible(entry.leftValue()) + "\n";
    };
  }

  /**
   * One changed row: the shared parts in the row style, the changed middle in the stronger
   * emphasis style.
   */
  private static String highlighted(String lead, String prefix, String middle, String suffix,
      Style row, Style emphasis, boolean color) {
    return row.paint(lead + Style.visible(prefix), color)
        + emphasis.paint(Style.visible(middle), color)
        + row.paint(Style.visible(suffix), color);
  }
}
