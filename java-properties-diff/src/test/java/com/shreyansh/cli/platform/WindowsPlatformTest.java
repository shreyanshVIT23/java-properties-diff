package com.shreyansh.cli.platform;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Unit tests for {@link WindowsPlatform}. They only check lookup rules and command lines, never
 * start cmd, so they run on every OS.
 */
public class WindowsPlatformTest {

  private final Platform windows = new WindowsPlatform();

  @TempDir
  Path tempDir;

  @Test
  void lookupUsesSemicolonsPathextAndStripsQuotes() throws Exception {
    Path first = Files.createDirectory(tempDir.resolve("first"));
    Path second = Files.createDirectory(tempDir.resolve("second"));
    Path less = TestFiles.executable(second.resolve("less.EXE"), "");
    // Windows allows quoted PATH entries, such as a quoted "C:/Program Files/Git" folder
    String path = first + ";;\"" + second + "\"";

    assertEquals(Optional.of(less),
        windows.findOnPath("less", Map.of("PATH", path, "PATHEXT", ".COM;.EXE")));
  }

  @Test
  void variableNamesIgnoreCaseAndPathextHasDefault() throws Exception {
    Path bin = Files.createDirectory(tempDir.resolve("bin"));
    Path less = TestFiles.executable(bin.resolve("less.BAT"), "");

    // Windows usually spells it "Path"; without PATHEXT, .COM;.EXE;.BAT;.CMD applies
    assertEquals(Optional.of(less), windows.findOnPath("less", Map.of("Path", bin.toString())));
    assertEquals(Optional.of("x"), windows.env(Map.of("pAtH", "x"), "PATH"));
    assertEquals(Optional.of("exact"),
        windows.env(Map.of("PATH", "exact", "Path", "other"), "PATH"));
  }

  @Test
  void customPagerRunsThroughCmdAndItsExitCodeMeansCouldNotRun() {
    assertEquals(List.of("cmd", "/c", "more"), windows.shell("more"));
    assertTrue(windows.couldNotRun(9009));
    assertFalse(windows.couldNotRun(0));
    assertFalse(windows.couldNotRun(127));
  }
}
