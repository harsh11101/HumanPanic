# Contributing to HumanPanic

Thank you for considering contributing to the HumanPanic project! 🎉  
Your help makes the project better for everyone.

## Code of Conduct
Please follow the [Contributor Covenant](https://www.contributor-covenant.org/).

## How to contribute
You can contribute by:
- Reporting bugs/issues
- Suggesting or implementing new features
- Improving documentation
- Writing tests or improving test coverage
- Refactoring code for clarity, performance or maintainability

## Setup for local development
1. **Fork the repository** 
    Create your own copy of the repository by forking the repository.

1. **Clone the repository**
   ```bash
   git clone https://github.com/<yourUserName>/HumanPanic.git
   cd HumanPanic
    ```

2. **Ensure you have prerequisites installed**
   * JDK 17 or newer (the library is compiled with `--release 17`)
   * The bundled Gradle wrapper (`./gradlew`, `gradlew.bat`)

3. **Build the project**
   ```bash
   ./gradlew clean build
   ```
   This compiles the code (with `-Xlint:all`), runs all tests, and builds the jar, sources jar
   and Javadoc (with zero doclint warnings).

   To try your change the way a consumer would, run the demo:
   ```bash
   ./gradlew :lib:publishToMavenLocal
   ./gradlew -p demo installDist
   ```

4. **Code style / design rules**
   * Follow the existing code style (indentation, braces, naming)
   * The core must keep **zero runtime dependencies**. Spring/AspectJ are `compileOnly` and may only
     be referenced from the Spring integration classes (`HumanPanicConfiguration`, `ConfigLoader`,
     `HumanPanicAspect`)
   * Handling a failure must never throw: catch and log via `io.pants.humanpanic.internal.Diagnostics`
   * `io.pants.humanpanic.internal` is not public API; everything else is, so document it with Javadoc

5. **Testing**
   * Add unit tests for new features, and a regression test for every bug fix
   * Ensure all tests pass before submitting a pull request

6. **Commit / Branching Workflow**
   * Create a feature or bugfix branch from `main` named like `feature/xyz` or `bugfix/abc`
   * Make your changes, ensure tests pass locally
   * Write a clear commit message summarising the changes
   * Push your branch to your fork and open a Pull Request (PR) into the `main` branch of this repo

7. **Pull Request Checklist**
   * [ ] Branch is up to date with `main`
   * [ ] Code builds and tests pass
   * [ ] New code is tested (unit tests or integration tests)
   * [ ] Documentation updated if needed
   * [ ] Changes described clearly in the PR description
   * [ ] If applicable, link issue(s) addressed by the PR

## Releasing

Releases are automatic. Every push to `main` that passes CI:

1. tags the commit with the `version` from `gradle.properties`, if that tag does not exist yet;
2. moves the `latest` tag to that commit;
3. asks [JitPack](https://jitpack.io/#harsh11101/HumanPanic) to build the version, and fails if
   JitPack cannot.

So to release, change `version=` in `gradle.properties` (and the version in `README.md` and
`demo/gradle.properties`) and push to `main`. A version tag is never moved: pushing more commits
without changing the version only moves `latest`.

## Issue Reporting

When you open an issue, please include:

* A clear description of the problem
* Steps to reproduce (if applicable)
* Expected behaviour vs actual behaviour
* Environment details: Java version, OS, library version of HumanPanic
* Any relevant logs or stack traces
* Screenshots (if applicable)

## Code Ownership and Commit Rights

* Core maintainers will review and merge contributions
* By submitting a pull request, you agree to license your contribution under the same license as this project

---

Thank you for your contributions. Let’s make HumanPanic a robust and user-friendly panic-handling library together!

---