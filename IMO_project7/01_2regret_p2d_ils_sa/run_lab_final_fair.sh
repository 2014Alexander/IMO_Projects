#!/usr/bin/env bash
set -euo pipefail

TIME_LIMIT_SECONDS="${1:-0.5}"
RUNS_COUNT="${2:-200}"
BASE_SEED="${3:-20260614}"
CONFIG_DIR="${4:-configs}"
OUT_DIR="out_lab_final_fair_${TIME_LIMIT_SECONDS}s"
BUILD_DIR="build_lab_final_fair"
RAW_FILE="${OUT_DIR}/lab_final_fair_raw.csv"

rm -rf "${BUILD_DIR}"
mkdir -p "${BUILD_DIR}" "${OUT_DIR}"

find src -name '*.java' | sort > "${OUT_DIR}/sources.list"
javac -encoding UTF-8 -d "${BUILD_DIR}" @"${OUT_DIR}/sources.list"

java -cp "${BUILD_DIR}" app.LabFinalFairRunPrepared \
  "${TIME_LIMIT_SECONDS}" "${RUNS_COUNT}" "${BASE_SEED}" "${CONFIG_DIR}" \
  data/TSPA.csv data/TSPB.csv \
  > "${RAW_FILE}"

python3 summarize_lab_final_fair.py "${RAW_FILE}" "${OUT_DIR}"
