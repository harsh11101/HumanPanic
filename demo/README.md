# HumanPanic demo

Five small applications that use HumanPanic **as an external dependency**. They resolve
`com.github.harsh11101:HumanPanic:2.0.0` from Maven Local (or from JitPack once the tag is
released), exactly as an external project would, and use only the public API. This is a separate Gradle build. It does not reference the
library's source code.

| App | Shows | Dependencies |
|---|---|---|
| `cli-app` (`csvstat`) | Global handler, `wrap()` proxies, manual `handle()`, `exitCode`, configuration from `application.properties` and `-Dapp.*` | **HumanPanic only.** There is no Spring on the classpath, which proves the core has no dependencies. |
| `picocli-app` (`salesctl`) | [picocli](https://picocli.info): `handle()` from picocli's execution-exception handler, because picocli catches what commands throw | HumanPanic, picocli |
| `jcommander-app` (`salesavg`) | [JCommander](https://jcommander.org): `install()` alone is enough | HumanPanic, JCommander |
| `commons-cli-app` (`salesmax`) | [Apache Commons CLI](https://commons.apache.org/proper/commons-cli/): top-level try/catch with `handle()`, your own exit status, report directory from an option | HumanPanic, Commons CLI |
| `spring-app` (`notify`) | `@HumanPanic` on Spring beans behind JDK and CGLIB proxies, `silent` mode, `app.*` properties | HumanPanic, `spring-context`, `aspectjweaver` |

## Running

From the **repository root**:

```bash
./gradlew :lib:publishToMavenLocal     # 1. install the library into ~/.m2
./gradlew -p demo installDist          # 2. build all apps
```

On Windows, use `gradlew.bat`, and the `.bat` launchers in the same `bin/` folders.

### `csvstat` (plain Java CLI)

```bash
CSV=demo/cli-app/build/install/csvstat/bin/csvstat

$CSV stats  demo/data/sales.csv                     # works: prints statistics, exit 0
$CSV stats  demo/data/sales-corrupt.csv             # uncaught exception: friendly message + report, exit 1
$CSV export demo/data/sales.csv /no/such/dir/x.json # export fails through the proxy: short message, continues, exit 0
$CSV batch  demo/data/sales.csv demo/data/sales-corrupt.csv   # handle(): 1/2 processed, exit 0
$CSV import demo/data/sales-corrupt.csv             # @HumanPanic(exitCode = 2): report, then exit 2
```

Configuration:

```bash
# Override metadata and the report directory with system properties:
JAVA_OPTS="-Dapp.name=csvstat-nightly -Dcsvstat.reportDir=/tmp/csv-reports" $CSV stats demo/data/sales-corrupt.csv
```

Error-handling edge case: point `-Dcsvstat.reportDir` at a path under a regular *file*. The user is
told "the crash report could not be written", one WARNING line is logged, and the exit status is
still 1.

User messages go to **stderr**, normal output to stdout. Reports are written to `./crash-reports/`
by default. Open one to see the cause chain (`caused_by`), stack traces and system information.

### CLI frameworks

The same idea in three popular argument parsers. In each one a corrupt file is a bug (HumanPanic
handles it), and a usage error is the user's mistake (the framework reports it, exit 2, no crash
report).

```bash
CTL=demo/picocli-app/build/install/salesctl/bin/salesctl
AVG=demo/jcommander-app/build/install/salesavg/bin/salesavg
MAX=demo/commons-cli-app/build/install/salesmax/bin/salesmax

# picocli
$CTL total demo/data/sales.csv                  # total: 572.55, exit 0
$CTL top   demo/data/sales.csv -n 2             # two largest amounts, exit 0
$CTL total demo/data/sales-corrupt.csv          # friendly message + report, exit 1
$CTL total no-such-file.csv                     # picocli usage error, exit 2

# JCommander
$AVG demo/data/sales.csv                        # average amount: 143.14, exit 0
$AVG demo/data/sales-corrupt.csv                # friendly message + report, exit 1
$AVG -c price demo/data/sales.csv               # "No such column", exit 2

# Apache Commons CLI
$MAX demo/data/sales.csv                        # max: 310.00, exit 0
$MAX -r my-reports demo/data/sales-corrupt.csv  # report in ./my-reports, exit 70
$MAX -x                                         # usage error, exit 2
```

| Framework | Does it catch your exceptions? | Integration |
|---|---|---|
| picocli | **Yes.** `CommandLine.execute` catches them, so the uncaught-exception handler never runs. | `cli.setExecutionExceptionHandler((e, cmd, parsed) -> { panic.handle(e); return 1; })` |
| JCommander | No | `install()` |
| Commons CLI | No | `install()`, or a top-level try/catch with `handle()` if you want your own exit status |

### `notify` (Spring)

```bash
demo/spring-app/build/install/notify/bin/notify
```

It runs three calls: a successful one, a failing one (friendly message, `false` returned, the
service keeps running) and a silent one (report only, `0` returned). Exit status is 0.

## What to look at

* [`Main.java`](cli-app/src/main/java/com/example/csvstat/Main.java): one `PanicHandler` for the
  whole process.
* [`JsonFileExporter.java`](cli-app/src/main/java/com/example/csvstat/JsonFileExporter.java): the
  annotation on an implementation method.
* [`Importer.java`](cli-app/src/main/java/com/example/csvstat/Importer.java): the annotation on an
  interface method, with an exit code.
* [`SalesCtl.java`](picocli-app/src/main/java/com/example/salesctl/SalesCtl.java): picocli's
  execution-exception handler.
* [`SalesAvg.java`](jcommander-app/src/main/java/com/example/salesavg/SalesAvg.java): JCommander
  with only `install()`.
* [`SalesMax.java`](commons-cli-app/src/main/java/com/example/salesmax/SalesMax.java): Commons CLI
  with `handle()` and a custom exit status.
* [`EmailNotificationService.java`](spring-app/src/main/java/com/example/notify/EmailNotificationService.java):
  a Spring bean behind a JDK proxy.
