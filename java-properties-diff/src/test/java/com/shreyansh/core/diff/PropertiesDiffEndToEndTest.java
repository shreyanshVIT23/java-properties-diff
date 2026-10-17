package com.shreyansh.core.diff;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.shreyansh.core.parser.ParserMode;
import com.shreyansh.core.parser.PropertiesReadException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * End-to-end tests: real files on disk, parsed in every {@link ParserMode}, then diffed.
 */
public class PropertiesDiffEndToEndTest {

  private final PropertiesDiff diff = new PropertiesDiff();

  @TempDir
  Path tempDir;

  @ParameterizedTest
  @EnumSource(ParserMode.class)
  void reportsEveryDiffTypeForFixturePair(ParserMode mode) throws Exception {
    DiffResult result = diff.compare(
        resource("/fixtures/left.properties"), resource("/fixtures/right.properties"), mode);

    assertEquals(
        List.of(
            new DiffEntry("app.name", "demo", "demo", DiffType.UNCHANGED),
            new DiffEntry("app.version", "1.0", "1.1", DiffType.CHANGED),
            new DiffEntry("db.url", "jdbc:postgresql://localhost:5432/demo",
                "jdbc:postgresql://db.example.com:5432/demo", DiffType.CHANGED),
            new DiffEntry("db.user", "admin", "admin", DiffType.UNCHANGED),
            new DiffEntry("empty", null, "", DiffType.ONLY_IN_RIGHT),
            new DiffEntry("feature.legacy", "true", null, DiffType.ONLY_IN_LEFT),
            new DiffEntry("feature.new", null, "enabled", DiffType.ONLY_IN_RIGHT),
            new DiffEntry("greeting", "hello world", "hello world", DiffType.UNCHANGED),
            new DiffEntry("timeout", "45", "45", DiffType.UNCHANGED)),
        result.entries());
    assertTrue(result.hasDifferences());
  }

  @ParameterizedTest
  @EnumSource(ParserMode.class)
  void fileComparedWithItselfHasNoDifferences(ParserMode mode) throws Exception {
    Path file = resource("/fixtures/spec-edge-cases.properties");

    DiffResult result = diff.compare(file, file, mode);

    assertFalse(result.entries().isEmpty());
    assertFalse(result.hasDifferences());
  }

  @ParameterizedTest
  @EnumSource(ParserMode.class)
  void formattingOnlyChangesAreUnchanged(ParserMode mode) throws Exception {
    Path left = Files.writeString(tempDir.resolve("left.properties"), "a=1\nb : 2\nc=x\\\n  y\n");
    Path right = Files.writeString(
        tempDir.resolve("right.properties"), "# reordered\nc xy\nb=2\n   a   =   1\n");

    assertFalse(diff.compare(left, right, mode).hasDifferences());
  }

  @ParameterizedTest
  @EnumSource(ParserMode.class)
  void missingFileOnEitherSideThrows(ParserMode mode) throws Exception {
    Path existing = resource("/fixtures/right.properties");
    Path missing = tempDir.resolve("missing.properties");

    assertThrows(PropertiesReadException.class, () -> diff.compare(missing, existing, mode));
    assertThrows(PropertiesReadException.class, () -> diff.compare(existing, missing, mode));
  }

  private static Path resource(String name) throws Exception {
    return Path.of(PropertiesDiffEndToEndTest.class.getResource(name).toURI());
  }
}
