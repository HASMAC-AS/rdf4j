# Codex Agent Guidelines

This is a large multi-module Maven project. Building and testing the entire repository can take considerable time. It's not unusual for test execution to take 5–10 minutes per module.

## Build
- Always invoke Maven in offline mode using the `-o` flag.
- To build the entire project without running tests:
  ```bash
  mvn -o verify -DskipTests
  ```
- To build the project and run all tests:
  ```bash
  mvn -o verify
  ```
  Running all modules sequentially will take a long time.

## Code Formatting
- This project has strict code formatting requirements. Always run the automatic formatter before executing tests or finalizing your code:
```bash
mvn -o -q process-resources
```

## Running Tests
- To test a specific module, use the `-pl` option. Example for running the SHACL tests:
  ```bash
  mvn -o -pl core/sail/shacl test
  ```
- Running from a module subdirectory is also possible; remember to include `-o`.
