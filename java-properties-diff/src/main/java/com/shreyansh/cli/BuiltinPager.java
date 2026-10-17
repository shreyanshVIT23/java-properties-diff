package com.shreyansh.cli;

import java.io.IOException;

/**
 * A pager that runs inside the JVM, used when there is no {@code $PAGER} and no system
 * {@code less}.
 */
@FunctionalInterface
interface BuiltinPager {

  /**
   * Shows the text page by page until the user quits.
   *
   * @param text the full output, possibly with ANSI colors
   * @throws IOException if there is no usable terminal; the caller then prints directly
   */
  void page(String text) throws IOException;
}
