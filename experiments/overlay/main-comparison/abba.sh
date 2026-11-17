#!/bin/bash
set -euo pipefail
cd "$(dirname "$0")/.."
for spec in 'main true narrow-main-a' 'exp true narrow-exp-b' 'exp true narrow-exp-c' 'main true narrow-main-d'; do
 WARMUPS=1 RUNS=2 bash main-comparison/run.sh $spec
done
for spec in 'main true narrow-main-exact' 'exp true narrow-exp-exact'; do
 EXACT=true bash main-comparison/run.sh $spec
done
for spec in 'main false wide-main-a' 'exp false wide-exp-b' 'exp false wide-exp-c' 'main false wide-main-d'; do
 WARMUPS=0 RUNS=1 bash main-comparison/run.sh $spec
done
