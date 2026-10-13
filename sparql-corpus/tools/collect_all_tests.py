#!/usr/bin/env python3
"""Index every retained upstream test without confusing collection with execution.

No upstream code is executed and no network access is performed. Whole Git trees,
including fixtures, test runners, build files, licenses and archived suites, are
verified against their pinned Git inventories before any completeness claim.
"""
from __future__ import annotations

import argparse
import ast
import bisect
import collections
import hashlib
import html
import json
import re
import sys
import zipfile
from pathlib import Path
from urllib.parse import quote, unquote, urlsplit

import rdflib
import yaml
from rdflib import BNode, Graph, Literal, Namespace, RDF, RDFS, URIRef

# Test literals must retain their lexical forms, including intentionally invalid values.
rdflib.NORMALIZE_LITERALS = False

ROOT = Path(__file__).resolve().parents[1]
VIRTUAL = "https://corpus.invalid/"
MF = Namespace("http://www.w3.org/2001/sw/DataAccess/tests/test-manifest#")
FONTS = {".ttf", ".otf", ".woff", ".woff2", ".eot"}
QUERIES = {".rq", ".ru", ".sparql", ".arq"}
TEST_PART = re.compile(r"(?:^|[-_.])(?:test(?:s|ing)?|e2e|specs?)(?:$|[-_.])", re.I)
JAVA_ANNOTATION = re.compile(
    r"@(?:[\w$]+\.)*(Test|ParameterizedTest|RepeatedTest|TestFactory|TestTemplate|Theory)\b")
JAVA_METHOD = re.compile(
    r"\b(?:void|[\w$.]+(?:\s*<[^;{}]+?>)?(?:\s*\[\])?)\s+"
    r"([\w$]+)\s*\([^;{}]*\)\s*(?:throws\s+[^;{}]+)?\{")
CPP_TEST = re.compile(
    r"\b(TEST(?:_F|_P)?|TYPED_TEST(?:_P)?)\s*\(\s*([\w:]+)\s*,\s*([\w:]+)\s*\)\s*\{")
LEXICAL = re.compile(
    r'//[^\n]*|/\*[\s\S]*?\*/|R"([^ ()\\\t\r\n]{0,16})\([\s\S]*?\)\1"'
    r'|"""[\s\S]*?"""|"(?:\\[\s\S]|[^"\\])*"|\'(?:\\[\s\S]|[^\'\\])*\'')


def sha(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


def identifier(text: str) -> str:
    return sha(text.encode("utf-8"))[:24]


def dump(path: Path, obj: object) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(obj, ensure_ascii=True, indent=2, sort_keys=True) + "\n", encoding="utf-8")


def jsonl(path: Path, records: list[dict]) -> None:
    with path.open("w", encoding="utf-8") as out:
        for record in records:
            out.write(json.dumps(record, ensure_ascii=True, sort_keys=True, separators=(",", ":")) + "\n")


def safe_path(root: Path, name: str) -> Path:
    path = (root / name).resolve()
    if not path.is_relative_to(root.resolve()):
        raise ValueError(f"Path escapes source root: {name}")
    return path


def mask_code(text: str) -> str:
    """Preserve offsets/newlines while masking comments and string literals."""
    return LEXICAL.sub(lambda m: re.sub(r"[^\n]", " ", m.group()), text)


def native_declarations(text: str, suffix: str) -> list[dict]:
    """Source declarations, NOT expanded parameter combinations or runtime tests."""
    masked = mask_code(text)
    newlines = [i for i, c in enumerate(text) if c == "\n"]
    # A single linear brace pass avoids repeatedly scanning large method bodies.
    stack: list[int] = []
    ends: dict[int, int] = {}
    for i, char in enumerate(masked):
        if char == "{":
            stack.append(i)
        elif char == "}" and stack:
            ends[stack.pop()] = i + 1
    found = []
    if suffix == ".java":
        methods = list(JAVA_METHOD.finditer(masked))
        starts = [m.start() for m in methods]
        annotated: set[int] = set()
        for annotation in JAVA_ANNOTATION.finditer(masked):
            index = bisect.bisect_left(starts, annotation.end())
            method = methods[index] if index < len(methods) else None
            # Do not associate a class/field annotation with an unrelated method.
            if method and (";" in masked[annotation.end():method.start()]
                           or re.search(r"\b(?:class|interface|enum)\b", masked[annotation.end():method.start()])):
                method = None
            start = annotation.start()
            if method:
                annotated.add(method.start())
                end = ends.get(method.end() - 1, method.end())
                name = method.group(1)
            else:
                end, name = annotation.end(), "unresolved-annotated-declaration"
            found.append((start, end, annotation.group(1), name, method is not None))
        for method in methods:
            if method.start() not in annotated and method.group(1).startswith("test"):
                # Retain candidates even when inheritance/runner configuration is needed.
                found.append((method.start(), ends.get(method.end() - 1, method.end()),
                              "JUnit3-name-candidate", method.group(1), False))
    elif suffix in {".cpp", ".cc", ".cxx", ".h", ".hpp"}:
        for method in CPP_TEST.finditer(masked):
            found.append((method.start(), ends.get(method.end() - 1, method.end()),
                          method.group(1), method.group(2) + "." + method.group(3), True))
    return [{"name": name, "declarationKind": kind, "line": bisect.bisect_left(newlines, start) + 1,
             "endLine": bisect.bisect_left(newlines, end - 1) + 1,
             "startOffset": start, "endOffset": end,
             "declarationResolved": resolved, "sourceTextSha256": sha(text[start:end].encode("utf-8"))}
            for start, end, kind, name, resolved in sorted(found)]


def manifest_entries(graph: Graph) -> list:
    """Deduplicate list entries before assigning ordinals, matching the existing importer."""
    result, seen = [], set()
    for head in graph.objects(None, MF.entries):
        for entry in graph.items(head):
            if entry not in seen:
                result.append(entry)
                seen.add(entry)
    for subject, typ in graph.subject_objects(RDF.type):
        if str(typ).startswith(str(MF)) and "Test" in str(typ) and typ != MF.TestStatus and subject not in seen:
            result.append(subject)
            seen.add(subject)
    return result


class Collector:
    def __init__(self, root: Path):
        self.root = root.resolve()
        self.out = self.root / "collection"
        self.out.mkdir(exist_ok=True)
        self.pins = json.loads((self.root / "sources.json").read_text())
        self.files: dict[str, dict] = {}
        self.archive_paths: dict[str, dict] = {}
        self.errors: list[dict] = []
        self.omitted: list[dict] = []
        self.ported = json.loads((self.root / "corpus/cases.json").read_text())
        self.by_manifest: dict[tuple, list[str]] = collections.defaultdict(list)
        self.by_source: dict[str, list[dict]] = collections.defaultdict(list)
        self.query_paths: set[str] = set()
        for case in self.ported:
            if case.get("manifest") is not None:
                self.by_manifest[(case["manifest"], case.get("ordinal"))].append(case["id"])
            source = case.get("source", {})
            project = next((p for p, pin in self.pins.items() if pin["repository"] == source.get("repository")), None)
            if project and source.get("path"):
                self.by_source[f'vendor/{project}/{source["path"]}'].append(case)
            if case.get("queryAsset", {}).get("path"):
                self.query_paths.add(case["queryAsset"]["path"])

    def source(self, path: str, line: int | None = None) -> dict:
        _, project, name = path.split("/", 2)
        pin = self.pins[project]
        archive = self.archive_paths.get(path)
        origin = archive["archive"] if archive else name
        url = f'https://github.com/{pin["repository"]}/blob/{pin["revision"]}/{quote(origin, safe="/")}'
        if line and not archive:
            url += "#L" + str(line)
        result = {"repository": pin["repository"], "revision": pin["revision"], "path": name,
                  "url": url, "sha256": self.files[path]["sha256"]}
        if archive:
            result.update(archive)
        return result

    def add_file(self, path: str, project: str, mode: str, blob: str | None = None) -> None:
        file = safe_path(self.root, path)
        if file.suffix.lower() in FONTS:
            self.omitted.append({"path": path, "reason": "Font asset excluded from redistribution"})
            return
        if not file.is_file() or file.is_symlink():
            raise ValueError("Missing/non-regular pinned file: " + path)
        data = file.read_bytes()
        actual_blob = hashlib.sha1(b"blob " + str(len(data)).encode() + b"\0" + data).hexdigest()
        if blob is not None and actual_blob != blob:
            raise ValueError("Git blob mismatch: " + path)
        parts = Path(path).parts[2:]
        test_related = any(TEST_PART.search(part) for part in parts) or Path(path).name.lower().startswith("test")
        self.files[path] = {"path": path, "project": project, "bytes": len(data), "mode": mode,
                            "sha256": sha(data), "gitBlobSha1": actual_blob,
                            "tracked": blob is not None, "testRelatedPath": bool(test_related)}

    def verify_trees(self) -> dict:
        counts = {}
        for project, pin in sorted(self.pins.items()):
            if not re.fullmatch(r"[0-9a-f]{40}", pin["revision"]):
                raise ValueError("Source is not pinned to a full Git commit: " + project)
            count = 0
            for row in (self.root / "corpus" / (project + "-git-files.txt")).read_text().splitlines():
                metadata, name = row.split("\t", 1)
                mode, blob, stage = metadata.split()
                if name.startswith('"'):
                    # Git C-quoting represents non-ASCII bytes using octal escapes.
                    name = ast.literal_eval(name).encode("latin1").decode("utf-8")
                if stage != "0" or mode not in {"100644", "100755"}:
                    raise ValueError(f"Uncollected Git entry ({mode}, stage {stage}): {project}/{name}")
                self.add_file(f"vendor/{project}/{name}", project, mode, blob)
                count += 1
            counts[project] = count
        archive_map = json.loads((self.root / "corpus/archive-members.json").read_text())
        grouped: dict[tuple[str, str], list[tuple[str, str]]] = collections.defaultdict(list)
        for old_path, entry in archive_map.items():
            if "/vendor/" not in old_path:
                raise ValueError("Unknown archived member root: " + old_path)
            path = "vendor/" + old_path.split("/vendor/", 1)[1]
            project = path.split("/", 2)[1]
            self.archive_paths[path] = entry
            grouped[project, entry["archive"]].append((path, entry["member"]))
        for (project, name), members in sorted(grouped.items()):
            with zipfile.ZipFile(safe_path(self.root, f"vendor/{project}/{name}")) as archive:
                for path, member in sorted(members):
                    self.add_file(path, project, "100644")
                    if path in self.files and sha(archive.read(member)) != self.files[path]["sha256"]:
                        raise ValueError("Expanded archive member differs from pinned archive: " + path)
        return counts

    def term(self, graph: Graph, node, seen: tuple = ()) -> object:
        if node is None:
            return None
        if isinstance(node, Literal):
            return {"type": "literal", "value": str(node), "datatype": str(node.datatype) if node.datatype else None,
                    "language": node.language}
        if isinstance(node, BNode):
            if node in seen:
                return {"type": "bnode-reference", "ancestor": seen.index(node)}
            result = {"type": "descriptor", "properties": {}}
            for predicate in sorted(set(graph.predicates(node)), key=str):
                children = [self.term(graph, obj, seen + (node,)) for obj in graph.objects(node, predicate)]
                result["properties"][str(predicate)] = sorted(children, key=lambda x: json.dumps(x, sort_keys=True))
            return result
        value = str(node)
        result = {"type": "iri", "value": value}
        if value.startswith(VIRTUAL):
            path = unquote(urlsplit(value).path.lstrip("/"))
            if path in self.files:
                result["asset"] = {"path": path, "sha256": self.files[path]["sha256"], "base": value}
        return result

    def manifest_tests(self) -> tuple[list[dict], list[dict]]:
        records, assets_seen = [], set()
        known = {m["path"] for m in json.loads((self.root / "corpus/manifest-inventory.json").read_text())}
        # Also retry parse-error manifests; never drop a source simply because an old importer did.
        known.update(e["path"] for e in json.loads((self.root / "corpus/import-errors.json").read_text())
                     if e.get("stage", "").startswith("manifest"))
        # Discover manifests outside the original importer inventory.
        for path in self.files:
            file = Path(path)
            if file.suffix.lower() in {".ttl", ".n3"} and "manifest" in file.name.lower():
                known.add(path)
        for path in sorted(known):
            try:
                graph = Graph().parse(safe_path(self.root, path), format="turtle", publicID=VIRTUAL + quote(path, safe="/"))
                entries = manifest_entries(graph)
                # Serialize each descriptor structurally instead of canonicalizing the entire
                # manifest graph: global blank-node symmetry can make canonicalization exponential.
                canonical = graph
                unique = set()
                for ordinal, entry in enumerate(entries):
                    if entry in unique:
                        continue
                    unique.add(entry)
                    types = sorted(str(t) for t in graph.objects(entry, RDF.type))
                    if str(MF.TestStatus) in types:
                        continue
                    name = str(graph.value(entry, MF.name) or graph.value(entry, RDFS.label) or ("anonymous-test-" + str(ordinal) if isinstance(entry, BNode) else entry))
                    properties = {}
                    for predicate in sorted(set(canonical.predicates(entry)), key=str):
                        values = [self.term(canonical, obj) for obj in canonical.objects(entry, predicate)]
                        properties[str(predicate)] = sorted(values, key=lambda x: json.dumps(x, sort_keys=True))
                    assets: dict[str, dict] = {}
                    def gather(value):
                        if isinstance(value, dict):
                            if "asset" in value:
                                assets[value["asset"]["path"]] = value["asset"]
                            for child in value.values():
                                gather(child)
                        elif isinstance(value, list):
                            for child in value:
                                gather(child)
                    gather(properties)
                    query_files = []
                    for asset_path, asset in sorted(assets.items()):
                        assets_seen.add(asset_path)
                        if Path(asset_path).suffix.lower() in QUERIES:
                            query_files.append({**asset, "text": safe_path(self.root, asset_path).read_text(encoding="utf-8", errors="replace")})
                    case_ids = self.by_manifest.get((path, ordinal), [])
                    records.append({"id": identifier(path + "#" + str(ordinal) + ":" + name),
                        "name": name, "manifest": path, "ordinal": ordinal, "types": types,
                        "source": self.source(path), "originalProperties": properties,
                        "queryFiles": query_files, "referencedAssets": list(assets.values()),
                        "rdf4jCaseIds": case_ids,
                        "adaptationStatus": "existing-catalogue" if case_ids else "collected-not-adapted"})
            except Exception as exc:
                self.errors.append({"path": path, "stage": "manifest-index", "error": str(exc),
                    "sourceRetained": path in self.files,
                    "vocabularyDefinitionFile": any(x.lower() == "vocabularies" for x in Path(path).parts)})
        unregistered = [{**record, "source": self.source(path),
                         "reason": "No reference from an indexed manifest or existing executable case; may be a test, example or benchmark"}
                        for path, record in sorted(self.files.items())
                        if Path(path).suffix.lower() in QUERIES and path not in assets_seen and path not in self.query_paths]
        return records, unregistered

    def native_tests(self) -> list[dict]:
        records = []
        for path, info in sorted(self.files.items()):
            suffix = Path(path).suffix.lower()
            if not info["testRelatedPath"] or suffix not in {".java", ".cpp", ".cc", ".cxx", ".h", ".hpp"}:
                continue
            text = safe_path(self.root, path).read_text(encoding="utf-8", errors="replace")
            for declaration in native_declarations(text, suffix):
                linked = [c["id"] for c in self.by_source.get(path, [])
                          if declaration["line"] <= c.get("source", {}).get("line", 0) <= declaration["endLine"]]
                records.append({**declaration, "id": identifier(path + ":" + str(declaration["startOffset"]) + ":" + declaration["declarationKind"]),
                    "path": path, "source": self.source(path, declaration["line"]), "rdf4jCaseIds": sorted(set(linked)),
                    "adaptationStatus": "has-existing-adaptations" if linked else "collected-not-adapted",
                    "prerequisites": "Retained enclosing source, setup/teardown helpers, fixtures and upstream build/runner configuration",
                    "expectedResults": "Original assertions retained in sourceText; not inferred from RDF4J output",
                    "sourceText": text[declaration["startOffset"]:declaration["endOffset"]]})
        return records

    def yaml_tests(self) -> list[dict]:
        records = []
        for path, info in sorted(self.files.items()):
            if not info["testRelatedPath"] or Path(path).suffix.lower() not in {".yaml", ".yml"}:
                continue
            text = safe_path(self.root, path).read_text(encoding="utf-8")
            obj = yaml.safe_load(text)
            if not isinstance(obj, dict) or not isinstance(obj.get("queries"), list):
                continue
            for ordinal, query in enumerate(obj["queries"]):
                if not isinstance(query, dict) or not query.get("sparql"):
                    continue
                name = str(query.get("query", ordinal))
                linked = [c for c in self.by_source.get(path, [])
                          if c.get("family") == "qlever-yaml" and c["name"] == name]
                records.append({"id": identifier(path + "#" + str(ordinal)), "name": name,
                    "path": path, "ordinal": ordinal, "source": self.source(path),
                    "originalQueryAndChecks": query,
                    "suiteSettings": {k: v for k, v in obj.items() if k != "queries"},
                    "rdf4jCaseIds": [c["id"] for c in linked],
                    "existingFixtureDefinitions": [c["fixtures"] for c in linked],
                    "adaptationStatus": "existing-catalogue" if linked else "collected-not-adapted"})
        return records

    def run(self) -> dict:
        counts = self.verify_trees()
        manifests, unregistered = self.manifest_tests()
        natives = self.native_tests()
        yaml_records = self.yaml_tests()
        records = list(sorted(self.files.values(), key=lambda x: x["path"]))
        tests = [r for r in records if r["testRelatedPath"]]
        jsonl(self.out / "files.jsonl", records)
        jsonl(self.out / "test-files.jsonl", tests)
        jsonl(self.out / "manifest-tests.jsonl", manifests)
        jsonl(self.out / "native-tests.jsonl", natives)
        jsonl(self.out / "yaml-tests.jsonl", yaml_records)
        jsonl(self.out / "unregistered-query-files.jsonl", unregistered)
        dump(self.out / "sources.lock.json", self.pins)
        dump(self.out / "index-errors.json", self.errors)
        dump(self.out / "omitted-files.json", self.omitted)
        summary = {"pinnedTrackedFiles": counts, "trackedFilesVerified": sum(r["tracked"] for r in records),
            "archiveMembersVerified": sum(not r["tracked"] for r in records), "allRetainedFilesIndexed": len(records),
            "testRelatedFiles": len(tests), "manifestTestRecords": len(manifests),
            "yamlTestRecords": len(yaml_records), "nativeDeclarationRecords": len(natives), "nativeDeclarationsByKind": dict(collections.Counter(r["declarationKind"] for r in natives)),
            "manifestTestsByType": dict(collections.Counter(t for r in manifests for t in r["types"])),
            "unregisteredQueryFiles": len(unregistered), "existingRdf4jCatalogueCases": len(self.ported),
            "existingCatalogueStatus": dict(collections.Counter(c["status"] for c in self.ported)),
            "manifestIndexErrors": len(self.errors), "nonVocabularyManifestIndexErrors": sum(not e["vocabularyDefinitionFile"] for e in self.errors),
            "omittedFiles": len(self.omitted), "completePinnedRegularFileCollection": not self.omitted,
            "exhaustiveRuntimeTestEnumerationProven": False, "allNativeAssertionsPorted": False,
            "newRdf4jExecutionPerformed": False, "python": sys.version.split()[0], "rdflib": rdflib.__version__,
            "notes": ["Counts are source records, not distinct semantic behaviors or executed tests.",
                "Parameterized/factory/inherited tests may expand into multiple runtime tests.",
                "All tracked files are retained, irrespective of path classification or declaration recognition.",
                "Manifest descriptors include original query/update actions, expected results and local asset hashes.",
                "The old execution reports remain historical; collection does not establish that cases pass."]}
        dump(self.out / "summary.json", summary)
        links = [("Manifest records, including updates", "manifest-tests.jsonl"),
                 ("Native declarations and original assertion bodies", "native-tests.jsonl"),
                 ("QLever YAML queries and original checks", "yaml-tests.jsonl"),
                 ("Test-related files", "test-files.jsonl"), ("Every verified file", "files.jsonl"),
                 ("Unregistered query/update files", "unregistered-query-files.jsonl"),
                 ("Source pins", "sources.lock.json"), ("Indexing errors", "index-errors.json")]
        (self.out / "index.html").write_text(
            '<!doctype html><html lang="en"><meta charset="utf-8"><title>Complete source test collection</title>'
            '<style>body{font:16px system-ui;max-width:1050px;margin:40px auto;padding:0 20px;line-height:1.5}pre{white-space:pre-wrap;overflow-wrap:anywhere}a{display:block;margin:.6em 0}</style>'
            '<h1>Jena and QLever: complete pinned source collection</h1><p>Collection is not execution. '
            'The existing RDF4J catalogue and unported source tests are explicitly separate.</p>'
            '<a href="../corpus/catalog.html">Existing RDF4J/JUnit Jupiter catalogue</a>'
            + ''.join('<a href="' + file + '">' + html.escape(name) + '</a>' for name, file in links)
            + '<h2>Verified collection summary</h2><pre>' + html.escape(json.dumps(summary, indent=2)) + '</pre></html>', encoding="utf-8")
        print(json.dumps(summary, indent=2), flush=True)
        return summary


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--root", type=Path, default=ROOT)
    args = parser.parse_args()
    result = Collector(args.root).run()
    if not result["completePinnedRegularFileCollection"] or result["nonVocabularyManifestIndexErrors"]:
        raise SystemExit("Incomplete collection/index: inspect collection/summary.json and index-errors.json")


if __name__ == "__main__":
    main()
