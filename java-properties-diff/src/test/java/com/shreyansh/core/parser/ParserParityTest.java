package com.shreyansh.core.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Checks that {@link CustomParser} reads files exactly like {@link DefaultParser}
 * (which wraps {@link java.util.Properties}) when there are no duplicate keys.
 */
public class ParserParityTest {

  @ParameterizedTest
  @ValueSource(strings = {"/test.properties", "/fixtures/spec-edge-cases.properties"})
  void customMatchesDefaultAfterSortingByKey(String resource) throws Exception {
    Path file = Path.of(ParserParityTest.class.getResource(resource).toURI());

    List<KeyValue> expected = new DefaultParser().parseFile(file);
    List<KeyValue> actual = new CustomParser().parseFile(file).stream()
        .sorted(Comparator.comparing(KeyValue::key))
        .toList();

    assertFalse(expected.isEmpty(), "fixture should not be empty: " + resource);
    assertEquals(expected, actual);
  }
}
