package com.shreyansh.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Unit tests for {@link WordDiff}, the within-value highlighting of changed values.
 */
public class WordDiffTest {

  @Test
  void identicalValuesHaveEmptyMiddles() {
    assertEquals(new WordDiff("same", "", "", ""), WordDiff.of("same", "same"));
  }

  @Test
  void changedMiddleKeepsCommonPrefixAndSuffix() {
    assertEquals(
        new WordDiff("jdbc:postgresql://", "localhost", "db.example.com", ":5432/demo"),
        WordDiff.of(
            "jdbc:postgresql://localhost:5432/demo",
            "jdbc:postgresql://db.example.com:5432/demo"));
  }

  @Test
  void nothingInCommonIsAllMiddle() {
    assertEquals(new WordDiff("", "abc", "xyz", ""), WordDiff.of("abc", "xyz"));
  }

  @Test
  void suffixNeverOverlapsPrefix() {
    assertEquals(new WordDiff("aa", "", "a", ""), WordDiff.of("aa", "aaa"));
    assertEquals(new WordDiff("ab", "", "ab", ""), WordDiff.of("ab", "abab"));
  }

  @Test
  void emptyValueOnEitherSide() {
    assertEquals(new WordDiff("", "", "x", ""), WordDiff.of("", "x"));
    assertEquals(new WordDiff("", "x", "", ""), WordDiff.of("x", ""));
  }

  @Test
  void neverSplitsSurrogatePairs() {
    // 😀 and 😁 share their high surrogate; a char-based split would cut them in half
    assertEquals(new WordDiff("a", "😀", "😁", "b"), WordDiff.of("a😀b", "a😁b"));
  }

  @ParameterizedTest
  @CsvSource({"1.0, 1.1", "localhost, example.com", "hello world, hello there world",
      "'', ''", "abcabc, abc"})
  void piecesReassembleIntoBothOriginals(String left, String right) {
    WordDiff diff = WordDiff.of(left, right);

    assertEquals(left, diff.prefix() + diff.leftMiddle() + diff.suffix());
    assertEquals(right, diff.prefix() + diff.rightMiddle() + diff.suffix());
  }

  @Test
  void visibleShowsControlCharactersAsEscapes() {
    assertEquals("a\\tb\\nc\\rd\\fe", WordDiff.visible("a\tb\nc\rd\fe"));
    assertEquals("plain text = ok", WordDiff.visible("plain text = ok"));
  }
}
