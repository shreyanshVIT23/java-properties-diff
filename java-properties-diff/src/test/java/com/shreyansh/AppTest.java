package com.shreyansh;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link App}. They call {@code App.run}, never {@code main}, because main calls
 * {@link System#exit} and would stop the test JVM.
 */
public class AppTest {

  @Test
  void helpExitsWithZero() {
    assertEquals(0, App.run(new String[] {"--help"}));
  }

  @Test
  void runsTheDiffCommandOnRealFiles() {
    String left = Path.of("src", "test", "resources", "fixtures", "left.properties").toString();
    String right = Path.of("src", "test", "resources", "fixtures", "right.properties").toString();

    assertEquals(1, App.run(new String[] {left, right}));
    assertEquals(0, App.run(new String[] {left, left}));
  }
}
