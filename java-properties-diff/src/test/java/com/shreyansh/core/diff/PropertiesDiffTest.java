package com.shreyansh.core.diff;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.shreyansh.core.parser.KeyValue;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link PropertiesDiff#compare(List, List)}.
 */
public class PropertiesDiffTest {

  private final PropertiesDiff diff = new PropertiesDiff();

  @Test
  void identicalInputsAreAllUnchanged() {
    List<KeyValue> props = List.of(new KeyValue("a", "1"), new KeyValue("b", "2"));

    DiffResult result = diff.compare(props, props);

    assertEquals(
        List.of(
            new DiffEntry("a", "1", "1", DiffType.UNCHANGED),
            new DiffEntry("b", "2", "2", DiffType.UNCHANGED)),
        result.entries());
    assertFalse(result.hasDifferences());
  }

  @Test
  void sameKeyDifferentValueIsChanged() {
    DiffResult result = diff.compare(
        List.of(new KeyValue("host", "localhost"), new KeyValue("name", "v")),
        List.of(new KeyValue("host", "example.com"), new KeyValue("name", "v ")));

    // Values are compared exactly: trailing white space counts
    assertEquals(
        List.of(
            new DiffEntry("host", "localhost", "example.com", DiffType.CHANGED),
            new DiffEntry("name", "v", "v ", DiffType.CHANGED)),
        result.entries());
    assertTrue(result.hasDifferences());
  }

  @Test
  void keyMissingFromRightIsOnlyInLeft() {
    DiffResult result = diff.compare(List.of(new KeyValue("old", "x")), List.of());

    assertEquals(
        List.of(new DiffEntry("old", "x", null, DiffType.ONLY_IN_LEFT)), result.entries());
    assertTrue(result.hasDifferences());
  }

  @Test
  void keyMissingFromLeftIsOnlyInRight() {
    DiffResult result = diff.compare(List.of(), List.of(new KeyValue("new", "y")));

    assertEquals(
        List.of(new DiffEntry("new", null, "y", DiffType.ONLY_IN_RIGHT)), result.entries());
    assertTrue(result.hasDifferences());
  }

  @Test
  void bothEmptyGivesNoEntries() {
    DiffResult result = diff.compare(List.of(), List.of());

    assertTrue(result.entries().isEmpty());
    assertFalse(result.hasDifferences());
  }

  @Test
  void emptyValueIsDistinctFromMissingKey() {
    DiffResult result = diff.compare(
        List.of(new KeyValue("blank", ""), new KeyValue("gone", "")),
        List.of(new KeyValue("blank", "filled")));

    assertEquals(
        List.of(
            new DiffEntry("blank", "", "filled", DiffType.CHANGED),
            new DiffEntry("gone", "", null, DiffType.ONLY_IN_LEFT)),
        result.entries());
  }

  @Test
  void duplicateKeyUsesLastValueLikeProperties() {
    DiffResult result = diff.compare(
        List.of(new KeyValue("k", "first"), new KeyValue("k", "second")),
        List.of(new KeyValue("k", "second")));

    assertEquals(
        List.of(new DiffEntry("k", "second", "second", DiffType.UNCHANGED)), result.entries());
  }

  @Test
  void mixedInputIsSortedByKeyAndFilterableByType() {
    DiffResult result = diff.compare(
        List.of(new KeyValue("d", "1"), new KeyValue("b", "1"), new KeyValue("a", "1")),
        List.of(new KeyValue("c", "1"), new KeyValue("b", "2"), new KeyValue("a", "1")));

    assertEquals(
        List.of("a", "b", "c", "d"),
        result.entries().stream().map(DiffEntry::key).toList());
    assertEquals(
        List.of(new DiffEntry("a", "1", "1", DiffType.UNCHANGED)),
        result.byType(DiffType.UNCHANGED));
    assertEquals(
        List.of(new DiffEntry("b", "1", "2", DiffType.CHANGED)),
        result.byType(DiffType.CHANGED));
    assertEquals(
        List.of(new DiffEntry("d", "1", null, DiffType.ONLY_IN_LEFT)),
        result.byType(DiffType.ONLY_IN_LEFT));
    assertEquals(
        List.of(new DiffEntry("c", null, "1", DiffType.ONLY_IN_RIGHT)),
        result.byType(DiffType.ONLY_IN_RIGHT));
  }
}
