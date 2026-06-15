#!/usr/bin/env bash
set -euo pipefail
TIME_LIMIT_SECONDS="${1:-0.5}"
RUNS_COUNT="${2:-200}"
BASE_SEED="${3:-20260614}"
CONFIG_DIR="${4:-configs}"
OUT_DIR="out_parallel_staged_ils_sa_${TIME_LIMIT_SECONDS}s"
RAW_FILE="$OUT_DIR/parallel_staged_ils_sa_raw.csv"

mkdir -p build_parallel_staged_ils_sa "$OUT_DIR"
find src -name '*.java' | sort > build_parallel_staged_ils_sa/sources.list
javac -encoding UTF-8 -d build_parallel_staged_ils_sa @build_parallel_staged_ils_sa/sources.list
java -cp build_parallel_staged_ils_sa app.ParallelStagedIlsSaRun "$TIME_LIMIT_SECONDS" "$RUNS_COUNT" "$BASE_SEED" "$CONFIG_DIR" data/TSPA.csv data/TSPB.csv > "$RAW_FILE"
python3 summarize_ils_sa_start_variants.py "$RAW_FILE" "$OUT_DIR"
