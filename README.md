# HumanPanic
![JitPack]([![](https://jitpack.io/v/harsh11101/HumanPanic.svg)](https://jitpack.io/#harsh11101/HumanPanic))

**Human-friendly panic handler for Java CLI applications**

This library provides a simple way to convert “panic” scenarios in Java CLI applications into clearer, human-friendly output, and optionally collect debug data for easier diagnosis.

---

## What it does
When a Java CLI application encounters an unexpected runtime error (an unhandled exception, fatal error, etc.), by default the stack trace and exception details may be confusing for end users or harder to parse for diagnosis.  
HumanPanic aims to:
- Intercept uncaught exceptions / errors at a global level.
- Provide a more readable “panic message” to users (less intimidating, more actionable).
- Optionally collect a minimal dump/report (stack trace, metadata) to assist debugging (if configured).
- Make it easier for developers and operators to recognise, diagnose and triage incidents without overwhelming end-users with low-level details.

---

## How it works
1. When a fatal/unhandled exception or error occurs, HumanPanic catches it and:
    - Suppresses or replaces the default noisy stack-trace for end-users (in production builds).
    - Logs or writes a concise crash-report file (if configured) containing helpful metadata (application name, version, timestamp, OS, stack trace, etc).
    - Displays (or logs) an end-user-friendly message: e.g. “Something went wrong, we’re sorry. A report has been generated. Please send it to support.”
2. Developer/Support can then open the generated crash report file to inspect the real trace, reproduce, fix, or request further info from the user.

---

## Getting Started
### Requirements
- Java 8 or higher (or whichever version you support)

### Installation
Include the library in your config files (example for Gradle):
```groovy
// In settings.gradle or settings.gradle.kts
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
        maven { url 'https://jitpack.io' }
    }
}
```

```groovy
// In build.gradle or build.gradle.kts
dependencies {
    implementation "com.github.harsh11101:HumanPanic:<version you want to use>"
}
```

### Usage

#### Basic usage

```java
import io.pants.humanpanic.HumanPanic;

public class MyService {
    @HumanPanic
    public void performOperation() {
        throw new RuntimeException("Something unexpected happened");
    }
}
```

#### Custom message & stack-trace enabled

```java
@HumanPanic(
    message = "Database connection failed – please retry later.",
    printStackTrace = true
)
public void connectToDatabase(String url) {
    throw new IllegalStateException("DB state invalid");
}
```

#### Silent mode (no user-facing message) but still generate report

```java
@HumanPanic(
    silent = true,
    createCrashReport = true
)
public void backgroundTask() {
    throw new RuntimeException("Background error");
}
```

#### Disable crash-report generation, just logging

```java
@HumanPanic(
    createCrashReport = false,
    message = "Minor failure – please check logs."
)
public void methodWithoutReport() {
    throw new RuntimeException("Minor error");
}
```

### What happens on panic

* If an uncaught exception/error occurs, you’ll see a short friendly message (not the full stack trace) on the console
* A crash report, in form of a .json file, is generated containing metadata + full stack trace + optionally system info
* You can instruct your users (via the message) to send the report file

---
## Why use it?

* Improves user experience: end-users don’t see raw exceptions and feel “left out”
* Improves diagnostics: you get structured crash reports, easier triage
* Encourages bug reporting: by making it easy for users to send a report
* Works as a safety net in production builds to avoid silent failures

---

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md) for instructions on how to contribute, set up the project locally, coding standards, etc.

---

## License

HumanPanic is [MIT licensed](LICENSE)

---

Thank you for using HumanPanic!
If you encounter bugs, please first Panic Humanely, then open an issue or submit a pull request.

---