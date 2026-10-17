package com.shreyansh.core.diff;

/**
 * Splits two values into the part they share and the part that changed, so a front end (CLI,
 * GUI) can emphasize only the changed part, like delta's word-level highlighting.
 *
 * @param prefix the common start of both values
 * @param leftMiddle the changed part of the left value
 * @param rightMiddle the changed part of the right value
 * @param suffix the common end of both values; it never overlaps the prefix
 */
public record WordDiff(String prefix, String leftMiddle, String rightMiddle, String suffix) {

  /**
   * Compares two values by code point, so surrogate pairs are never split.
   *
   * @param left the left value
   * @param right the right value
   * @return the common prefix, both changed middles and the common suffix
   */
  public static WordDiff of(String left, String right) {
    int[] leftPoints = left.codePoints().toArray();
    int[] rightPoints = right.codePoints().toArray();
    int shorter = Math.min(leftPoints.length, rightPoints.length);

    int prefix = 0;
    while (prefix < shorter && leftPoints[prefix] == rightPoints[prefix]) {
      prefix++;
    }
    // Only what is left after the prefix may be matched as suffix, so the two never overlap
    int suffix = 0;
    while (suffix < shorter - prefix
        && leftPoints[leftPoints.length - 1 - suffix]
            == rightPoints[rightPoints.length - 1 - suffix]) {
      suffix++;
    }

    return new WordDiff(
        text(leftPoints, 0, prefix),
        text(leftPoints, prefix, leftPoints.length - suffix),
        text(rightPoints, prefix, rightPoints.length - suffix),
        text(leftPoints, leftPoints.length - suffix, leftPoints.length));
  }

  private static String text(int[] codePoints, int from, int to) {
    return new String(codePoints, from, to - from);
  }
}
