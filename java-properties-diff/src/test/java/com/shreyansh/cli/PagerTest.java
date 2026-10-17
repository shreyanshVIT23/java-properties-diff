package com.shreyansh.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.shreyansh.cli.platform.Platform;
import com.shreyansh.cli.platform.TestFiles;
import com.shreyansh.cli.platform.UnixPlatform;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

/**
 * Tests for {@link Pager}. Real pager processes are started, with commands that write to a temp
 * file instead of a screen. OS-specific rules (PATH format, shells, exit codes) are tested in
 * the {@code platform} package.
 */
@DisabledOnOs(OS.WINDOWS)
public class PagerTest {

  private static final String TEXT = "Δ left ⟶ right\nchanged (1)\n";

  /** This class is disabled on Windows, so the Unix rules always apply. */
  private static final Platform UNIX = new UnixPlatform();

  @TempDir
  Path tempDir;

  @Test
  void notInteractivePrintsDirectly() {
    StringWriter out = new StringWriter();

    new Pager(new Terminal(false, Map.of("PAGER", "false")))
        .show(TEXT, new PrintWriter(out), true);

    assertEquals(TEXT, out.toString());
  }

  @Test
  void disabledPagerPrintsDirectly() {
    StringWriter out = new StringWriter();

    new Pager(new Terminal(true, Map.of("PAGER", "false")))
        .show(TEXT, new PrintWriter(out), false);

    assertEquals(TEXT, out.toString());
  }

  @Test
  void pagerCommandReceivesTheText() throws Exception {
    Path paged = tempDir.resolve("paged.txt");
    StringWriter out = new StringWriter();

    new Pager(new Terminal(true, Map.of("PAGER", "cat > '" + paged + "'")))
        .show(TEXT, new PrintWriter(out), true);

    assertEquals(TEXT, Files.readString(paged));
    assertEquals("", out.toString());
  }

  @Test
  void missingPagerCommandFallsBackToDirectOutput() {
    StringWriter out = new StringWriter();

    new Pager(new Terminal(true, Map.of("PAGER", "propdiff-no-such-pager-command")))
        .show(TEXT, new PrintWriter(out), true);

    assertEquals(TEXT, out.toString());
  }

  @Test
  void lessOptionsDefaultToFrxButUserValueWins() throws Exception {
    Path defaulted = tempDir.resolve("default.txt");
    String recordLess = "cat > /dev/null; printf %s \"$LESS\" > ";

    new Pager(new Terminal(true, Map.of("PAGER", recordLess + "'" + defaulted + "'")))
        .show(TEXT, new PrintWriter(new StringWriter()), true);
    assertEquals("FRX", Files.readString(defaulted));

    Path custom = tempDir.resolve("custom.txt");
    new Pager(new Terminal(true, Map.of("PAGER", recordLess + "'" + custom + "'", "LESS", "-S")))
        .show(TEXT, new PrintWriter(new StringWriter()), true);
    assertEquals("-S", Files.readString(custom));
  }

  @Test
  void withoutPagerAndWithoutLessTheBuiltinPagerIsUsed() throws Exception {
    Path emptyBin = Files.createDirectory(tempDir.resolve("empty-bin"));
    AtomicReference<String> builtin = new AtomicReference<>();
    StringWriter out = new StringWriter();

    // No PAGER and no less on PATH: no process starts, the Java pager shows the text
    new Pager(new Terminal(true, Map.of("PATH", emptyBin.toString())), UNIX, builtin::set)
        .show(TEXT, new PrintWriter(out), true);

    assertEquals(TEXT, builtin.get());
    assertEquals("", out.toString());
  }

  @Test
  void customPagerAndSystemLessWinOverTheBuiltinPager() throws Exception {
    AtomicReference<String> builtin = new AtomicReference<>();
    Path bin = Files.createDirectory(tempDir.resolve("bin"));
    TestFiles.executable(bin.resolve("less"), "#!/bin/sh\ncat > /dev/null\n");

    new Pager(new Terminal(true, Map.of("PAGER", "cat > /dev/null")), UNIX, builtin::set)
        .show(TEXT, new PrintWriter(new StringWriter()), true);
    new Pager(new Terminal(true, Map.of("PATH", bin.toString())), UNIX, builtin::set)
        .show(TEXT, new PrintWriter(new StringWriter()), true);
    // A PAGER the user chose that cannot run prints directly; it never switches pagers
    StringWriter out = new StringWriter();
    new Pager(new Terminal(true, Map.of("PAGER", "propdiff-no-such-pager-command")), UNIX,
        builtin::set).show(TEXT, new PrintWriter(out), true);

    assertNull(builtin.get());
    assertEquals(TEXT, out.toString());
  }

  @Test
  void builtinPagerIsNeverUsedWhenNotInteractiveOrDisabled() throws Exception {
    Path emptyBin = Files.createDirectory(tempDir.resolve("empty-bin"));
    AtomicReference<String> builtin = new AtomicReference<>();
    StringWriter piped = new StringWriter();
    StringWriter disabled = new StringWriter();
    Map<String, String> noLess = Map.of("PATH", emptyBin.toString());

    new Pager(new Terminal(false, noLess), UNIX, builtin::set)
        .show(TEXT, new PrintWriter(piped), true);
    new Pager(new Terminal(true, noLess), UNIX, builtin::set)
        .show(TEXT, new PrintWriter(disabled), false);

    assertNull(builtin.get());
    assertEquals(TEXT, piped.toString());
    assertEquals(TEXT, disabled.toString());
  }

  @Test
  void failingBuiltinPagerFallsBackToDirectOutput() throws Exception {
    Path emptyBin = Files.createDirectory(tempDir.resolve("empty-bin"));
    StringWriter out = new StringWriter();

    new Pager(new Terminal(true, Map.of("PATH", emptyBin.toString())), UNIX, text -> {
      throw new IOException("no usable terminal");
    }).show(TEXT, new PrintWriter(out), true);

    assertEquals(TEXT, out.toString());
  }

  @Test
  void withoutPagerTheLessFoundOnPathIsRun() throws Exception {
    Path bin = Files.createDirectory(tempDir.resolve("bin"));
    Path paged = tempDir.resolve("paged.txt");
    // A fake less that records what it is given and the LESS it sees
    TestFiles.executable(bin.resolve("less"),
        "#!/bin/sh\ncat > '" + paged + "'\nprintf %s \"$LESS\" > '" + paged + ".less'\n");
    StringWriter out = new StringWriter();

    new Pager(new Terminal(true, Map.of("PATH", bin.toString())))
        .show(TEXT, new PrintWriter(out), true);

    assertEquals(TEXT, Files.readString(paged));
    assertEquals("FRX", Files.readString(tempDir.resolve("paged.txt.less")));
    assertEquals("", out.toString());
  }
}
