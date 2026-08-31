#!/usr/bin/env bash
set -euo pipefail

die() { echo "ERROR: $*" >&2; exit 1; }
have() { command -v "$1" >/dev/null 2>&1; }

nearest_module_dir() {
  local path="$1"
  local dir
  dir="$(cd "$(dirname "$path")" && pwd)"
  while [[ "$dir" != "/" ]]; do
    if [[ -f "$dir/pom.xml" ]]; then
      local root; root="$(git rev-parse --show-toplevel)"
      python3 - <<'PY' "$root" "$dir"
import os, sys
root, d = sys.argv[1], sys.argv[2]
print(os.path.relpath(d, root))
PY
      return
    fi
    dir="$(dirname "$dir")"
  done
  return 1
}

to_fqn_from_test_path() {
  local p="$1"
  local rel="${p#*/src/test/java/}"
  if [[ "$rel" == "$p" ]]; then rel="${p#*/src/test/}"; fi
  rel="${rel%.java}"; rel="${rel%.kt}"; rel="${rel%.groovy}"
  echo "${rel//\//.}"
}

have git || die "git is required"
have mvn || die "maven (mvn) is required"
git rev-parse --is-inside-work-tree >/dev/null 2>&1 || die "Run inside a git repo"

echo "[1/7] Scanning for new/changed test files…"
mapfile -t CANDIDATES < <(git status --porcelain | awk '{print $2}' \
  | grep -E 'src/test/.*/.*(Test|IT)\.(java|kt|groovy)$' || true)
(( ${#CANDIDATES[@]} > 0 )) || die "No modified test files under src/test/**/(Test|IT).*"

SELECTED=""
if (( ${#CANDIDATES[@]} == 1 )); then
  SELECTED="${CANDIDATES[0]}"; echo "  found: $SELECTED"
else
  echo "Multiple candidate tests:"
  i=1; for f in "${CANDIDATES[@]}"; do echo "  [$i] $f"; ((i++)); done
  read -rp "Choose one by number: " idx
  [[ "$idx" =~ ^[0-9]+$ && $idx -ge 1 && $idx -le ${#CANDIDATES[@]} ]] || die "Invalid selection"
  SELECTED="${CANDIDATES[$((idx-1))]}"
fi
[[ -f "$SELECTED" ]] || die "Selected file does not exist: $SELECTED"

echo "[2/7] Deriving module and test class…"
MODULE_DIR="$(nearest_module_dir "$SELECTED")" || die "No pom.xml found upward from $SELECTED"
TEST_FQN="$(to_fqn_from_test_path "$SELECTED")"
REPO_ROOT="$(git rev-parse --show-toplevel)"
echo "  module: $MODULE_DIR"
echo "  test  : $TEST_FQN"
echo "  path  : $SELECTED"

echo "[3/7] Preflight: reactor install with -Pquick (tests disabled)…"
( set -x; mvn -B -Pquick -DskipTests clean install )

echo "[4/7] Run only the selected test (verify, NO -Pquick, NO -am)…"
set +e
( set -x; mvn -B -pl "$MODULE_DIR" \
    -Dtest="$TEST_FQN" -DfailIfNoTests=false -DtrimStackTrace=false verify )
HEAD_STATUS=$?
set -e

if (( HEAD_STATUS == 0 )); then
  echo "⚠️  Your selected test PASSED on HEAD."
  read -rp "Continue bisect anyway? [y/N] " cont
  [[ "${cont:-N}" =~ ^[yY]$ ]] || exit 0
else
  echo "✅ Test fails on HEAD (good for bisect)."
fi

read -rp "[5/7] Enter a known-good tag or commit (e.g. v3.7.5 or a1b2c3d): " GOOD_REF
[[ -n "${GOOD_REF:-}" ]] || die "You must provide a known-good ref"
git rev-parse --verify "$GOOD_REF^{commit}" >/dev/null 2>&1 || die "Ref not found: $GOOD_REF"

SNAP="$(mktemp -t bisect_test_XXXXXX)"
cp "$SELECTED" "$SNAP"
REL_TEST_PATH="$(python3 - <<'PY' "$REPO_ROOT" "$SELECTED"
import os, sys
root, p = sys.argv[1], sys.argv[2]
print(os.path.relpath(os.path.abspath(p), os.path.abspath(root)))
PY
)"

RUNNER="$(mktemp -t bisect_run_XXXXXX.sh)"
cat > "$RUNNER" <<'RUN'
#!/usr/bin/env bash
set -euo pipefail

ROOT="$(git rev-parse --show-toplevel)"
TEST_PATH="$REL_TEST_PATH"
SNAP="$SNAP"
MODULE_DIR="$MODULE_DIR"
TEST_FQN="$TEST_FQN"

# Recreate the test file in the current checkout
mkdir -p "$ROOT/$(dirname "$TEST_PATH")"
cp "$SNAP" "$ROOT/$TEST_PATH"

# 1) Prepare reactor artifacts fast: -Pquick install (tests disabled)
if ! mvn -B -Pquick -DskipTests clean install; then
  exit 125  # unrelated build break -> skip commit
fi

# 2) Run ONLY the chosen test (verify) — NO -Pquick, NO -am
mvn -B -pl "$MODULE_DIR" \
  -Dtest="$TEST_FQN" -DfailIfNoTests=false -DtrimStackTrace=false verify
RUN
chmod +x "$RUNNER"

echo "[6/7] Starting git bisect…"
git bisect start
git bisect bad || true
git bisect good "$GOOD_REF"

set +e
git bisect run "$RUNNER"
BISECT_STATUS=$?
set -e

echo "[7/7] Resetting bisect state…"
git bisect reset >/dev/null || true

rm -f "$RUNNER" "$SNAP"

if (( BISECT_STATUS == 0 )); then
  echo "🎯 Bisect finished. The output above shows the FIRST BAD COMMIT."
  echo "Tip: run 'git show' to inspect it."
else
  echo "Bisect exited with status $BISECT_STATUS. Check the logs above."
fi

