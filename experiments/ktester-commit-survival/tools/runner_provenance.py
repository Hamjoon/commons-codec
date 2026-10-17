"""Accept only the explicitly authorized Phase A concurrency-only runner transition."""
import json
from pathlib import Path


def runner_compatible(saved, current, phase):
    if saved == current:
        return True
    if phase != 'a':
        return False
    path = Path(__file__).resolve().parents[1] / 'results/part3-worker-restart.json'
    if not path.exists():
        return False
    record = json.loads(path.read_text())
    return (saved == record['previous_runner_sha256']
            and current == record['resumed_runner_sha256'])
