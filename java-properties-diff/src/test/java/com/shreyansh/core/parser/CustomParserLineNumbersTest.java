package com.shreyansh.core.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Tests for the line numbers reported by {@link CustomParser#parseFileWithLines(Path)}.
 */
public class CustomParserLineNumbersTest {

  private final CustomParser parser = new CustomParser();

  @TempDir
  Path tempDir;

  @Test
  void numbersPlainLinesFromOne() throws Exception {
    assertEquals(
        List.of(
            new NumberedKeyValue(new KeyValue("a", "1"), 1),
            new NumberedKeyValue(new KeyValue("b", "2"), 2)),
        parse("a=1\nb=2\n"));
  }

  @Test
  void commentsAndBlankLinesStillCountAsLines() throws Exception {
    assertEquals(
        List.of(
            new NumberedKeyValue(new KeyValue("a", "1"), 4),
            new NumberedKeyValue(new KeyValue("b", "2"), 6)),
        parse("# comment\n\n  ! another\na=1\n\nb=2"));
  }

  @Test
  void continuationReportsStartLineAndLaterLinesKeepCounting() throws Exception {
    assertEquals(
        List.of(
            new NumberedKeyValue(new KeyValue("a", "1 2 3"), 1),
            new NumberedKeyValue(new KeyValue("b", "4"), 4),
            new NumberedKeyValue(new KeyValue("b", "5"), 5)),
        parse("a=1 \\\n  2 \\\n  3\nb=4\nb=5\n"));
  }

  @Test
  void everyLineTerminatorAdvancesTheCount() throws Exception {
    assertEquals(
        List.of(
            new NumberedKeyValue(new KeyValue("a", "1"), 1),
            new NumberedKeyValue(new KeyValue("b", "2"), 2),
            new NumberedKeyValue(new KeyValue("c", "3"), 3)),
        parse("a=1\r\nb=2\rc=3\n"));
  }

  @Test
  void fixtureLinesMatchAndParseFileReturnsSamePairs() throws Exception {
    Path file = fixture();
    List<NumberedKeyValue> entries = parser.parseFileWithLines(file);

    assertEquals(
        List.of(2, 3, 4, 5, 6, 8, 9, 10),
        entries.stream().map(NumberedKeyValue::line).toList());
    assertEquals(new KeyValue("greeting", "hello world"), entries.get(7).pair());
    assertEquals(entries.stream().map(NumberedKeyValue::pair).toList(), parser.parseFile(file));
  }

  @Test
  void onlyCustomModeIsLineAware() {
    assertInstanceOf(LineAwareParser.class, ParserMode.CUSTOM.create());
    assertFalse(ParserMode.DEFAULT.create() instanceof LineAwareParser);
  }

  private List<NumberedKeyValue> parse(String content) throws Exception {
    Path file = Files.writeString(tempDir.resolve("input.properties"), content);
    return parser.parseFileWithLines(file);
  }

  private static Path fixture() throws Exception {
    return Path.of(
        CustomParserLineNumbersTest.class.getResource("/fixtures/left.properties").toURI());
  }
}
