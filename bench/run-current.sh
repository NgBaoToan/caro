#!/usr/bin/env bash
# Measure the checked-out production Difficulty settings without reflection overrides.
set -euo pipefail
cd "$(dirname "$0")/.."
repeats=${1:-5}
if ! [[ "$repeats" =~ ^[1-9][0-9]*$ ]]; then
  echo 'Usage: bash bench/run-current.sh [positive repeats]' >&2
  exit 1
fi
mkdir -p bench/build/current bench/results
javac -encoding UTF-8 -d bench/build/current src/main/java/caroai/*.java bench/Benchmark.java bench/LatencyProbe.java
{
  date -u '+UTC: %Y-%m-%dT%H:%M:%SZ'
  uname -srm
  java -version 2>&1
  echo 'JVM: -Xms256m -Xmx512m; warmup=2 per fixture/level'
  echo "Repeats: $repeats"
  cat src/main/java/caroai/Difficulty.java
} > bench/results/current-environment.txt
java -Xms256m -Xmx512m -cp bench/build/current caroai.Benchmark "$repeats" > bench/results/current-850ms.csv
java -Xms256m -Xmx512m -cp bench/build/current caroai.LatencyProbe > bench/results/current-850ms-latency.csv
