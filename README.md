# HumanPanic

[![CI](https://github.com/harsh11101/HumanPanic/actions/workflows/ci.yml/badge.svg)](https://github.com/harsh11101/HumanPanic/actions/workflows/ci.yml)
[![](https://jitpack.io/v/harsh11101/HumanPanic.svg)](https://jitpack.io/#harsh11101/HumanPanic)

**Human-friendly crash handling for Java applications**, inspired by Rust's
[`human-panic`](https://github.com/rust-cli/human-panic).

When your CLI tool or service hits an unexpected exception, the user sees a short, polite message
instead of a 60-line stack trace. A structured JSON crash report is written for you to debug from.

```text
Well, this is embarrassing.

csvstat had a problem and crashed. To help us diagnose
the problem you can send us a crash report.

We have generated a report file at:

  /home/ada/crash-reports/crash-2026-11-09-23-35-04-569-c7b4f1.json

Submit an issue or email with the subject of:

  csvstat Crash Report

- Open an issue at:
    https://github.com/example/csvstat/issues

We take privacy seriously, and do not perform any automated error collection.
In order to improve the software, we rely on people to submit reports.

Thank you kindly!
```

## Why use it

* **Better UX for end users.** They get an explanation and a next step, not a stack trace.
* **Better bug reports for you.** Each report has the exception, the full **cause chain**, stack
  traces, JVM/OS information and your app's version.
* **Private by design.** Nothing is sent anywhere. The user decides whether to share the file.
* **Zero runtime dependencies.** The core is plain Java 17. Spring support is optional, and only
  activates if your application already uses Spring.
* **Safe to run while things are failing.** Handling a crash never throws an exception of its own.
  If a report can't be written, the user is told so, and the original failure keeps its normal
  handling.

## How it works

There are three ways to use HumanPanic, and you can combine them:

| Integration | For | How |
|---|---|---|
| **Global handler** | CLI apps: anything uncaught, including a crash in `main` | `PanicHandler.builder()...build().install()` |
| **`@HumanPanic` on interface methods** | Plain Java code at a "report and continue" boundary | `handler.wrap(impl, MyInterface.class)` |
| **`@HumanPanic` on Spring beans** | Spring applications | `@Import(HumanPanicConfiguration.class)` |

Each one writes a crash report (JSON) and a user message (to `System.err` by default).

## Installation

Requires **Java 17+**.

HumanPanic is published through [JitPack](https://jitpack.io/#harsh11101/HumanPanic).

**Gradle**

```groovy
repositories {
    mavenCentral()
    maven { url 'https://jitpack.io' }
}

dependencies {
    implementation 'com.github.harsh11101:HumanPanic:2.0.0'
}
```

**Maven**

```xml
<repositories>
  <repository>
    <id>jitpack.io</id>
    <url>https://jitpack.io</url>
  </repository>
</repositories>

<dependency>
  <groupId>com.github.harsh11101</groupId>
  <artifactId>HumanPanic</artifactId>
  <version>2.0.0</version>
</dependency>
```

For the Spring integration, your application must also have `org.springframework:spring-context`
(6.x) and `org.aspectj:aspectjweaver` on its classpath. Spring Boot's `spring-boot-starter-aop`
provides both. HumanPanic does not pull them in.

## Quick start (CLI)

```java
import io.pants.humanpanic.PanicHandler;
import io.pants.humanpanic.config.AppMetadata;

public class Main {
    public static void main(String[] args) {
        AppMetadata app = new AppMetadata();
        app.setName("csvstat");
        app.setVersion("1.4.0");
        app.setIssueUrl("https://github.com/example/csvstat/issues");

        PanicHandler.builder().metadata(app).build().install();

        runTheTool(args);   // an uncaught exception now gets a friendly message and a crash report
    }
}
```

The process still ends with **exit status 1** after an uncaught exception in `main`, exactly as it
would without HumanPanic. Scripts calling your tool can still detect the failure.

## Usage

### `@HumanPanic` on plain Java interfaces

```java
public interface Exporter {
    boolean export(Stats stats, Path target) throws IOException;
}

class JsonExporter implements Exporter {
    @HumanPanic(createCrashReport = false,
                message = "Could not export the summary; the statistics above are still valid.")
    @Override
    public boolean export(Stats stats, Path target) throws IOException { ... }
}

Exporter exporter = handler.wrap(new JsonExporter(), Exporter.class);
boolean ok = exporter.export(stats, out);   // on failure: message shown, returns false
```

The annotation can go on the implementation method or on the interface method. Methods without it
pass through untouched, exceptions included.

### `@HumanPanic` on Spring beans

```java
@Configuration
@Import(HumanPanicConfiguration.class)
@PropertySource("classpath:application.properties")   // or Spring Boot's application.properties
class AppConfig { }

@Service
class EmailNotificationService implements NotificationService {
    @HumanPanic(message = "We could not send your email. Nothing was lost; please try again.")
    public boolean send(String to, String body) { ... }
}
```

This works with interface (JDK) proxies and class (CGLIB) proxies, in plain Spring and in Spring
Boot.

### With a CLI framework (picocli, JCommander, Commons CLI)

`install()` works with any argument parser that lets exceptions escape `main`, such as JCommander
and Apache Commons CLI. **picocli is different:** `CommandLine.execute` catches what your commands
throw, so pass the failure to HumanPanic from picocli's own hook:

```java
PanicHandler panic = PanicHandler.builder().metadata(app).build().install();

CommandLine cli = new CommandLine(new MyCommand());
cli.setExecutionExceptionHandler((failure, commandLine, parseResult) -> {
    panic.handle(failure);
    return commandLine.getCommandSpec().exitCodeOnExecutionException();
});
System.exit(cli.execute(args));
```

Usage errors (unknown options, missing arguments) stay with the framework. The
[demo](demo/README.md#cli-frameworks) has a runnable example for each of the three.

### Handling a failure yourself

```java
try {
    process(file);
} catch (Exception e) {
    Optional<Path> report = handler.handle(e);   // message + report; never throws
}
```

## The `@HumanPanic` annotation

| Attribute | Default | Effect |
|---|---|---|
| `message` | `""` | Extra sentence shown to the user. |
| `createCrashReport` | `true` | Write a JSON report. If `false`, the message names the exception instead. |
| `silent` | `false` | No user message. A report is still written if enabled. |
| `printStackTrace` | `false` | Also print the raw stack trace to the message stream (useful in development). |
| `exitCode` | `0` | If not `0`, call `System.exit(exitCode)` after reporting. |

> **The exception is swallowed.** After handling, an intercepted method returns a default value
> unless `exitCode` ends the process: `0` or `false` for primitives, an empty value for `Optional`,
> `List`, `Collection`, `Iterable`, `Set` and `Map`, and `null` for everything else. Use it only at boundaries
> where "report and continue" or "report and exit" is really what you want. Two exceptions:
> a `VirtualMachineError` (`OutOfMemoryError`, `StackOverflowError`, ...) is reported and then
> **rethrown**, and an `InterruptedException` has the thread's interrupt flag restored.

## Configuration reference

### Application metadata (`AppMetadata`)

| Field | Property key | Default | Shown as |
|---|---|---|---|
| name | `app.name` | `Unknown Application` | "*name* had a problem and crashed" |
| version | `app.version` | `Unknown Version` | report only |
| authors | `app.authors` (comma-separated) | none | "Contact the authors" |
| homepage | `app.homepage` | none | report only |
| supportUrl | `app.support-url` | none | "Get help at" |
| issueUrl | `app.issue-url` | none | "Open an issue at" |

Sections with no value are left out of the message.

The metadata can come from three places:

* **In code:** `new AppMetadata()` plus setters.
* **Without a framework:** `new NonSpringConfigLoader().load()` reads `application.properties` (from
  the classpath, otherwise the working directory, as UTF-8), then lets `-Dapp.*` system properties
  override it.
* **In Spring:** the `app.*` properties from the Spring `Environment`, bound automatically by
  `HumanPanicConfiguration`.

### `PanicHandler.builder()`

| Method | Default |
|---|---|
| `metadata(AppMetadata)` | default metadata (it is copied at `build()`) |
| `reportDirectory(Path)` | `./crash-reports` (created when the first report is written) |
| `output(PrintStream)` | `System.err` (looked up when each message is written) |

### Spring settings

| Property | Default | Effect |
|---|---|---|
| `humanpanic.report-dir` | `crash-reports` | Directory the crash reports are written to. |

Messages go to `System.err`.

## Crash reports

Each failure gets its own file, `crash-<yyyy-MM-dd-HH-mm-ss-SSS>-<random>.json`. Files are created
atomically, so concurrent failures never overwrite each other. The file contains:

| Field | Content |
|---|---|
| `name`, `version` | Your application's name and version. |
| `timestamp` | ISO-8601 time of the failure. |
| `thread` | Thread that handled it. |
| `operating_system` | OS name and version. |
| `cause` | The top-level exception. |
| `method` | The intercepted method, if any. |
| `backtrace` | Stack trace of the top-level exception. |
| `caused_by` | The cause chain, each entry with its own backtrace. |
| `system_info` | Java version and vendor, OS, architecture, memory, CPU count, uptime. |
| `application_info` | Your app metadata. |

The schema is modelled by `io.pants.humanpanic.model.CrashReport`. If you have Jackson, you can
read a report back with `objectMapper.readValue(file, CrashReport.class)`.

## Error handling and diagnostics

* If a report cannot be written (read-only directory, disk full, ...), the user is told so in the
  message, and HumanPanic logs one WARNING line through `System.Logger`. That is
  `java.util.logging` unless you bridge it. The full stack trace of that internal problem is
  logged at DEBUG level.
* Problems while handling a failure never replace the original exception.
* Configuration mistakes made in code (for example `null` builder arguments) fail fast with a
  `NullPointerException` or `IllegalArgumentException`.

## Limitations

* **Interception requires a proxy.** Spring AOP only intercepts calls made through the Spring
  proxy: not `this.method()` self-calls, and only public methods. `wrap()` only works with
  interfaces. Native AspectJ weaving (ajc or load-time weaving) is **not supported**.
* **Constructors.** `@HumanPanic` is allowed on constructors only for compatibility with 1.x. It has
  no effect there.
* **Swallowed exceptions.** After a handled failure, a method declared to return a concrete or
  specialised collection type (`ArrayList`, `SortedSet`, ...) or any other object gets `null`. Only
  `Optional`, `List`, `Collection`, `Iterable`, `Set` and `Map` get an empty value.
* **Exit codes.** `exitCode` calls `System.exit`. That runs shutdown hooks, and can deadlock if a
  shutdown hook waits for the thread that is exiting.
* **Report contents.** Reports contain exception messages and stack traces, which may include
  sensitive data. On Linux and macOS each report is readable by its owner only (`rw-------`). On
  Windows it inherits the permissions of the report directory.
* **Fixed message text.** The message is in English, and its wording cannot be changed yet.
* **Spring output.** The Spring integration always writes messages to `System.err`.

## Building and testing

```bash
./gradlew build                          # compile, run all tests, javadoc, sources jar
./gradlew :lib:publishToMavenLocal       # install 2.0.0 into ~/.m2 (used by the demo)
```

The test suite has unit tests, Spring and Spring Boot integration tests, and forked-JVM tests that
check real exit codes. It needs JDK 17 or newer.

## Demo

[`demo/`](demo/README.md) has five small applications that consume HumanPanic from Maven Local,
the way an external project would: a plain-Java CLI tool with no other dependencies, the same idea
with picocli, JCommander and Apache Commons CLI, and a Spring service.

```bash
./gradlew :lib:publishToMavenLocal
./gradlew -p demo installDist
demo/cli-app/build/install/csvstat/bin/csvstat stats demo/data/sales-corrupt.csv
```

## Upgrading from 1.x

See [CHANGELOG.md](CHANGELOG.md#migration-from-1x). In short: Spring users must have
`spring-context` and `aspectjweaver` on their own classpath, and messages now go to `System.err`
instead of SLF4J.

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md).

## License

HumanPanic is [MIT licensed](LICENSE).
