package com.shreyansh.core.parser;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;

/**
 * Hand-written parser for the {@link java.util.Properties#load(java.io.Reader)}
 * format.
 *
 * <p>Unlike {@link DefaultParser}, it returns the pairs in file order and keeps
 * every occurrence of a duplicated key.
 */
public class CustomParser implements Parser {

  @Override
  public List<KeyValue> parseFile(Path path) throws PropertiesReadException {
    try {
      List<KeyValue> result = new ArrayList<>();
      for (String line : logicalLines(Files.readString(path))) {
        result.add(parseLine(line));
      }
      return List.copyOf(result);
    } catch (IOException | IllegalArgumentException e) {
      // IllegalArgumentException comes from a malformed unicode escape, same as
      // DefaultParser
      throw new PropertiesReadException(path, e);
    }
  }

  /**
   * Joins natural lines into logical lines and drops blank and comment lines.
   * Every returned
   * line starts at its first non-white space character.
   */
  private static List<String> logicalLines(String content) {
    List<String> logical = new ArrayList<>();
    StringBuilder current = null;
    // String.lines() splits on \n, \r and \r\n: the same terminators Properties
    // uses
    for (String natural : content.lines().toList()) {
      // Leading white space is ignored on new lines and on continuation lines alike
      String line = natural.substring(skipWhitespace(natural, 0));
      if (current == null) {
        // Comment and blank checks only apply at the start of a logical line
        if (line.isEmpty() || line.charAt(0) == '#' || line.charAt(0) == '!') {
          continue;
        }
        current = new StringBuilder();
      }
      if (endsWithOddBackslashes(line)) {
        current.append(line, 0, line.length() - 1);
      } else {
        logical.add(current.append(line).toString());
        current = null;
      }
    }
    if (current != null) {
      // File ended right after a continuation backslash
      logical.add(current.toString());
    }
    return logical;
  }

  /**
   * Splits a logical line at the first unescaped '=', ':' or white space, then
   * decodes escapes.
   */
  private static KeyValue parseLine(String line) {
    int keyEnd = 0;
    boolean escaped = false;
    while (keyEnd < line.length()) {
      char c = line.charAt(keyEnd);
      if (escaped) {
        escaped = false;
      } else if (c == '\\') {
        escaped = true;
      } else if (c == '=' || c == ':' || isWhitespace(c)) {
        break;
      }
      keyEnd++;
    }
    int valueStart = skipWhitespace(line, keyEnd);
    if (valueStart < line.length()) {
      char separator = line.charAt(valueStart);
      // At most one '=' or ':' is consumed; anything after it belongs to the value
      if (separator == '=' || separator == ':') {
        valueStart = skipWhitespace(line, valueStart + 1);
      }
    }
    return new KeyValue(unescape(line.substring(0, keyEnd)), unescape(line.substring(valueStart)));
  }

  /**
   * Decodes {@code \t \n \r \f \\uXXXX}; any other escaped character stands for
   * itself.
   *
   * @throws IllegalArgumentException on a malformed unicode escape
   */
  private static String unescape(String raw) {
    StringBuilder out = new StringBuilder(raw.length());
    int i = 0;
    while (i < raw.length()) {
      char c = raw.charAt(i++);
      if (c != '\\') {
        out.append(c);
        continue;
      }
      if (i == raw.length()) {
        // A lone trailing backslash is dropped, as Properties does
        break;
      }
      char escape = raw.charAt(i++);
      if (escape == 'u') {
        if (i + 4 > raw.length()) {
          throw new IllegalArgumentException("Malformed \\uxxxx encoding.");
        }
        // fromHexDigits accepts only ASCII hex digits and throws
        // IllegalArgumentException otherwise
        out.append((char) HexFormat.fromHexDigits(raw, i, i + 4));
        i += 4;
      } else {
        char decoded = switch (escape) {
          case 't' -> '\t';
          case 'n' -> '\n';
          case 'r' -> '\r';
          case 'f' -> '\f';
          default -> escape;
        };
        out.append(decoded);
      }
    }
    return out.toString();
  }

  /**
   * Returns the index of the first non-white space character at or after the
   * given index.
   */
  private static int skipWhitespace(String s, int from) {
    int i = from;
    while (i < s.length() && isWhitespace(s.charAt(i))) {
      i++;
    }
    return i;
  }

  /** White space as defined by Properties: space, tab and form feed. */
  private static boolean isWhitespace(char c) {
    return c == ' ' || c == '\t' || c == '\f';
  }

  /** An odd run of trailing backslashes means the line terminator is escaped. */
  private static boolean endsWithOddBackslashes(String line) {
    int count = 0;
    for (int i = line.length() - 1; i >= 0 && line.charAt(i) == '\\'; i--) {
      count++;
    }
    return count % 2 == 1;
  }
}
