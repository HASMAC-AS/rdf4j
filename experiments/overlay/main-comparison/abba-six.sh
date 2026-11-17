#!/bin/bash
set -euo pipefail
cd "$(dirname "$0")/.."
for spec in 'main true six-main-a' 'exp true six-exp-b' 'exp true six-exp-c' 'main true six-main-d'; do
 WARMUPS=1 RUNS=2 bash main-comparison/run-six.sh $spec
done
for spec in 'main true six-main-exact' 'exp true six-exp-exact'; do
 EXACT=true bash main-comparison/run-six.sh $spec
done
