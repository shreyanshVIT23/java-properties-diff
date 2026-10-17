package com.shreyansh.core.diff;

import com.shreyansh.core.parser.KeyValue;
import com.shreyansh.core.parser.Parser;
import com.shreyansh.core.parser.ParserMode;
import com.shreyansh.core.parser.PropertiesReadException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Compares two sets of properties key by key.
 */
public class PropertiesDiff {

  /**
   * Compares the left and right pairs. When a key appears more than once on one side, its last
   * value is used, as {@link java.util.Properties} does.
   *
   * @param left the pairs of the left file
   * @param right the pairs of the right file
   * @return one entry per key found on either side, sorted by key
   */
  public DiffResult compare(List<KeyValue> left, List<KeyValue> right) {
    Map<String, String> leftValues = lastValueByKey(left);
    Map<String, String> rightValues = lastValueByKey(right);
    SortedSet<String> keys = new TreeSet<>(leftValues.keySet());
    keys.addAll(rightValues.keySet());

    List<DiffEntry> entries = new ArrayList<>();
    for (String key : keys) {
      String leftValue = leftValues.get(key);
      String rightValue = rightValues.get(key);
      entries.add(new DiffEntry(key, leftValue, rightValue, typeOf(leftValue, rightValue)));
    }
    return new DiffResult(entries);
  }

  /**
   * Parses both files with the given mode and compares them.
   *
   * @param left the left properties file
   * @param right the right properties file
   * @param mode which parser reads the files
   * @return one entry per key found in either file, sorted by key
   * @throws PropertiesReadException if either file cannot be read or is malformed
   */
  public DiffResult compare(Path left, Path right, ParserMode mode)
      throws PropertiesReadException {
    Parser parser = mode.create();
    return compare(parser.parseFile(left), parser.parseFile(right));
  }

  private static Map<String, String> lastValueByKey(List<KeyValue> pairs) {
    Map<String, String> values = new HashMap<>();
    for (KeyValue pair : pairs) {
      values.put(pair.key(), pair.value());
    }
    return values;
  }

  // Parsers never produce null values, so null here means the key is missing on that side
  private static DiffType typeOf(String leftValue, String rightValue) {
    if (rightValue == null) {
      return DiffType.ONLY_IN_LEFT;
    }
    if (leftValue == null) {
      return DiffType.ONLY_IN_RIGHT;
    }
    return leftValue.equals(rightValue) ? DiffType.UNCHANGED : DiffType.CHANGED;
  }
}
