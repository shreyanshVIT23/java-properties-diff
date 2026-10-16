package com.shreyansh.core.diff;

/**
 * How a key compares between the left and right properties files.
 */
public enum DiffType {

  /** The key is in both files with the same value. */
  UNCHANGED,

  /** The key is in both files with different values. */
  CHANGED,

  /** The key is only in the left file. */
  ONLY_IN_LEFT,

  /** The key is only in the right file. */
  ONLY_IN_RIGHT
}
