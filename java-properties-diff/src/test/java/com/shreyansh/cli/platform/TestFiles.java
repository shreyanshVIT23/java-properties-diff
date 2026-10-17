package com.shreyansh.cli.platform;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;

/**
 * File helpers shared by the pager and platform tests.
 */
public final class TestFiles {

  private TestFiles() {
  }

  /**
   * Writes a script and marks it executable. On file systems without POSIX permissions
   * (Windows), files are executable by extension, so only the content is written.
   *
   * @param file the file to create
   * @param script its content
   * @return the file
   * @throws IOException if the file cannot be written
   */
  public static Path executable(Path file, String script) throws IOException {
    Files.writeString(file, script);
    if (file.getFileSystem().supportedFileAttributeViews().contains("posix")) {
      Files.setPosixFilePermissions(file, PosixFilePermissions.fromString("rwxr-xr-x"));
    }
    return file;
  }
}
