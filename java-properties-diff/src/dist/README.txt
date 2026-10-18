propdiff @VERSION@
==================

Compare two Java .properties files key by key, in the style of delta: changed values with the
changed part highlighted, keys that exist on only one side, line numbers, and an optional
side-by-side view. A long diff opens in a pager.

This folder contains its own Java runtime. You do not need Java installed.


Run it
------
Linux:    propdiff/bin/propdiff OLD.properties NEW.properties
Windows:  propdiff\propdiff.exe OLD.properties NEW.properties

Keep the folder together: the program finds its runtime relative to its own location. To run it
as plain "propdiff", put a link to it in a folder on your PATH, for example on Linux:

    ln -s "$PWD/propdiff/bin/propdiff" ~/.local/bin/propdiff


Options
-------
  -s, --side-by-side        show the two files in two columns
      --show-unchanged      also list keys that are the same in both files
      --only=TYPE[,TYPE...] only list these: changed, only-in-left, only-in-right, unchanged
      --mode=MODE           parser: custom (default, reports line numbers) or default
                            (java.util.Properties, no line numbers)
      --color=WHEN          auto (default), always or never; NO_COLOR is respected
      --width=COLUMNS       output width (default: $COLUMNS, else 120)
      --no-pager            print directly instead of through a pager
  -h, --help                show all options
  -V, --version             show the version

Exit codes: 0 = no differences, 1 = differences found, 2 = error or invalid usage.
That makes it usable in scripts, for example:  propdiff --no-pager a.properties b.properties


Pager
-----
When the output goes to a terminal, propdiff pages it with, in this order:
  1. the program in $PAGER, if set
  2. less, if installed (started with LESS=FRX unless you set LESS yourself)
  3. its own built-in pager (Space/b to scroll, /text to search, q to quit)
Piped or redirected output is never paged and has no colors.


Check the download
------------------
The .sha256 file next to the zip holds its SHA-256 checksum.
Linux:    sha256sum -c propdiff-@VERSION@-linux-x64.zip.sha256
Windows:  Get-FileHash propdiff-@VERSION@-windows-x64.zip -Algorithm SHA256
          (compare with the value in the .sha256 file)


Update or remove
----------------
Delete this propdiff folder (and any link you made to it). To update, unzip the new release in
its place.
