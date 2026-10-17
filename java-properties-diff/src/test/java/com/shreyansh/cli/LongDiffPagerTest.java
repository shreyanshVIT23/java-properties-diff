package com.shreyansh.cli;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.function.IntFunction;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;
import picocli.CommandLine;

/**
 * Stress tests for the pager with output far larger than a pipe buffer (64 KiB on Linux). The
 * timeout turns a pipe deadlock into a failure instead of a hung build.
 */
@DisabledOnOs(OS.WINDOWS)
@Timeout(30)
public class LongDiffPagerTest {

  /** Keys per generated file; every third key changes, so 6666 changed and 13334 unchanged. */
  private static final int KEYS = 20_000;

  @TempDir
  Path tempDir;

  @Test
  void pagerReceivesEveryLineOfHugeOutput() throws Exception {
    // About 2.5 MB: roughly 40 times a pipe buffer
    String text = IntStream.rangeClosed(1, 200_000)
        .mapToObj(i -> "line " + i + "\n")
        .collect(Collectors.joining());
    Path paged = tempDir.resolve("paged.txt");
    StringWriter out = new StringWriter();

    new Pager(new Terminal(true, Map.of("PAGER", "cat > '" + paged + "'")))
        .show(text, new PrintWriter(out), true);

    assertEquals(text, Files.readString(paged));
    assertEquals("", out.toString());
  }

  @Test
  void quittingThePagerEarlyIsNotAnErrorAndDoesNotReprint() throws Exception {
    String text = IntStream.rangeClosed(1, 200_000)
        .mapToObj(i -> "line " + i + "\n")
        .collect(Collectors.joining());
    Path paged = tempDir.resolve("paged.txt");
    StringWriter out = new StringWriter();
    // head exits after 5 lines, like pressing q in less; the rest of the write hits a broken pipe
    Pager pager = new Pager(new Terminal(true, Map.of("PAGER", "head -n 5 > '" + paged + "'")));

    assertDoesNotThrow(() -> pager.show(text, new PrintWriter(out), true));

    assertEquals(List.of("line 1", "line 2", "line 3", "line 4", "line 5"),
        Files.readAllLines(paged));
    assertEquals("", out.toString());
  }

  @Test
  void longDiffFromTheCommandGoesThroughThePagerInFull() throws Exception {
    Path paged = tempDir.resolve("paged.txt");
    // Interactive, so the pager runs; TERM=dumb keeps --color auto from adding escape codes
    Terminal tty = new Terminal(true, Map.of("PAGER", "cat > '" + paged + "'", "TERM", "dumb"));
    StringWriter out = new StringWriter();
    CommandLine commandLine = DiffCommand.commandLine(tty);
    commandLine.setOut(new PrintWriter(out, true));
    Path left = write("left.properties", i -> "key." + i + "=value-" + i);
    Path right = write("right.properties",
        i -> "key." + i + (i % 3 == 0 ? "=changed-" : "=value-") + i);

    assertEquals(1, commandLine.execute(left.toString(), right.toString()));

    String output = Files.readString(paged);
    int changed = KEYS / 3;
    assertTrue(output.startsWith("Δ " + left + " ⟶ " + right + "\n"));
    assertTrue(output.endsWith(
        "\n" + changed + " changed · " + (KEYS - changed) + " unchanged\n"));
    assertEquals(2L * changed,
        output.lines().filter(row -> row.contains("│ - key.") || row.contains("│ + key."))
            .count());
    assertEquals("", out.toString());
  }

  private Path write(String name, IntFunction<String> line) throws IOException {
    return Files.write(tempDir.resolve(name),
        IntStream.rangeClosed(1, KEYS).mapToObj(line).toList());
  }
}
