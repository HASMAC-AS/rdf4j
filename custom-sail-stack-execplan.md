# Add Custom Sail Stack builder to Workbench

This ExecPlan is a living document. The sections `Progress`, `Surprises & Discoveries`, `Decision Log`, and `Outcomes & Retrospective` must be kept up to date as work proceeds.

This plan must be maintained in accordance with PLANS.md at /workspace/rdf4j/PLANS.md.

## Purpose / Big Picture

Workbench users need a single “Custom Sail Stack” option in the New Repository flow so they can compose a base store (Memory, Native, or LMDB) with optional wrapper sails (SHACL, RDFS inferencer, Lucene), configure each layer in place, preview the generated Turtle configuration, and create a repository with that configuration. After this change, a user can open the new Custom Sail Stack page, build a stack, view the Turtle config, and successfully create a repository; the generated config should round-trip through RDF4J configuration parsing without errors.

## Progress

- [x] 2026-01-31 18:20Z Capture repository instructions and existing create flow.
- [x] 2026-01-31 18:50Z Build stack spec model, registry, validation, and config generator.
- [x] 2026-01-31 18:52Z Add preview endpoint and create flow wiring.
- [x] 2026-01-31 18:56Z Implement Custom Sail Stack UI and preview wiring.
- [x] 2026-01-31 18:45Z Run formatting, targeted tests, screenshots, and handoff prep.

## Surprises & Discoveries

- Observation: TypeScript compilation reported pre-existing errors in delete.ts and server.ts but still emitted JS outputs.
  Evidence: tools/workbench/compileTypescript.sh output (TS7006/TS2551) during compilation.

## Decision Log

- Decision: Use a dedicated JSON preview endpoint to return Turtle + warnings, while the existing CreateServlet handles the final repository creation using the same stack spec.
  Rationale: Keep the preview logic separate from the create flow, and avoid mixing preview responses into the existing XSL transformation flow.
  Date/Author: 2026-01-31 (Codex)

## Outcomes & Retrospective

- Outcome: Implemented the Custom Sail Stack builder end-to-end (backend config generation, preview endpoint, UI, and tests). Workbench now supports live Turtle preview and validated stack creation for Memory/Native/LMDB with SHACL, RDFS, and Lucene wrappers.

## Context and Orientation

Workbench routes are implemented by commands under tools/workbench/src/main/java/org/eclipse/rdf4j/workbench/commands, and rendered through XSL transformations in tools/workbench/src/main/webapp/transformations. The “New Repository” flow uses CreateServlet and create.xsl, with per-type XSL pages such as create-memory.xsl and config templates under core/repository/api/src/main/resources/org/eclipse/rdf4j/repository/config. JavaScript for Workbench lives under tools/workbench/src/main/webapp/scripts, with TypeScript sources in tools/workbench/src/main/webapp/scripts/ts compiled by tools/workbench/compileTypescript.sh.

A “Sail” is RDF4J’s storage and inference layer; a “stack” is a chain of Sail configurations where each wrapper delegates to the next via config:delegate. The unified RDF4J configuration vocabulary is in the namespace tag:rdf4j.org,2023:config/ and is exposed via CONFIG in code. LMDB-specific configuration uses the namespace http://rdf4j.org/config/sail/lmdb# as defined by LmdbStoreSchema.

## Plan of Work

First, add a small model and registry in the Workbench module to represent the stack spec coming from the browser: repository id/title plus an ordered list of layers. The registry will define supported sails (Memory, Native, LMDB, RDFS inferencer, SHACL, Lucene), their kind (base vs wrapper), default values, and the logic to build the corresponding SailImplConfig objects. Build a validator that enforces one base at the bottom, no unsupported duplicates, and required fields, and that emits warnings for ordering concerns (RDFS/Lucene and RDFS/SHACL). Next, implement a config generator that wraps sails bottom-up, produces a SailRepositoryConfig and RepositoryConfig, exports to a Model, and serializes Turtle using Rio.

Then, add a new servlet endpoint for previewing configs. It should accept JSON, validate it, and return JSON containing the Turtle string plus any warnings or validation errors. Update CreateServlet to accept the same stack spec and call the config generator, then add the repository config to the manager. Add unit tests that parse the generated Turtle back into RepositoryConfig and verify validation passes.

Finally, add the Custom Sail Stack UI as a new create-* XSL page plus TypeScript that manages the stack list, renders per-layer config fields, posts to the preview endpoint, and fills a read-only Turtle preview. Wire the form submission to include the JSON stack spec for the create action. Update create.xsl to include the new option in the repository type list, and include new CSS or inline styles as needed.

## Concrete Steps

1. Create new Java classes under tools/workbench/src/main/java/org/eclipse/rdf4j/workbench/sailstack for:
   - SailStackSpec (repo + list of layers), RepoSpec, SailLayerSpec, and an enum of layer types.
   - SailDescriptor registry with buildConfig methods using RDF4J SailImplConfig classes.
   - SailStackValidator returning errors and warnings.
   - SailStackConfigGenerator that returns a Turtle string and RepositoryConfig.

2. Add a new servlet under tools/workbench/src/main/java/org/eclipse/rdf4j/workbench/commands to handle /custom-sail-preview. It should parse JSON using ObjectMapper, validate, and return JSON with fields (turtle, warnings, errors). Update tools/workbench/src/main/webapp/WEB-INF/web.xml to add the init-param mapping.

3. Update CreateServlet to detect type=custom-sail and use the config generator instead of a template. The form should pass a JSON string (stackSpec) from the UI. Validation errors should throw a ServletException with a clear message.

4. Add new XSL page tools/workbench/src/main/webapp/transformations/create-custom-sail.xsl that includes:
   - Repository ID and title fields.
   - A stack editor (list with base at bottom, add wrapper dropdown, reorder buttons).
   - Per-layer panels with fields for the supported sails.
   - A read-only Turtle preview area plus a Download button.
   - Create/Cancel buttons that reuse the existing create flow.

5. Add TypeScript under tools/workbench/src/main/webapp/scripts/ts/create-custom-sail.ts to manage stack editing, JSON spec construction, preview POSTs, and warnings rendering. Compile with tools/workbench/compileTypescript.sh.

6. Add unit tests under tools/workbench/src/test/java/org/eclipse/rdf4j/workbench/sailstack verifying:
   - Memory, Native, and LMDB stacks generate valid configs and round-trip parsing.
   - Wrappers are nested in the expected order by config:delegate.
   - Validation errors are produced for missing base or missing Lucene indexDir.

7. Run formatting, quick install, and targeted tests for the Workbench module.

## Validation and Acceptance

- Start Workbench with the existing server (or via tests) and navigate to New Repository → Custom Sail Stack. Build a stack, confirm the Turtle preview updates, and click Create. The new repository should appear and open its summary page.
- Run targeted unit tests for the new stack generator and any updated servlet tests. Tests should fail before the change and pass after, but for Routine D the plan is to run and confirm they pass for the final implementation.

## Idempotence and Recovery

The new endpoints and UI are additive. If a step fails, revert the last changed file, rerun the formatter, and re-run the targeted tests. The config generator is deterministic; repeated submissions produce the same Turtle output for the same input.

## Artifacts and Notes

- Expected preview response JSON shape:
    {
      "turtle": "@prefix ...",
      "warnings": ["Put RDFS above Lucene if you want inferred literals searchable."],
      "errors": []
    }

- Example stack spec posted by the UI:
    {
      "repo": { "id": "myrepo", "title": "My Repo" },
      "stack": [
        { "type": "LUCENE", "config": { "indexDir": "lucene" } },
        { "type": "RDFS", "config": {} },
        { "type": "LMDB", "config": { "tripleIndexes": "spoc,posc" } }
      ]
    }

## Interfaces and Dependencies

Use existing RDF4J config classes in the Workbench module:

- org.eclipse.rdf4j.sail.memory.config.MemoryStoreConfig
- org.eclipse.rdf4j.sail.nativerdf.config.NativeStoreConfig
- org.eclipse.rdf4j.sail.lmdb.config.LmdbStoreConfig
- org.eclipse.rdf4j.sail.inferencer.fc.config.SchemaCachingRDFSInferencerConfig
- org.eclipse.rdf4j.sail.shacl.config.ShaclSailConfig
- org.eclipse.rdf4j.sail.lucene.config.LuceneSailConfig
- org.eclipse.rdf4j.repository.sail.config.SailRepositoryConfig
- org.eclipse.rdf4j.repository.config.RepositoryConfig

Repository configs should be exported with CONFIG vocabulary (CONFIG.NS), and LMDB settings should use LmdbStoreSchema namespace. JSON parsing should use Jackson ObjectMapper, which is already available in the Workbench module dependencies.

Plan revision note: Initial plan drafted for implementation; no revisions yet.

Plan revision note: Updated progress to reflect completed backend/UI steps and recorded the TypeScript compilation warnings observed during script execution.
Plan revision note: Marked final progress item complete and recorded outcome summary after implementing and validating the feature.
