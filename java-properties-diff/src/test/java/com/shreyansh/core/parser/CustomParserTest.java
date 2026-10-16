package com.shreyansh.core.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Unit tests for {@link CustomParser}, one per rule of the
 * {@link java.util.Properties#load(java.io.Reader)} grammar.
 */
public class CustomParserTest {

  private final Parser parser = new CustomParser();

  @TempDir
  Path tempDir;

  @Test
  void skipsCommentsAndBlankLines() throws Exception {
    String content = "# hash comment\n"
        + "! bang comment\n"
        + "   # indented comment\n"
        + "# a comment ending in a backslash does not continue \\\n"
        + "\n"
        + "   \t \n"
        + "key=value\n";

    assertEquals(List.of(new KeyValue("key", "value")), parse(content));
  }

  @ParameterizedTest
  @ValueSource(strings = {"key=value", "key:value", "key value", "key\tvalue", "key\fvalue",
      "key = value", "key : value", "key   value", "  key=value", "key \t=\t value"})
  void everySeparatorFormSplitsKeyAndValue(String line) throws Exception {
    assertEquals(List.of(new KeyValue("key", "value")), parse(line + "\n"));
  }

  @Test
  void onlyFirstSeparatorSplitsTheRestBelongsToValue() throws Exception {
    assertEquals(
        List.of(new KeyValue("url", "http://host:80/a=b"), new KeyValue("k", "= v")),
        parse("url=http://host:80/a=b\nk = = v\n"));
  }

  @Test
  void escapedSeparatorsStayInKey() throws Exception {
    assertEquals(
        List.of(new KeyValue("a=b:c d", "v")),
        parse("a\\=b\\:c\\ d=v\n"));
  }

  @Test
  void lineContinuationJoinsLinesAndDropsLeadingWhitespace() throws Exception {
    String content = "multi=first \\\n"
        + "      second \\\r\n"
        + "\t  third\n"
        + "even=ends\\\\\n"
        + "next=after\r"
        + "last=done";

    assertEquals(
        List.of(
            new KeyValue("multi", "first second third"),
            new KeyValue("even", "ends\\"),
            new KeyValue("next", "after"),
            new KeyValue("last", "done")),
        parse(content));
  }

  @Test
  void decodesCharacterEscapes() throws Exception {
    assertEquals(
        List.of(new KeyValue("k", "t\tn\nr\rf\fb\\q")),
        parse("k=t\\tn\\nr\\rf\\fb\\\\\\q\n"));
  }

  @Test
  void decodesUnicodeEscapesAndUtf8Text() throws Exception {
    assertEquals(
        List.of(new KeyValue("Aé", "€"), new KeyValue("utf8", "héllo")),
        parse("\\u0041\\u00E9=\\u20ac\nutf8=héllo\n")); // file gets literal backslash-u escapes
  }

  @Test
  void valueKeepsInternalAndTrailingWhitespace() throws Exception {
    assertEquals(List.of(new KeyValue("key", "a  b  ")), parse("key =   a  b  \n"));
  }

  @ParameterizedTest
  @ValueSource(strings = {"key=", "key", "key   ", "key =   ", "key:"})
  void missingValueGivesEmptyString(String line) throws Exception {
    assertEquals(List.of(new KeyValue("key", "")), parse(line + "\n"));
  }

  @Test
  void keepsFileOrderAndDuplicateKeys() throws Exception {
    assertEquals(
        List.of(new KeyValue("b", "1"), new KeyValue("a", "2"), new KeyValue("b", "3")),
        parse("b=1\na=2\nb=3\n"));
  }

  @ParameterizedTest
  @ValueSource(strings = {"key=\\u12zz", "key=\\u12", "\\uXYZW=value"})
  void malformedUnicodeEscapeThrows(String line) throws Exception {
    Path file = Files.writeString(tempDir.resolve("bad.properties"), line + "\n");

    PropertiesReadException ex =
        assertThrows(PropertiesReadException.class, () -> parser.parseFile(file));
    assertTrue(ex.getMessage().contains(file.toString()));
  }

  @Test
  void missingFileThrowsWithIoCause() {
    Path missing = tempDir.resolve("does-not-exist.properties");

    PropertiesReadException ex =
        assertThrows(PropertiesReadException.class, () -> parser.parseFile(missing));
    assertInstanceOf(NoSuchFileException.class, ex.getCause());
  }

  private List<KeyValue> parse(String content) throws Exception {
    Path file = Files.writeString(tempDir.resolve("input.properties"), content);
    return parser.parseFile(file);
  }
}
