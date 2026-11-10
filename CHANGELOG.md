# Changelog

All notable changes to this project are documented here. The project follows
[Semantic Versioning](https://semver.org/).

## [2.0.0] - unreleased

A production-hardening release. The `@HumanPanic` annotation and its attributes are unchanged.
Version 2.0.0 is a major version because the dependency scopes and the destination of the user
message have changed. See [Migration from 1.x](#migration-from-1x).

### Added
- Spring property `humanpanic.report-dir` sets the crash report directory.
- After a handled failure, intercepted methods return an empty value instead of `null` when
  the declared return type is `Optional` (or a primitive variant), `List`, `Collection`,
  `Iterable`, `Set` or `Map`.
- Crash reports are created readable by their owner only (`rw-------`) on POSIX file systems.
- Demos for picocli, JCommander and Apache Commons CLI.
- GitHub Actions CI: Linux and Windows, JDK 17 and 21.
- `PanicHandler`, a framework-free entry point. It offers `install()` (the default
  uncaught-exception handler, for crashes in `main`), `wrap(target, Interface.class)` (proxy) and
  `handle(Throwable)` (manual). The README always claimed global handling, but 1.x did not
  implement it.
- Crash reports now include `timestamp`, `thread`, and the full **cause chain** (`caused_by`), each
  cause with its own backtrace.
- `NonSpringConfigLoader.load()` and `load(Properties)` now actually load metadata, from
  `application.properties` (classpath, then working directory, UTF-8) and `-Dapp.*` system
  properties.
- New constructors that take explicit settings: `CrashReporter(AppMetadata, Path)`,
  `UserNotifier(AppMetadata)` and `UserNotifier(AppMetadata, PrintStream)`.
- Other additions: `AppMetadata` copy constructor, `UserNotifier.printStackTrace(Throwable)` and
  `CrashReporter.getReportDirectory()`.
- A `demo/` build with a plain-Java CLI and a Spring service that consume the published artifact.
- About 85 new tests, including concurrency, forked-JVM exit-code, Spring JDK-proxy and Spring Boot
  tests.

### Changed (behaviour)

- **The user message is written to `System.err`** (or a `PrintStream` you supply), not to an SLF4J
  logger at INFO level. Before, users with no SLF4J binding saw nothing.
- **`VirtualMachineError`s are rethrown** after reporting (`OutOfMemoryError`,
  `StackOverflowError`, ...). Before, the method returned `null` as if nothing had happened.
- **`InterruptedException`**: the interrupt flag is restored after handling.
- `printStackTrace = true` writes to the same stream as the message.
- An empty `message` is passed through as empty. The `"An error occurred"` placeholder is gone,
  so a real message with that text is now shown instead of being suppressed.
- `AppMetadata` defaults: authors default to an empty array, and the homepage, support and issue
  URLs default to `""`. Before, they defaulted to placeholder text (`"Unknown Support-URL"`, ...)
  that was shown to users.
- `ConfigLoader`: `app.authors` defaults to none (it used to be `{"Unknown"}`). Authors are
  trimmed, and blank entries are dropped.
- The short (no-report) message no longer claims "The application will continue running."
- Report file names include milliseconds and a random suffix:
  `crash-yyyy-MM-dd-HH-mm-ss-SSS-xxxxxx.json`.
- Crash reports name the implementation class, even for interface-proxied Spring beans.
- `HumanPanicConfiguration` declares its beans explicitly. Their names all start with `humanPanic`
  (`humanPanicAppMetadata`, `humanPanicConfigLoader`, ...), and it no longer component-scans
  `io.pants.humanpanic`.
- HumanPanic's own problems are logged through `System.Logger`: one WARNING line, with the stack
  trace at DEBUG.

### Fixed

- NPE in the Spring aspect when a bean is proxied through an interface (JDK proxy), which is the
  default for plain `@EnableAspectJAutoProxy`.
- `HumanPanicProxy` ignored `@HumanPanic` on the implementation's methods.
- Crash reports written in the same second overwrote each other.
- Any exception while building a report (for example a `null` metadata field) escaped and replaced
  the application's exception.
- `NonSpringConfigLoader` never loaded anything, and it leaked input streams.
- The manifest fallback reported **HumanPanic's own** jar title and version as the application's.
- `HumanPanicProxy.wrap(target)` missed interfaces inherited from superclasses. `isWrapped(null)`
  threw an NPE. `createProxy` mutated shared state. `equals` was not reflexive through the proxy.
- Reflection failures inside the proxy (`IllegalAccessException`) were reported as application
  crashes.
- `AppMetadata.getAuthors()` exposed the internal array.
- `ConfigLoader` no longer depends on `jakarta.annotation`'s `@PostConstruct` being on the
  classpath. It implements `InitializingBean` instead.

### Removed

- **All runtime dependencies.** Spring, AspectJ, Jackson (databind and YAML), SLF4J,
  `jakarta.annotation-api` and **Lombok** (which leaked onto consumers' runtime classpath) are no
  longer pulled in.
- Native AspectJ weaving support (`META-INF/aop.xml` and the post-compile weaving). It could never
  work: the woven aspect had no no-arg constructor, so `aspectOf()` always failed.
- YAML configuration in `NonSpringConfigLoader`. It never ran in 1.x.
- `HumanPanicAspect.handlePanic(ProceedingJoinPoint)`. The advice is now
  `handlePanic(ProceedingJoinPoint, HumanPanic)`, with the annotation bound by the pointcut.
- `HumanPanicConfiguration` no longer registers a `PropertySourcesPlaceholderConfigurer`.
  Spring's built-in fallback resolves `${app.*:default}` placeholders from the `Environment`.

### Build

- The bytecode target is lowered from Java 21 to **Java 17**.
- Group and version are defined once, in `gradle.properties`, and can be overridden with
  `-Pversion`.
- Archives are reproducible.
- The jar is named `HumanPanic-<version>.jar`, with `Implementation-Title: HumanPanic`.
- Full Javadoc, with zero doclint warnings. `-Xlint:all` is clean.
- `LICENSE` now names the correct copyright holder.

### Migration from 1.x

1. **Spring users:** make sure your build has `org.springframework:spring-context` and
   `org.aspectj:aspectjweaver`. Spring Boot users get both from `spring-boot-starter-aop`.
   Nothing else changes: keep `@Import(HumanPanicConfiguration.class)` and your `app.*`
   properties.
2. **If you captured the user message from the `io.pants.humanpanic.reporter.UserNotifier`
   logger**, capture `System.err` instead, or construct `UserNotifier(metadata, yourStream)`.
3. **If you injected HumanPanic beans by name** (`appMetadata`, `crashReporter`, ...), switch to
   by-type injection or use the new `humanPanic*` names.
4. **If you relied on HumanPanic to provide Jackson, Lombok, SLF4J or `jakarta.annotation` to your
   application**, declare them yourself.
5. **If you parse report file names**, update to the new `crash-...-SSS-xxxxxx.json` pattern.
6. **Non-Spring users** previously had to build a `ConfigLoader` by hand. Use
   `PanicHandler.builder()` instead.

## [1.0.0] - 2025-10-29

- Initial release.
