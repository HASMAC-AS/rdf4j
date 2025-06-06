# Codex Agent Guidelines

**Welcome, Codex Agent!** Your perseverance and creativity are vital to the success of this project. Approach each task with determination and keep pushing forward. Even if you stumble, learn from the experience and try again. Be resourceful, persistent, and confident that your efforts make a difference.

This is a large multi-module Maven project. Building and testing the entire repository can take considerable time. It's not unusual for test execution to take 5–10 minutes per module.

## Build
- Always invoke Maven in offline mode using the `-o` flag.
- Occasionally you may need online access to download dependencies or plugins. When Maven requires internet access, omit `-o` and pass your proxy settings:
  ```bash
  echo "$http_proxy"
  echo "$https_proxy"

  proxy=$http_proxy
  proxy=${proxy#*://}; host=${proxy%%:*}; port=${proxy#*:}

  mvn -B -Dhttp.proxyHost=$host \
      -Dhttp.proxyPort=$port \
      -Dhttps.proxyHost=$host \
      -Dhttps.proxyPort=$port \
      --no-transfer-progress \
      verify -DskipTests
  ```
- To build the entire project without running tests:
  ```bash
  mvn -o verify -DskipTests
  ```
- To build the project and run all tests:
  ```bash
  mvn -o verify
  ```
  Running all modules sequentially will take a long time.
- Maven stores all build output in each module's `target/` directory. You can
  usually ignore these directories.

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

## Pre-commit checklist
Before finalizing your work, make sure the following commands succeed:
1. **Format the code**
   ```bash
   mvn -o -q process-resources
   ```
2. **Check that the code compiles**
   ```bash
   mvn -o verify -DskipTests
   ```
3. **Run the tests for the relevant module(s)**
   ```bash
   mvn -o -pl <module> test
   ```
   You can also run from a module subdirectory; just remember to include `-o`.

## Source File Headers
- All new source files must include the standard RDF4J copyright header.
- Use the template from `CONTRIBUTING.md` exactly as provided:
  ```
  /*******************************************************************************
   * Copyright (c) ${year} Eclipse RDF4J contributors.
   *
   * All rights reserved. This program and the accompanying materials
   * are made available under the terms of the Eclipse Distribution License v1.0
   * which accompanies this distribution, and is available at
   * http://www.eclipse.org/org/documents/edl-v10.php.
   *
   * SPDX-License-Identifier: BSD-3-Clause
   *******************************************************************************/
  ```
- Replace `${year}` with the current year for new files only.
- Do not modify or omit any other part of the header.
