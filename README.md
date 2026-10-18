# propdiff: how the CLI works

`propdiff` compares two Java `.properties` files **key by key** and prints the differences in
the style of [delta](https://github.com/dandavison/delta). It is a semantic diff, not a line
diff: reordering keys, switching separators (`=`, `:`, space), line continuations, comments and
blank lines are not differences. A changed **value** is.

The project lives in [`java-properties-diff/`](java-properties-diff/README.md) (building, CI,
releases). This page is only about using the command.

## Running it

From a release zip (it bundles its own Java, nothing else to install):

| OS | Command |
|---|---|
| Windows | `propdiff\propdiff.exe LEFT RIGHT` |
| Linux | `propdiff/bin/propdiff LEFT RIGHT` |

In PowerShell, a quoted path needs the call operator: `& "C:\path with spaces\propdiff.exe" ...`.

From a source build (needs Java 17+): `java -jar java-properties-diff/target/java-properties-diff-1.0-SNAPSHOT.jar LEFT RIGHT`.

## Synopsis

```
propdiff [-hsV] [--no-pager] [--show-unchanged] [--color=WHEN] [--mode=MODE]
         [--width=COLUMNS] [--only=TYPE[,TYPE...]]... LEFT RIGHT
```

- `LEFT` is the original file, `RIGHT` the one compared with it. Swapping them swaps
  "only in left" and "only in right" and the `-`/`+` sides.
- Options can come before, between or after the two files.
- Long options take their value with `=` or a space: `--width=80` and `--width 80` are the same.
- Short flags can be grouped: `-hsV`.
- `--` ends the options, for a file whose name starts with `-`: `propdiff -- -old.properties new.properties`.

## Options

| Option | Values | Default | What it does |
|---|---|---|---|
| `-s`, `--side-by-side` | flag | off (unified) | Two columns, left file's value on the left; long values wrap onto extra rows marked `↵`. |
| `--show-unchanged` | flag | off | Also print the keys whose values are equal. |
| `--only=TYPE[,TYPE...]` | `changed`, `only-in-left`, `only-in-right`, `unchanged` | all except unchanged | Print only these sections. Comma-separated and/or repeated. |
| `--mode=MODE` | `custom`, `default` (any case) | `custom` | Which parser reads the files (see below). |
| `--color=WHEN` | `auto`, `always`, `never` (any case) | `auto` | When to color the output. |
| `--width=COLUMNS` | positive number | `$COLUMNS`, else 120 | Output width; never narrower than 20. |
| `--no-pager` | flag | pager on | Print directly instead of through a pager. |
| `-h`, `--help` | | | Print help and exit 0. |
| `-V`, `--version` | | | Print the version and exit 0. |

## What the output looks like

```
Δ left.properties ⟶ right.properties
────────────────────────────────────────

changed (2)
   3 ⋮     │ - app.version=1.0
     ⋮   4 │ + app.version=1.1
   4 ⋮     │ - db.url=jdbc:postgresql://localhost:5432/demo
     ⋮   5 │ + db.url=jdbc:postgresql://db.example.com:5432/demo

only in left (1)
   6 ⋮     │ - feature.legacy=true

only in right (2)
     ⋮   7 │ + feature.new=enabled
     ⋮  10 │ + empty=

2 changed · 1 only in left · 2 only in right · 4 unchanged
```

1. **Header:** `Δ LEFT ⟶ RIGHT` and a rule.
2. **Sections**, always in this order, each with its count, and only if non-empty:
   `changed`, `only in left`, `only in right`, `unchanged`.
3. **Rows:** a gutter `LEFT-LINE ⋮ RIGHT-LINE │`, then `-` (left value), `+` (right value) or two
   spaces (unchanged). A changed key has a `-` row and a `+` row. With color, only the part of
   the value that actually changed is emphasized (`localhost` vs `db.example.com`).
4. **Summary:** the non-zero counts of the **whole** comparison, or `no differences`.

Rules worth knowing:
- If a key appears twice in one file, the **last** value wins, as in `java.util.Properties`.
- Rows follow file order (by the line on the side they are shown on).
- The gutter is at least 4 digits wide and grows for longer files.
- File names are shown as you typed them; on Windows with `\` separators even if you typed `/`.
- If the filters leave nothing to show, only the header and the summary are printed.

The same comparison with `-s --width 90 --show-unchanged` (side by side has no `-`/`+`: the left
column is the left file. Each column has its own line-number gutter):

```
changed (2)
│   3 │ app.version=1.0                      │   4 │ app.version=1.1
│   4 │ db.url=jdbc:postgresql://localhost:↵ │   5 │ db.url=jdbc:postgresql://db.example↵
│     │ 5432/demo                            │     │ .com:5432/demo

only in left (1)
│   6 │ feature.legacy=true                  │     │

only in right (2)
│     │                                      │   7 │ feature.new=enabled
│     │                                      │  10 │ empty=

unchanged (4)
│   2 │ app.name=demo                        │   3 │ app.name=demo
│   5 │ db.user=admin                        │   6 │ db.user=admin
│   9 │ timeout=45                           │   8 │ timeout=45
│  10 │ greeting=hello world                 │   9 │ greeting=hello world

2 changed · 1 only in left · 2 only in right · 4 unchanged
```
Values are always printed as `key=value`, whatever separator the file used (`right.properties`
writes `app.name : demo`). `greeting` is unchanged although the left file splits it over two
lines with `\`; `timeout` is unchanged because the left file's duplicate `timeout=30` is
overridden by its later `timeout=45`.

## Parser modes

| | `--mode custom` (default) | `--mode default` |
|---|---|---|
| Parser | propdiff's own reader of the `Properties.load` format | `java.util.Properties` |
| Line numbers in the gutter | yes | no (gutter shows no numbers) |
| Row order | file order | by key |
| Values compared | the same in both modes | the same in both modes |

Use `default` as a cross-check that propdiff reads a file exactly as Java does.

## How the options combine

### What gets printed: `--only` × `--show-unchanged`

`unchanged` is hidden unless you ask for it, in either of two ways.

| `--only` | without `--show-unchanged` | with `--show-unchanged` |
|---|---|---|
| (not given) | changed, only in left, only in right | **all four** sections |
| `changed` | changed | changed |
| `only-in-left` | only in left | only in left |
| `only-in-right` | only in right | only in right |
| `unchanged` | unchanged | unchanged |
| `changed,only-in-left` | changed, only in left | changed, only in left |
| `only-in-left,only-in-right` | keys present in just one file | same |
| `changed,unchanged` | changed, unchanged | changed, unchanged |
| all four types | all four sections | all four sections |

So: `--only` picks the sections; `--show-unchanged` only matters when `--only` is **not** given,
and naming `unchanged` in `--only` shows it without `--show-unchanged`. These are equivalent:

```
propdiff --only changed,only-in-left a.properties b.properties
propdiff --only changed --only only-in-left a.properties b.properties
propdiff --only=CHANGED,Only-In-Left a.properties b.properties   # case does not matter
propdiff "--only=changed, only-in-left" a.properties b.properties # spaces after commas are fine
```

`--only` never changes the summary line or the exit code: they always describe the whole
comparison. For example `--only unchanged` still exits with 1 if anything differs.

### Layout × everything else

`-s` changes only the layout. Every other option works the same in both layouts:

| | unified (default) | `-s` side by side |
|---|---|---|
| `--only`, `--show-unchanged` | sections filtered | sections filtered |
| Row markers | `-` left value, `+` right value | none: left column = left file |
| `--mode` | line numbers in the gutter (custom) | line numbers per column (custom) |
| `--width` | width of the rule only; rows are never wrapped or cut | splits the width into two columns; long values wrap with `↵` |
| `--color` | `-`/`+` rows colored | both columns colored |

### Color: `--color` × where the output goes

| `--color` | Terminal | Piped / redirected to a file | `NO_COLOR` set (non-empty) | `TERM=dumb` |
|---|---|---|---|---|
| `auto` (default) | color | **no color** | no color | no color |
| `always` | color | color (escape codes in the file) | color | color |
| `never` | no color | no color | no color | no color |

`NO_COLOR` follows [no-color.org](https://no-color.org): it only affects `auto`.

### Pager: `--no-pager` × where the output goes

The pager runs only when **all** of these hold: output goes to a terminal, and `--no-pager` is
not given. Then the first available one is used:

1. `$PAGER`, if set and not blank, run through the shell (`sh -c` on Linux, `cmd /c` on
   Windows), so it may include arguments (e.g. `PAGER="less -S"`).
2. `less` from `PATH`.
3. A built-in pager (no `less` needed, e.g. on a plain Windows install).

`less` gets `LESS=FRX` unless you set `LESS` yourself: it quits at once when the diff fits on
one screen (`F`), keeps colors (`R`) and leaves the output on screen after quitting (`X`).
If a custom `$PAGER` cannot be started, propdiff prints directly instead.

| | Terminal | Piped / redirected |
|---|---|---|
| default | pager | printed directly |
| `--no-pager` | printed directly | printed directly |

### Width: `--width` × `COLUMNS`

| `--width` | `COLUMNS` | Width used |
|---|---|---|
| given, > 0 | anything | `--width` (at least 20) |
| not given | set to a number > 0 | `COLUMNS` (at least 20) |
| not given | unset or not a number | 120 |
| `0` or negative | anything | treated as not given |
| not a number (`--width abc`) | anything | exit 2: `'abc' is not an int` |

## Exit codes

| Code | Meaning |
|---|---|
| `0` | no differences (also for `--help`, `--version`) |
| `1` | differences found |
| `2` | error: a file is missing or unreadable, or invalid usage (unknown option, bad `--mode`, `--color` or `--only` value, wrong number of files) |

The diff goes to stdout. Errors go to stderr: a file problem as `propdiff: <reason>`, a usage
error as the reason followed by the usage text. Usage errors are colored by the option parser
(picocli) itself, even when redirected, and `--color` does not affect them; set `NO_COLOR=1` for
plain error text.

## Recipes

```
# Everyday: what changed between two configs
propdiff old.properties new.properties

# Two columns, including the keys that stayed the same
propdiff -s --show-unchanged old.properties new.properties

# Only the keys that exist in just one file
propdiff --only only-in-left,only-in-right old.properties new.properties

# Just the changed values, narrow, side by side
propdiff -s --only changed --width 100 old.properties new.properties

# Cross-check with java.util.Properties (no line numbers)
propdiff --mode default old.properties new.properties

# Save a plain report (no color, no pager)
propdiff --no-pager --color never old.properties new.properties > report.txt

# Keep color when piping into less yourself
propdiff --color always old.properties new.properties | less -R
```

In scripts, use the exit code:

```sh
# Linux / Git Bash
if propdiff --no-pager --color never expected.properties actual.properties > diff.txt; then
  echo "same"
elif [ $? -eq 1 ]; then
  echo "different"; cat diff.txt
else
  echo "propdiff failed"; exit 2
fi
```

```powershell
# PowerShell
propdiff --no-pager --color never expected.properties actual.properties > diff.txt
switch ($LASTEXITCODE) { 0 { 'same' } 1 { 'different'; Get-Content diff.txt } default { 'propdiff failed' } }
```

## Invalid combinations

All exit with 2 and print nothing on stdout. The messages are the real ones:

| Command | stderr (first line) |
|---|---|
| `propdiff a.properties` | `Missing required parameter: 'RIGHT'` |
| `propdiff a b c` | `Unmatched argument at index 2: 'c'` |
| `propdiff --mode xml a b` | `Invalid value for option '--mode': expected one of [DEFAULT, CUSTOM] (case-insensitive) but was 'xml'` |
| `propdiff --color rainbow a b` | `Invalid value for option '--color': expected one of [AUTO, ALWAYS, NEVER] (case-insensitive) but was 'rainbow'` |
| `propdiff --only nope a b` | `Invalid value for option '--only' (TYPE): 'nope' is not one of: changed, only-in-left, only-in-right, unchanged` |
| `propdiff --width abc a b` | `Invalid value for option '--width': 'abc' is not an int` |
| `propdiff --bogus a b` | `Unknown option: '--bogus'` |
| `propdiff -old.properties new.properties` | a usage error: a name starting with `-` needs `--` before it |
| `propdiff a missing.properties` | `propdiff: Couldn't read properties file: missing.properties (java.nio.file.NoSuchFileException: missing.properties)` |

## Checked against the release binary
Every table and example above was run against the Windows release build
(`propdiff.exe`, 2026-10-18), using the fixtures in `java-properties-diff/src/test/resources/fixtures`.
The interactive pager could not be exercised there (no terminal); its rules come from the code
and the unit tests.
