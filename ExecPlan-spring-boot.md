# Spring Boot Embedded Server for E2E Tests

This ExecPlan is a living document. The sections `Progress`, `Surprises & Discoveries`, `Decision Log`, and `Outcomes & Retrospective` must be kept up to date as work proceeds.

This document must be maintained in accordance with `PLANS.md` at the repository root.

## Purpose / Big Picture

E2E tests currently rely on Docker to boot the RDF4J server and workbench inside a separate servlet container. That arrangement is slow and brittle in constrained CI environments. After implementing this plan, a contributor can run the E2E suite purely on the host JVM by launching a Spring Boot application with an embedded Tomcat server that wires the existing Spring XML configuration for both the server and workbench. The application will expose the same `/rdf4j-server` and `/rdf4j-workbench` endpoints Playwright expects, allowing tests and developers to avoid Docker entirely while exercising the real stack.

## Progress

- [x] (2025-11-15 17:57Z) Established baseline by running the Docker-backed E2E script; it failed early when `xmllint` was missing, confirming reliance on the container toolchain.
- [x] (2025-11-15 18:00Z) Authored `EmbeddedServerBootTest` asserting `/rdf4j-server/repositories` and `/rdf4j-workbench/repositories` respond; test currently fails because both endpoints return non-2xx responses under the skeletal Boot app (see chunk `b61469`).
- [x] (2025-11-15 18:10Z) Created the `tools/server-workbench-boot` Maven module, wired resource copying for the legacy server/workbench WEB-INF assets, and added it to the `tools` reactor; build still red pending servlet wiring.
- [x] (2025-11-15 18:33Z) Completed Boot servlet and filter wiring so `/rdf4j-server` and `/rdf4j-workbench` respond via embedded Tomcat; updated the integration test to check the repositories transformation and captured passing evidence (chunk `a79d4f`, failing baseline `195235`).
- [x] (2025-11-15 18:38Z) Adapted the E2E runner to build and launch the Boot jar, added dynamic jar discovery, and ensured Boot packaging via `spring-boot:repackage`; Playwright install currently fails with HTTP 403 when downloading Chromium (chunk `37393b`) but Boot startup and readiness loop execute.
- [x] (2025-11-15 19:05Z) Added missing SPDX-compliant license headers and ran the project formatter to align with repository guidelines prior to final verification.
- [x] (2025-11-15 19:06Z) Finalized verification checklist items after formatting pass; remaining documentation updates tracked below.
- [x] Adapt E2E runner to launch the Boot application instead of Docker (covering both Tomcat and Jetty loops or replacing with a Boot invocation) and ensure shutdown hooks clean the process.
- [x] (2025-11-15 19:08Z) Re-ran `mvn -pl tools/server-workbench-boot -Dmaven.repo.local=.m2_repo test` to confirm the Boot module still passes after header updates (chunk `1a3011`).
- [x] (2025-11-15 19:10Z) Documented final verification steps and updated the retrospective before handoff.

## Surprises & Discoveries

- Observation: Running `./e2e/run.sh` currently depends on Docker build prerequisites like `xmllint`, which are absent in the bare environment, so the script halts before launching containers.
  Evidence: see chunk `6414a1`.
- Observation: Maven can resolve the existing RDF4J artifacts from the repo-local cache `.m2_repo`, so commands must set `-Dmaven.repo.local=.m2_repo` to compile the new module without building the entire reactor.
  Evidence: `mvn -pl tools/server-workbench-boot -Dmaven.repo.local=.m2_repo test` (chunk `b61469`).
- Observation: The Spring Boot integration test still receives non-2xx responses for `/rdf4j-server/repositories` and `/rdf4j-workbench/repositories` even though Tomcat starts cleanly, indicating the dispatcher mappings need to align with the legacy servlet path expectations.
  Evidence: `mvn -pl tools/server-workbench-boot -Dmaven.repo.local=.m2_repo test` (chunk `d47299`).
- Observation: The workbench servlet returns SPARQL XML with an XSL reference rather than raw HTML, so tests should assert the presence of `repositories.xsl` and ensure the stylesheet is reachable.
  Evidence: `mvn -pl tools/server-workbench-boot -Dmaven.repo.local=.m2_repo test` (chunk `195235`).
- Observation: Playwright’s `install --with-deps` step cannot download Chromium in this environment (repeated HTTP 403 responses), so full e2e execution is blocked despite the Boot server starting successfully.
  Evidence: `./e2e/run.sh` (chunk `37393b`).

## Decision Log

- Decision: Proceed with Spring Boot replacement despite missing Docker tooling because the new path removes that dependency.
  Rationale: The baseline failure demonstrates the pain point we intend to solve.
  Date/Author: 2025-11-15 / Assistant
- Decision: Align the workbench integration test with the XML+XSL response rather than raw HTML to match the legacy behavior while still validating static resource availability.
  Rationale: Browser rendering relies on the XSL transformation, so asserting on the stylesheet keeps coverage without forcing servlet changes.
  Date/Author: 2025-11-15 / Assistant

## Outcomes & Retrospective

- 2025-11-15: Completed license/SPDX compliance follow-up, revalidated the Spring Boot module tests, and captured outstanding Playwright download limitations for future mitigation.

## Context and Orientation

The RDF4J distribution packages two WAR modules under `tools/server` and `tools/workbench`. Both rely on XML-based Spring Web MVC configuration stored beneath `src/main/webapp/WEB-INF`. E2E tests in `e2e/` start the Docker stack defined in `docker/` to deploy these WARs inside Tomcat or Jetty, then Playwright scripts exercise `/rdf4j-server` and `/rdf4j-workbench`. There is no existing Spring Boot launcher for these apps. Maven reactor root `pom.xml` aggregates modules including `tools` and `spring-components` where other Spring Boot samples live.

## Plan of Work

1. Confirm the current test harness requires Docker by executing `e2e/run.sh` and capturing the behavior, establishing baseline evidence for the TDD cycle.
2. Introduce a new integration test under a dedicated Maven module (tentatively `tools/server-workbench-boot`) that uses Spring Boot's testing support to start the embedded container and verify the `/rdf4j-server` and `/rdf4j-workbench` endpoints respond with expected HTTP statuses. The test must fail before the Boot module is implemented.
3. Create the Maven module packaging type `jar` with `spring-boot-starter-web` and `spring-boot-starter-tomcat` dependencies, plus resources that copy existing XML, JSP, and static content from the server/workbench WARs into the Boot application's classpath (preserving their relative paths under `WEB-INF`). Configure the module to skip repackaged WARs to honor the constraint against bundling WARs. Verify the module compiles before wiring servlets.
4. Implement a `@SpringBootApplication` class that imports the XML files using `@ImportResource` and registers corresponding `DispatcherServlet`, `ServletRegistrationBean`, and `FilterRegistrationBean` beans replicating the legacy `web.xml` setup. Provide configuration for context paths `/rdf4j-server` and `/rdf4j-workbench`, including necessary init parameters and resource handlers for JSP rendering. Ensure data directories (e.g., `org.eclipse.rdf4j.appdata.basedir`) default to a writable temp path when not provided. Pay special attention to dispatcher servlet mappings: the RDF4J server dispatcher must see lookup paths like `/repositories` even when invoked under `/rdf4j-server/*`.
5. Wire a command-line launcher (main method) plus start/stop hooks so automation can run the server during Playwright tests. Provide a simple script or Maven goal to start the Boot app in the background and emit readiness logs.
6. Update `e2e/run.sh` (or supporting scripts) to build the Boot module, start it before tests, wait for readiness by polling HTTP endpoints, run Playwright once, and then stop the Boot process. Remove Docker-specific logic while keeping fallback environment variable hooks if necessary.
7. Execute the new integration test and the Playwright suite using the Boot path, capturing passing evidence. Ensure formatting/linting and finalize the plan sections.

## Concrete Steps

1. In repository root, run `./e2e/run.sh` to observe the Docker-based workflow. Keep the terminal output snippet showing Docker startup as baseline evidence.
2. Add a JUnit-based integration test (e.g., `EmbeddedServerBootIT`) under `tools/server-workbench-boot/src/test/java` using `@SpringBootTest(webEnvironment = RANDOM_PORT)` and `TestRestTemplate` to assert HTTP `200 OK` from `/rdf4j-server/repositories` and an HTML response from `/rdf4j-workbench/repositories`. Commit the failing test output to the Evidence section before implementing Boot wiring.
3. Define the new module by editing `tools/pom.xml` (if aggregation is needed) and creating `tools/server-workbench-boot/pom.xml` with dependencies on `rdf4j-http-server-spring`, `rdf4j-http-workbench` classes, `spring-boot-starter-web`, `spring-boot-starter-tomcat`, JSP support, and logging bridges already present. Configure the `<resources>` section to include `../server/src/main/webapp/WEB-INF/**` and `../workbench/src/main/webapp/**` into the jar under `server/` and `workbench/` prefixes. Verify a focused build with `mvn -pl tools/server-workbench-boot -Dmaven.repo.local=.m2_repo test` compiles before servlet wiring proceeds.
4. Implement `Rdf4jEmbeddedApplication` class plus supporting configuration classes under `tools/server-workbench-boot/src/main/java/org/eclipse/rdf4j/serverboot`. Provide beans to register the server `DispatcherServlet`, workbench `WorkbenchGateway`, and filters (`CompressingFilter`, `UrlRewriteFilter`, `PathFilter`, redirect/cache filters, etc.) using Boot's registration beans, pulling init parameters from helper configuration classes. Ensure the RDF4J server dispatcher is mapped to `/rdf4j-server/*` so that requests resolve to legacy handler mappings, and add readiness logging for resource extraction.
5. Supply `application.properties` with server port, context path defaults, and logging configuration to mimic the Docker environment. Add commands or scripts to start the Boot app from the repo root (e.g., `mvn -pl tools/server-workbench-boot spring-boot:run`) and ensure the plan documents expected log output.
6. Modify `e2e/run.sh` to build the Boot jar, start it in the background (capturing PID), wait for readiness by hitting `http://localhost:8080/rdf4j-server/repositories`, run `npx playwright test`, then cleanly kill the Boot process and wait for exit. Remove Docker loops and environment variables referencing `APP_SERVER`.
7. Re-run the integration test and Playwright suite, gather passing logs, and document in `Surprises & Discoveries`, `Decision Log`, and `Outcomes`. Ensure the plan instructs running `mvn -pl tools/server-workbench-boot test` and `./e2e/run.sh` for verification.

## Validation and Acceptance

Success is demonstrated when:

- `mvn -pl tools/server-workbench-boot test` executes the new integration test and reports the server responds as expected.
- `./e2e/run.sh` launches the Spring Boot process, skips Docker entirely, runs Playwright tests successfully, and shuts down the process without leaks.
- Manual navigation to `http://localhost:8080/rdf4j-workbench/` while the Boot app is running renders the workbench UI (optional but documented) to prove JSP support.

## Idempotence and Recovery

The Boot application should tolerate repeated starts: it uses a configurable app data directory (defaulting to `target/rdf4j-app`) that is cleaned between runs. `e2e/run.sh` must trap errors, terminate the Boot process on failure, and leave no Docker containers behind. If tests fail, rerunning the script should rebuild the module and restart cleanly. Provide notes on removing the temporary data directory if manual cleanup is needed.

## Artifacts and Notes

Document key command outputs, including the failing test stack trace before implementation, readiness logs from the Boot app, and the passing Playwright summary. Keep artifacts concise, referencing where to find them in the repository.

## Interfaces and Dependencies

The Boot module will expose a `org.eclipse.rdf4j.serverboot.Rdf4jEmbeddedApplication` main class. Supporting beans include:

- `ServletRegistrationBean<DispatcherServlet>` mapping `/rdf4j-server/*` and `FilterRegistrationBean` instances for `CompressingFilter`, `PathFilter`, and `UrlRewriteFilter` mirroring the legacy server `web.xml` behavior.
- `ServletRegistrationBean<WorkbenchGateway>` mapping `/rdf4j-workbench/repositories/*` with init parameters mirroring the legacy `web.xml`, plus `FilterRegistrationBean` entries for `RedirectFilter`, `CookieCacheControlFilter`, and `CacheFilter`.
- `WebServerFactoryCustomizer<TomcatServletWebServerFactory>` ensuring WEB-INF resources are available for JSP resolution and disabling redundant JAR scanning.
- `AppDataDirectoryConfiguration` guaranteeing `org.eclipse.rdf4j.appdata.basedir` points at a writable directory during tests.

Dependencies should re-use existing artifacts (`rdf4j-http-server-spring`, `rdf4j-http-workbench`) and Spring Boot starters. Avoid bundling WAR artifacts or duplicating configuration logic.

---

Revision 0 (2024-01-29): Initial plan drafted.
Revision 1 (2025-11-15): Updated progress after adding the module skeleton, captured failing Boot test evidence, and noted the dispatcher mapping gap blocking `/rdf4j-server` endpoints.
