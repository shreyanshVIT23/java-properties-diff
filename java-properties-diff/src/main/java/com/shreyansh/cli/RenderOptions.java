package com.shreyansh.cli;

import com.shreyansh.core.diff.DiffType;
import java.util.Map;
import java.util.Set;

/**
 * Everything a renderer needs besides the diff itself.
 *
 * @param leftLabel name shown for the left file
 * @param rightLabel name shown for the right file
 * @param width output width in columns
 * @param color whether to emit ANSI colors
 * @param showUnchanged whether to list keys that are the same on both sides
 * @param only the diff types to list; empty means all
 * @param leftLines line number of each key in the left file; empty when unknown
 * @param rightLines line number of each key in the right file; empty when unknown
 */
public record RenderOptions(String leftLabel, String rightLabel, int width, boolean color,
    boolean showUnchanged, Set<DiffType> only, Map<String, Integer> leftLines,
    Map<String, Integer> rightLines) {

  /**
   * Stores unmodifiable copies of the collections.
   *
   * @param leftLabel name shown for the left file
   * @param rightLabel name shown for the right file
   * @param width output width in columns
   * @param color whether to emit ANSI colors
   * @param showUnchanged whether to list keys that are the same on both sides
   * @param only the diff types to list; empty means all
   * @param leftLines line number of each key in the left file; empty when unknown
   * @param rightLines line number of each key in the right file; empty when unknown
   */
  public RenderOptions {
    only = Set.copyOf(only);
    leftLines = Map.copyOf(leftLines);
    rightLines = Map.copyOf(rightLines);
  }
}
