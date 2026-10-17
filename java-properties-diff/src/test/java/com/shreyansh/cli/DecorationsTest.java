package com.shreyansh.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link Decorations#naturalCompare(String, String)}, the key order used when
 * line numbers are unknown.
 */
public class DecorationsTest {

  @Test
  void digitRunsCompareAsNumbers() {
    List<String> keys = new ArrayList<>(
        List.of("key.10", "key.a", "key.9", "key.100", "app", "key.", "key.1002", "key.10002"));

    keys.sort(Decorations::naturalCompare);

    assertEquals(
        List.of("app", "key.", "key.9", "key.10", "key.100", "key.1002", "key.10002", "key.a"),
        keys);
  }

  @Test
  void leadingZerosAndHugeNumbersAreOrderedWithoutOverflow() {
    List<String> keys = new ArrayList<>(List.of(
        "v100000000000000000000", "v01", "v99999999999999999999", "v1", "v2"));

    keys.sort(Decorations::naturalCompare);

    // Equal values: fewer leading zeros first; 20+ digit runs never overflow a long
    assertEquals(
        List.of("v1", "v01", "v2", "v99999999999999999999", "v100000000000000000000"), keys);
  }
}
