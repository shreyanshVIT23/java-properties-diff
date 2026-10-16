package com.shreyansh;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link App}.
 */
public class AppTest {

  /**
   * The placeholder entry point runs without throwing.
   */
  @Test
  void mainRunsWithoutError() {
    assertDoesNotThrow(() -> App.main(new String[0]));
  }
}
