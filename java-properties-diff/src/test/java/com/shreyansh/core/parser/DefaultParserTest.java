package com.shreyansh.core.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Unit tests for {@link DefaultParser}.
 */
public class DefaultParserTest {

  private final Parser parser = new DefaultParser();

  @TempDir
  Path tempDir;

  @Test
  void parsesFixtureSortedByKey() throws Exception {
    List<KeyValue> expected = List.of(
        new KeyValue("build", "maven"),
        new KeyValue("name", "shreyansh"),
        new KeyValue("project", "java-properties-diff"));

    assertEquals(expected, parser.parseFile(resource("/test.properties")));
  }

  @Test
  void emptyFileGivesEmptyList() throws Exception {
    Path file = write("");

    assertTrue(parser.parseFile(file).isEmpty());
  }

  @Test
  void duplicateKeyKeepsLastValue() throws Exception {
    Path file = write("key=first\nkey=second\n");

    assertEquals(List.of(new KeyValue("key", "second")), parser.parseFile(file));
  }

  @Test
  void missingFileThrowsWithIoCause() {
    Path missing = tempDir.resolve("does-not-exist.properties");

    PropertiesReadException ex =
        assertThrows(PropertiesReadException.class, () -> parser.parseFile(missing));
    assertInstanceOf(NoSuchFileException.class, ex.getCause());
    assertTrue(ex.getMessage().contains(missing.toString()));
  }

  @Test
  void malformedUnicodeEscapeThrows() throws Exception {
    Path file = write("key=\\u12zz\n");

    PropertiesReadException ex =
        assertThrows(PropertiesReadException.class, () -> parser.parseFile(file));
    assertInstanceOf(IllegalArgumentException.class, ex.getCause());
  }

  private Path write(String content) throws IOException {
    return Files.writeString(tempDir.resolve("input.properties"), content);
  }

  private static Path resource(String name) throws URISyntaxException {
    return Path.of(DefaultParserTest.class.getResource(name).toURI());
  }
}
