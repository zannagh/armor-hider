#!/usr/bin/env bash
# Smoke-launch every loader/version combination.
#
# Usage:
#   scripts/smoke-all.sh                 # all combos, full compat set
#   scripts/smoke-all.sh --no-compat     # all combos, no compat mods (clean run/mods)
#   scripts/smoke-all.sh --only fabric   # only fabric-* combos
#   scripts/smoke-all.sh --only neoforge # only neoforge-* combos
#   scripts/smoke-all.sh --include gender,geckolib  # fetch only those compat mods
#   scripts/smoke-all.sh --delay 30000   # raise per-launch boot window from 15s to 30s
#
# Each launch opens a Minecraft window briefly and exits 0 if no crash within the
# boot window. On Linux CI, wrap the whole script in `xvfb-run`. On macOS/Windows
# desktops, the window will appear; this is unavoidable without GLFW patching and
# is the same behaviour as a real dev run.
#
# Exit code: non-zero on the first failed combo (gradle exit propagates). A summary
# of pass/fail per combo is written to .me/smoke-results-<timestamp>.txt.

set -uo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

FABRIC_VERSIONS=(
    "fabric-1.20.1" "fabric-1.21.1" "fabric-1.21.4" "fabric-1.21.8"
    "fabric-1.21.10" "fabric-1.21.11" "fabric-26.1.2" "fabric-26.2"
)
NEOFORGE_VERSIONS=(
    "neoforge-1.21.1" "neoforge-1.21.4" "neoforge-1.21.8"
    "neoforge-1.21.10" "neoforge-1.21.11" "neoforge-26.1.2" "neoforge-26.2"
)

ONLY="both"
COMPAT="all"
INCLUDE=""
DELAY="15000"
while [[ $# -gt 0 ]]; do
    case "$1" in
        --only) ONLY="$2"; shift 2 ;;
        --no-compat) COMPAT="none"; shift ;;
        --include) COMPAT="$2"; INCLUDE="$2"; shift 2 ;;
        --delay) DELAY="$2"; shift 2 ;;
        -h|--help) sed -n '2,18p' "$0"; exit 0 ;;
        *) echo "unknown flag: $1" >&2; exit 2 ;;
    esac
done

VERSIONS=()
case "$ONLY" in
    fabric)   VERSIONS=("${FABRIC_VERSIONS[@]}") ;;
    neoforge) VERSIONS=("${NEOFORGE_VERSIONS[@]}") ;;
    both)     VERSIONS=("${FABRIC_VERSIONS[@]}" "${NEOFORGE_VERSIONS[@]}") ;;
    *) echo "--only must be one of: fabric, neoforge, both" >&2; exit 2 ;;
esac

mkdir -p .me
TS="$(date +%Y%m%d-%H%M%S)"
RESULTS=".me/smoke-results-${TS}.txt"
echo "armor-hider smoke run @ ${TS}" > "$RESULTS"
echo "compat=${COMPAT}, delay=${DELAY}ms" >> "$RESULTS"
echo "" >> "$RESULTS"

PASS=0
FAIL=0
FAILED_COMBOS=()
for v in "${VERSIONS[@]}"; do
    loader="${v%%-*}"
    label=":${loader}:${v}:runClient"
    echo "::: smoke ${v}"
    if ./gradlew "$label" \
        -Psmoke \
        -Pcompat="$COMPAT" \
        -Psmoke.delay.ms="$DELAY" \
        --console=plain --no-daemon; then
        echo "  PASS  $v" | tee -a "$RESULTS"
        PASS=$((PASS+1))
    else
        echo "  FAIL  $v" | tee -a "$RESULTS"
        FAIL=$((FAIL+1))
        FAILED_COMBOS+=("$v")
    fi
done

echo "" | tee -a "$RESULTS"
echo "Done — pass=${PASS} fail=${FAIL}" | tee -a "$RESULTS"
if [[ $FAIL -gt 0 ]]; then
    echo "Failed combos:" | tee -a "$RESULTS"
    for v in "${FAILED_COMBOS[@]}"; do echo "  - $v" | tee -a "$RESULTS"; done
    exit 1
fi
