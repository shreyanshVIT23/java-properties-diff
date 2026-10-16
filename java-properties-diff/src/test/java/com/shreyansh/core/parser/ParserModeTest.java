package com.shreyansh.core.parser;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * Unit tests for {@link ParserMode}.
 */
public class ParserModeTest {

  @Test
  void defaultModeCreatesDefaultParser() {
    assertInstanceOf(DefaultParser.class, ParserMode.DEFAULT.create());
  }

  @Test
  void customModeCreatesCustomParser() {
    assertInstanceOf(CustomParser.class, ParserMode.CUSTOM.create());
  }

  @ParameterizedTest
  @EnumSource(ParserMode.class)
  void everyModeCreatesFreshWorkingParser(ParserMode mode) throws Exception {
    Path file = Path.of(ParserModeTest.class.getResource("/test.properties").toURI());

    assertNotSame(mode.create(), mode.create());
    assertTrue(mode.create().parseFile(file).contains(new KeyValue("name", "shreyansh")));
  }
}
