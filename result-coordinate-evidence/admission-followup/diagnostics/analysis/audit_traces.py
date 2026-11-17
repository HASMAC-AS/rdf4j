#!/usr/bin/env python3
"""Read-only audit of the finite direct-table cohort; never launches Java."""

import argparse
import hashlib
import json
from pathlib import Path


ARMS = ("ordinary", "initial", "always")
WORKING_SETS = (64, 4096, 8192, 16384, 65536)
WINDOW = 8192
REQUESTS = 131072
MASK64 = (1 << 64) - 1


def require(condition, message):
    if not condition:
        raise AssertionError(message)


def sha256(path):
    digest = hashlib.sha256()
    with path.open("rb") as handle:
        for block in iter(lambda: handle.read(1 << 20), b""):
            digest.update(block)
    return digest.hexdigest()


def full_hash(identifier):
    value = (identifier ^ ((4 * 0x9E3779B97F4A7C15) & MASK64)) & MASK64
    value ^= value >> 33
    value = (value * 0xFF51AFD7ED558CCD) & MASK64
    value ^= value >> 33
    value = (value * 0xC4CEB9FE1A85EC53) & MASK64
    return value ^ (value >> 33)


HASHES = [full_hash(identifier) for identifier in range(max(WORKING_SETS) + 2)]


def reserved(capacity):
    return 148 * capacity + 240 if capacity else 0


def arrays(capacity):
    return 36 * capacity + 80 if capacity else 0


def reference_trace(arm, working_set, bootstrap):
    """Source-derived mechanism model; no Values, source caches, GC, or timing."""
    table = None
    capacity = probes = hits = collisions = calls = 0
    resizes = weak_allocations = cutoff = 0
    peak_capacity = peak_reserved = peak_arrays = 0
    first_hit = first_tail_hit = prefix_hits = prefix_calls = 0
    disabled = False
    checkpoints = []
    for offset in range(REQUESTS):
        request = offset + 1
        identifier = working_set + 1 if offset < bootstrap else 1 + (offset - bootstrap) % working_set
        hit = False
        if arm != "ordinary" and not disabled:
            if table is None:
                capacity = 4096
                table = [0] * capacity
                peak_capacity = capacity
                peak_reserved = reserved(capacity)
                peak_arrays = arrays(capacity)
            probes += 1
            at = HASHES[identifier] & (capacity - 1)
            previous = table[at]
            if previous == identifier:
                hits += 1
                hit = True
            elif previous:
                collisions += 1
            if probes == WINDOW:
                if arm == "initial" and hits < probes // 4:
                    cutoff = 1
                    disabled = True
                    table = None
                    capacity = 0
            elif probes % WINDOW == 0 and capacity < 65536 and hits >= probes // 4 and collisions >= probes // 8:
                next_capacity = capacity * 2
                replacement = [0] * next_capacity
                for retained in table:
                    if retained:
                        destination = HASHES[retained] & (next_capacity - 1)
                        require(replacement[destination] == 0, "Fixed-hash doubling lost an occupied bucket")
                        replacement[destination] = retained
                peak_reserved = max(peak_reserved, reserved(capacity) + reserved(next_capacity))
                peak_arrays = max(peak_arrays, arrays(capacity) + arrays(next_capacity))
                capacity = next_capacity
                table = replacement
                peak_capacity = capacity
                resizes += 1
        if hit:
            first_hit = first_hit or request
            if request > bootstrap:
                first_tail_hit = first_tail_hit or request
        else:
            calls += 1
            if arm != "ordinary" and not disabled:
                table[HASHES[identifier] & (capacity - 1)] = identifier
                weak_allocations += 1
        if request == bootstrap:
            prefix_hits, prefix_calls = hits, calls
        if request == 1 or request % WINDOW == 0:
            checkpoints.append({
                "request": request, "memo_probes": probes, "hits": hits, "source_calls": calls,
                "collisions": collisions, "capacity": capacity, "reserved_bytes": reserved(capacity),
                "disabled": disabled,
            })
    memo = None if arm == "ordinary" else {
        "probes": probes, "hits": hits, "collisions": collisions, "invalidations": 0,
        "unsupported_values": 0, "clear_calls": 0, "resizes": resizes, "rehash_discards": 0,
        "low_hit_disable": cutoff, "initial_admission_refusals": 0, "growth_admission_refusals": 0,
        "peak_capacity": peak_capacity, "peak_array_bytes": peak_arrays,
        "occupied_slots_at_close": sum(identifier != 0 for identifier in table) if table is not None else 0,
        "capacity_at_close": capacity, "reserved_after_close": 0, "disabled_at_close": int(disabled),
        "collected_misses": 0, "weak_entry_allocations": weak_allocations, "owner_reference_allocations": 0,
        "peak_reservation_bytes": peak_reserved, "weak_retention": 1,
    }
    return {
        "source_calls": calls, "initializations": calls, "hits": hits,
        "tail_hits": hits - prefix_hits, "tail_source_calls": calls - prefix_calls,
        "first_hit_request": first_hit, "first_tail_hit_request": first_tail_hit,
        "capacity_before_close": capacity, "reserved_before_close": reserved(capacity),
        "peak_reserved_bytes": peak_reserved, "ledger_after_close": 0, "manager_after_close": 0,
        "checkpoints": checkpoints, "memo": memo,
    }


def input_files(manifest):
    for base, value in manifest.items():
        if isinstance(value, str):
            yield Path(base), value
        else:
            require(isinstance(value, dict), "Unknown input-manifest format")
            for relative, expected in value.items():
                yield Path(base) / relative, expected


def transitions(checkpoints):
    result = []
    previous = None
    for checkpoint in checkpoints:
        state = checkpoint["capacity"], checkpoint["disabled"]
        if state != previous:
            result.append(checkpoint)
            previous = state
    return result


def audit(root):
    expected_cases = {(width, bootstrap) for width in WORKING_SETS for bootstrap in (0, WINDOW)}
    result = {"scope": "untimed_synthetic_direct_table", "status": "pass", "processes": {}, "cases": []}
    by_arm = {}
    common_inputs = None
    normalized_commands = []
    raw_file_count = 0
    for arm in ARMS:
        directory = root / arm
        process = json.loads((directory / "process.json").read_text())
        require(process["exitCode"] == 0 and not process["timedOut"], f"{arm}: process did not finish normally")
        require(process["inputsUnchanged"] is True and process["comparisonSample"] is False,
                f"{arm}: process provenance differs")
        for name, expected in process["rawHashes"].items():
            require(sha256(directory / name) == expected, f"{arm}: raw hash differs: {name}")
            raw_file_count += 1
        inputs_bytes = (directory / "inputs.json").read_bytes()
        if common_inputs is None:
            common_inputs = inputs_bytes
        require(inputs_bytes == common_inputs, f"{arm}: runtime input manifest differs")
        command = json.loads((directory / "command.json").read_text())["argv"]
        require(command[-1] == arm, f"{arm}: command selects a different treatment")
        normalized_commands.append(["<gc-log-option>" if argument.startswith("-Xlog:gc:file=") else argument
                                    for argument in command[:-1]])
        lines = (directory / "stdout.jsonl").read_text().splitlines()
        records = [json.loads(line) for line in lines if line.startswith("{")]
        require(len(records) == 10, f"{arm}: expected exactly ten JSON records")
        cases = {}
        for ordinal, row in enumerate(records):
            case = row["working_set"], row["bootstrap_requests"]
            require(case not in cases and case in expected_cases, f"{arm}: unexpected/duplicate case {case}")
            require(row["kind"] == "untimed-admission-trace" and row["arm"] == arm, f"{arm}: wrong record kind")
            require(row["execution_scope"] == "direct_table_not_query" and row["source_cache"] == "none"
                    and row["gc_control"] == "intentional_live_consumers", f"{arm}: different fixture scope")
            require(row["requests"] == REQUESTS and row["tail_requests"] == REQUESTS - case[1],
                    f"{arm}: request count mismatch")
            require(row["tail_complete_cycles"] == (REQUESTS - case[1]) // case[0]
                    and row["tail_partial_cycle_requests"] == (REQUESTS - case[1]) % case[0],
                    f"{arm}: cycle count mismatch")
            require(row["source_calls"] == row["requests"] - row["hits"], f"{arm}: source/hit identity mismatch")
            require(row["initializations"] == row["source_calls"], f"{arm}: synthetic decoder count mismatch")
            modeled = reference_trace(arm, *case)
            for field, expected in modeled.items():
                if field == "memo" and expected is not None:
                    actual = dict(row[field])
                    require(actual.pop("owner_ordinal") == ordinal + 1, f"{arm}: wrong owner sequence")
                else:
                    actual = row[field]
                require(actual == expected, f"{arm} {case}: source-derived model differs at {field}")
            cases[case] = row
            result["cases"].append({
                "arm": arm, "working_set": case[0], "bootstrap_requests": case[1],
                "requests": REQUESTS, "tail_requests": row["tail_requests"],
                "hits": row["hits"], "tail_hits": row["tail_hits"],
                "hit_rate_percent": 100 * row["hits"] / REQUESTS,
                "tail_hit_rate_percent": 100 * row["tail_hits"] / row["tail_requests"],
                "source_calls": row["source_calls"], "tail_source_calls": row["tail_source_calls"],
                "capacity_before_close": row["capacity_before_close"],
                "peak_reserved_bytes": row["peak_reserved_bytes"],
                "first_hit_request": row["first_hit_request"],
                "first_tail_hit_request": row["first_tail_hit_request"],
                "checksum_hex": row["checksum_hex"],
                "memo": row["memo"], "capacity_transitions": transitions(row["checkpoints"]),
                "all_checkpoints_match_model": True,
            })
        require(set(cases) == expected_cases, f"{arm}: incomplete matrix")
        by_arm[arm] = cases
        result["processes"][arm] = {
            "exit_code": 0, "timed_out": False, "inputs_unchanged_recorded": True,
            "raw_hashes_verified": process["rawHashes"], "process_sha256": sha256(directory / "process.json"),
            "records": len(records), "non_json_startup_lines": sum(not line.startswith("{") for line in lines),
        }
    require(all(command == normalized_commands[0] for command in normalized_commands[1:]),
            "Runtime commands differ beyond arm and GC log destination")
    for case in expected_cases:
        require(len({by_arm[arm][case]["checksum_hex"] for arm in ARMS}) == 1,
                f"Cross-arm checksum mismatch for {case}")
        if case[1] != 0:
            first = dict(by_arm["initial"][case])
            second = dict(by_arm["always"][case])
            first.pop("arm")
            second.pop("arm")
            require(first == second, f"Bootstrapped initial/always behavior differs for {case}")
    manifest = json.loads(common_inputs)
    entries = list(input_files(manifest))
    for path, expected in entries:
        require(path.is_file() and sha256(path) == expected, f"Current captured input differs: {path}")
    result["verification"] = {
        "processes": 3, "records": 30, "total_checked_requests": 30 * REQUESTS,
        "cross_arm_checksum_groups": 10, "checkpoint_states_reconciled": 30 * 17,
        "enabled_records_without_collection_or_invalidations": 20,
        "records_with_zero_query_and_global_ledger_after_close": 30,
        "bootstrapped_initial_always_exact_record_pairs": 5,
        "raw_files_hash_verified": raw_file_count, "captured_input_files_hash_verified": len(entries),
        "captured_input_bytes_hash_verified": sum(path.stat().st_size for path, _ in entries),
        "common_input_manifest_sha256": hashlib.sha256(common_inputs).hexdigest(),
        "commands_match_except_arm_and_log_destination": True,
        "reference_model_scope": "fixed full-key hash, live referents, current cutoff/growth, no source cache",
    }
    return result


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--raw", type=Path, required=True)
    parser.add_argument("--output", type=Path, required=True)
    args = parser.parse_args()
    require(not args.output.exists(), f"Refusing to overwrite {args.output}")
    result = audit(args.raw)
    args.output.parent.mkdir(parents=True, exist_ok=True)
    with args.output.open("x") as handle:
        json.dump(result, handle, indent=2, sort_keys=True)
        handle.write("\n")
    print(json.dumps({"status": result["status"], **result["verification"]}, sort_keys=True))


if __name__ == "__main__":
    main()
