# Contributing to HumanPanic

Thank you for considering contributing to the HumanPanic project! 🎉  
Your help makes the project better for everyone.

## Code of Conduct
Please follow the [Contributor Covenant](https://www.contributor-covenant.org/) or whatever code of conduct you adopt.

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
   * Java 8+ (or configured target version)
   * Gradle (if you prefer your local install) or use the bundled wrapper (`./gradlew`, `gradlew.bat`)

3. **Build the project**
   ```bash
   ./gradlew clean build
   ```
   This will compile the code, run tests, and produce build artifacts.

4. **Code style / formatting**
   * Follow the existing code style (indentation, braces, naming)

5. **Testing**
   * Add unit tests for new features/bug fixes
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