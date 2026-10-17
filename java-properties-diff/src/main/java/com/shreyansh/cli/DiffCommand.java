package com.shreyansh.cli;

import com.shreyansh.core.diff.DiffResult;
import com.shreyansh.core.diff.DiffType;
import com.shreyansh.core.diff.PropertiesDiff;
import com.shreyansh.core.parser.KeyValue;
import com.shreyansh.core.parser.LineAwareParser;
import com.shreyansh.core.parser.NumberedKeyValue;
import com.shreyansh.core.parser.Parser;
import com.shreyansh.core.parser.ParserMode;
import com.shreyansh.core.parser.PropertiesReadException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Spec;

/**
 * The {@code propdiff} command: compares two properties files and prints a delta-style diff.
 *
 * <p>Exit codes follow diff: 0 when there are no differences, 1 when there are, 2 on any error.
 */
@Command(name = "propdiff", mixinStandardHelpOptions = true, version = "propdiff 1.0-SNAPSHOT",
    sortOptions = false, showDefaultValues = true, exitCodeOnExecutionException = 2,
    description = "Compare two .properties files key by key, in the style of delta.",
    exitCodeListHeading = "%nExit codes:%n",
    exitCodeList = {"0:no differences", "1:differences found", "2:error or invalid usage"})
public class DiffCommand implements Callable<Integer> {

  @Parameters(index = "0", paramLabel = "LEFT", description = "The original file.")
  Path left;

  @Parameters(index = "1", paramLabel = "RIGHT", description = "The file to compare with.")
  Path right;

  @Option(names = "--mode", defaultValue = "custom", paramLabel = "MODE",
      description = "Parser: ${COMPLETION-CANDIDATES}. Only custom reports line numbers.")
  ParserMode mode;

  @Option(names = {"-s", "--side-by-side"}, description = "Show the two files in two columns.")
  boolean sideBySide;

  @Option(names = "--show-unchanged", description = "Also list keys that are the same.")
  boolean showUnchanged;

  @Option(names = "--only", split = ",", paramLabel = "TYPE",
      showDefaultValue = CommandLine.Help.Visibility.NEVER,
      description = "Only list these types: changed, only-in-left, only-in-right, unchanged.")
  Set<DiffType> only = new LinkedHashSet<>();

  @Option(names = "--color", defaultValue = "auto", paramLabel = "WHEN",
      description = "When to use colors: ${COMPLETION-CANDIDATES}.")
  ColorMode color;

  @Option(names = "--width", paramLabel = "COLUMNS",
      description = "Output width (default: $COLUMNS, else " + Terminal.DEFAULT_WIDTH + ").")
  Integer width;

  @Option(names = "--no-pager",
      description = "Print directly instead of through $PAGER (default: less).")
  boolean noPager;

  @Spec
  CommandSpec spec;

  private final Terminal terminal;

  DiffCommand(Terminal terminal) {
    this.terminal = terminal;
  }

  /**
   * Builds the configured command line: case-insensitive enum values and kebab-case diff types.
   * Exit code 2 for unexpected errors is set on the annotation (exitCodeOnExecutionException).
   *
   * @param terminal where the output goes; decides color and width
   * @return a ready-to-execute command line
   */
  public static CommandLine commandLine(Terminal terminal) {
    CommandLine commandLine = new CommandLine(new DiffCommand(terminal));
    commandLine.setCaseInsensitiveEnumValuesAllowed(true);
    // "only-in-left" -> ONLY_IN_LEFT
    commandLine.registerConverter(DiffType.class, value -> {
      try {
        return DiffType.valueOf(value.trim().replace('-', '_').toUpperCase(Locale.ROOT));
      } catch (IllegalArgumentException e) {
        throw new CommandLine.TypeConversionException("'" + value
            + "' is not one of: changed, only-in-left, only-in-right, unchanged");
      }
    });
    return commandLine;
  }

  @Override
  public Integer call() {
    Parser parser = mode.create();
    Side leftSide;
    Side rightSide;
    try {
      leftSide = read(parser, left);
      rightSide = read(parser, right);
    } catch (PropertiesReadException e) {
      spec.commandLine().getErr()
          .println("propdiff: " + e.getMessage() + " (" + e.getCause() + ")");
      return 2;
    }

    DiffResult result = new PropertiesDiff().compare(leftSide.pairs(), rightSide.pairs());
    RenderOptions options = new RenderOptions(left.toString(), right.toString(),
        terminal.width(width), terminal.useColor(color), showUnchanged, only,
        leftSide.lines(), rightSide.lines());
    String text = sideBySide
        ? new SideBySideRenderer().render(result, options)
        : new UnifiedRenderer().render(result, options);

    new Pager(terminal).show(text, spec.commandLine().getOut(), !noPager);
    return result.hasDifferences() ? 1 : 0;
  }

  /** Parses one file; line numbers are only known when the parser is line aware. */
  private static Side read(Parser parser, Path path) throws PropertiesReadException {
    if (!(parser instanceof LineAwareParser lineAware)) {
      return new Side(parser.parseFile(path), Map.of());
    }
    List<NumberedKeyValue> numbered = lineAware.parseFileWithLines(path);
    Map<String, Integer> lines = new HashMap<>();
    for (NumberedKeyValue entry : numbered) {
      // Last occurrence wins, matching the value PropertiesDiff compares
      lines.put(entry.pair().key(), entry.line());
    }
    return new Side(numbered.stream().map(NumberedKeyValue::pair).toList(), lines);
  }

  /** The pairs of one file and the line of each key (empty when unknown). */
  private record Side(List<KeyValue> pairs, Map<String, Integer> lines) {
  }
}
