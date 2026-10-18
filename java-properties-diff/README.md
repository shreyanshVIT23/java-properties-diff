# propdiff

Compare two Java `.properties` files **key by key**, in the style of
[delta](https://github.com/dandavison/delta). It's a semantic diff, not a line diff: reordering
keys, changing separators (`=`, `:`, space) or adding comments and blank lines is not a
difference, while a changed value is.

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

In a terminal the rows are colored and only the changed part of a value is emphasized
(`localhost` vs `db.example.com` above).

## Features
- **Diff types:** changed, only in left, only in right, unchanged (shown with `--show-unchanged`).
  If a key appears twice in one file, the last value wins, as in `java.util.Properties`.
- **Two parsers:**
  - `--mode custom` (the default): our own parser of the `Properties.load` format. It keeps file
    order and reports **line numbers**.
  - `--mode default`: `java.util.Properties`. No line numbers.
- **Views:**
  - unified (default)
  - side-by-side with `-s`, where long values wrap with `↵`
  - in both, rows follow file order and the line-number gutter widens for large files
- **Word-level highlighting** of the changed part of each value.
- **Paging:** `$PAGER`, then the system `less` (with `LESS=FRX`), then a built-in Java pager
  ([JLine](https://github.com/jline/jline3)), so it works on systems without `less`.
- **Colors:** on in a terminal, off when piped. `--color always|never|auto`; `NO_COLOR` is
  respected.
- **Exit codes:** 0 = no differences, 1 = differences, 2 = error or invalid usage.
- **Release zip:** has its own Java runtime, so no Java install is needed to run it.

## Usage
```
propdiff [-hsV] [--no-pager] [--show-unchanged] [--color=WHEN] [--mode=MODE]
         [--width=COLUMNS] [--only=TYPE[,TYPE...]]... LEFT RIGHT
```
Examples:
```
propdiff old.properties new.properties
propdiff -s --show-unchanged old.properties new.properties
propdiff --only changed,only-in-left --no-pager old.properties new.properties
propdiff --help
```
To install a release, unzip `propdiff-<version>-linux-x64.zip` and run `propdiff/bin/propdiff`.
`README.txt` inside the zip has the details.

## Building
Requirements: a JDK 17 or newer. Maven comes with the wrapper (`./mvnw`).

| Command | What it does |
|---|---|
| `./mvnw clean verify` | Checkstyle (Google style, warnings fail the build), compile with `-Xlint:all -Werror`, all tests, and the fat jar `target/java-properties-diff-<version>.jar` |
| `java -jar target/java-properties-diff-1.0-SNAPSHOT.jar a.properties b.properties` | Run without packaging (needs Java) |
| `./mvnw -Pdist -DskipTests clean verify` | Release zip `target/propdiff-<app.version>-<os>-x64.zip` and its `.sha256`, built with jpackage |
| `sh scripts/smoke-test.sh target/propdiff-1.0.0-linux-x64.zip` | Test the release zip as a user gets it (unzips, runs with no Java on `PATH`, checks exit codes, output, stderr, file modes and the README) |

Always build the release with `clean`. Without it, shade merges the libraries into an
already shaded jar again and warns about overlapping classes.

### The JDK used for packaging
jpackage launchers built with some distro JDKs (dynamically linked `libstdc++`, for example
OpenJDK 21 on Arch-based systems) print `pure virtual method called` on every run
([JDK-8312488](https://bugs.openjdk.org/browse/JDK-8312488)). Package with a Temurin JDK
instead. The build uses `.jdks/temurin-17` automatically when it exists (the `local-temurin`
profile), or you can pass `-Djpackage.jdk=/path/to/jdk`. To fetch Temurin into the project
(git-ignored):
```
mkdir -p .jdks/temurin-17
curl -fL -o .jdks/temurin-17.tar.gz https://api.adoptium.net/v3/binary/latest/17/ga/linux/x64/jdk/hotspot/normal/eclipse
tar -xzf .jdks/temurin-17.tar.gz -C .jdks/temurin-17 --strip-components=1
```

## Continuous integration (local Jenkins)
`jenkins/` runs Jenkins in Docker, configured entirely as code (`jenkins/casc.yaml`):
- **Controller:** `jenkins/jenkins:lts-jdk21` at http://localhost:8080 (this machine only),
  user `admin`.
- **Agent:** label `linux`, reached over SSH, with Temurin 17 for the build.
- **Job:** a Multibranch Pipeline on this repository (mounted read-only).
  - every branch is built: tests, release zip, smoke test, archived zip + `.sha256`
  - every `v*` tag (e.g. `v1.2.3`) becomes a **kept release build** with version `1.2.3`
  - the Windows build is defined but off (`BUILD_WINDOWS`) until a Windows agent exists

One-time setup: create an SSH key pair in `jenkins/.secrets/` and a `jenkins/.env` with
`JENKINS_ADMIN_PASSWORD` and `AGENT_SSH_PUBKEY`. Both are git-ignored. Then:
```
docker compose -f jenkins/docker-compose.yml up -d --build
```
Jenkins scans for new commits and tags every 2 minutes. For instant builds, install the git
hook `scripts/git-hooks/reference-transaction`, which asks Jenkins to scan as soon as a branch
or tag changes:
- put a Jenkins API token in `jenkins/.secrets/api-token` as `user = "admin:<token>"`
- from the git root, run
  `ln -s ../../java-properties-diff/scripts/git-hooks/reference-transaction .git/hooks/`
- its own test: `sh scripts/git-hooks/test-reference-transaction.sh`

Release:
```
git tag -a v1.2.3 -m "propdiff 1.2.3"
```
The build appears under **propdiff → Tags → v1.2.3**.

## Project layout
```
src/main/java/com/shreyansh/
  App.java                 entry point (runs DiffCommand)
  core/parser/             KeyValue, Parser, DefaultParser (java.util.Properties),
                           CustomParser (own parser, line numbers), ParserMode
  core/diff/               PropertiesDiff, DiffResult, DiffEntry, DiffType, WordDiff
  cli/                     DiffCommand (picocli), UnifiedRenderer, SideBySideRenderer,
                           Decorations, Style, Terminal, Pager, JlineLess (built-in pager)
  cli/platform/            Platform, UnixPlatform, WindowsPlatform (PATH, shell, env rules)
src/test/                  JUnit 5 tests and .properties fixtures
src/dist/README.txt        README shipped inside the release zip
src/jpackage/*.args        OS-specific jpackage options (e.g. --win-console)
scripts/                   smoke-test.sh, git hooks
jenkins/                   Docker Compose, Dockerfiles, plugins, Configuration as Code
Jenkinsfile                the CI pipeline
```
`core` contains no display code; it can be reused by other front ends (a GUI or git
integration). OS-specific behavior is confined to `cli/platform`.
