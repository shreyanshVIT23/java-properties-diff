package com.shreyansh.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link Style}, the ANSI SGR colors used by the renderers.
 */
public class StyleTest {

  @Test
  void paintWrapsTextInSgrCodesWhenEnabled() {
    assertEquals("\u001b[48;5;52mold\u001b[0m", Style.MINUS.paint("old", true));
    assertEquals("\u001b[48;5;28mnew\u001b[0m", Style.PLUS_EMPH.paint("new", true));
  }

  @Test
  void paintLeavesTextUnchangedWhenDisabledOrEmpty() {
    assertEquals("old", Style.MINUS.paint("old", false));
    assertEquals("", Style.MINUS.paint("", true));
  }

  @Test
  void visibleShowsControlCharactersAsEscapes() {
    assertEquals("a\\tb\\nc\\rd\\fe", Style.visible("a\tb\nc\rd\fe"));
    assertEquals("plain text = ok", Style.visible("plain text = ok"));
  }
}
