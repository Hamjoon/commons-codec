#!/usr/bin/env bash
set -euo pipefail
# Run inside KTester compose's ktester service; its ktester-m2 volume is /root/.m2.
script_dir="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
python "$script_dir/fetch_classpath.py"
