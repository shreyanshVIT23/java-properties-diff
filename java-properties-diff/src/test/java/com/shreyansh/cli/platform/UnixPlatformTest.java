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
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

/**
 * Unit tests for {@link UnixPlatform} (Linux, macOS). Disabled on Windows, where a file cannot
 * be made non-executable to test the skip rule.
 */
@DisabledOnOs(OS.WINDOWS)
public class UnixPlatformTest {

  private final Platform unix = new UnixPlatform();

  @TempDir
  Path tempDir;

  @Test
  void findsTheExecutableInLaterPathEntry() throws Exception {
    Path first = Files.createDirectory(tempDir.resolve("first"));
    Path second = Files.createDirectory(tempDir.resolve("second"));
    Path less = TestFiles.executable(second.resolve("less"), "#!/bin/sh\n");

    assertEquals(Optional.of(less),
        unix.findOnPath("less", Map.of("PATH", first + ":" + second)));
  }

  @Test
  void skipsDirectoriesNonExecutableFilesAndEmptyEntries() throws Exception {
    Path withDirectory = Files.createDirectories(tempDir.resolve("a").resolve("less"));
    Path withPlainFile = Files.createDirectory(tempDir.resolve("b"));
    Files.writeString(withPlainFile.resolve("less"), "#!/bin/sh\nexit 0\n");
    String path = withDirectory.getParent() + ":" + withPlainFile + "::";

    assertEquals(Optional.empty(), unix.findOnPath("less", Map.of("PATH", path)));
  }

  @Test
  void variableNamesAreCaseSensitive() {
    assertEquals(Optional.of("/bin"), unix.env(Map.of("PATH", "/bin"), "PATH"));
    assertEquals(Optional.empty(), unix.env(Map.of("Path", "/bin"), "PATH"));
  }

  @Test
  void customPagerRunsThroughShAndShellExitCodesMeanCouldNotRun() {
    assertEquals(List.of("sh", "-c", "less -S"), unix.shell("less -S"));
    assertTrue(unix.couldNotRun(126));
    assertTrue(unix.couldNotRun(127));
    assertFalse(unix.couldNotRun(0));
    assertFalse(unix.couldNotRun(1));
    assertFalse(unix.couldNotRun(9009));
  }
}
