"""Verify the authorized eight-worker restart without rerunning any measurement."""
import collections
import hashlib
import json
from survival_common import RESULTS, check_inputs, denominator, write_json
from runner_provenance import runner_compatible


def main():
    check_inputs()
    plan = json.loads((RESULTS / 'part3-worker-restart.json').read_text())
    for name, expected in plan['preserved_raw_records'].items():
        path = RESULTS / 'raw' / name
        assert hashlib.sha256(path.read_bytes()).hexdigest() == expected['sha256'], name
        assert path.stat().st_mtime_ns == expected['mtime_ns'], name
    layout = json.loads((RESULTS / 'phase-a-running.json').read_text())
    assert layout['workers'] == plan['resumed_workers'] == 8
    expected_layout = {str(i + 1): list(range(1, 540))[i::8] for i in range(8)}
    assert layout['worker_layout'] == expected_layout
    assert runner_compatible(plan['previous_runner_sha256'], plan['resumed_runner_sha256'], 'a')
    assert not runner_compatible(plan['previous_runner_sha256'], plan['resumed_runner_sha256'], 'b')
    tampered = {**plan['resumed_runner_sha256'], 'run_commit.py': 'unapproved change'}
    assert not runner_compatible(plan['previous_runner_sha256'], tampered, 'a')
    complete = [i for i in range(1, 540) if i not in plan['completed_commit_indices']
                and all((RESULTS / 'raw' / f'{i}-{g}.json').exists() for g in ['dev', 'llm'])]
    complete.sort(key=lambda i: max((RESULTS / 'raw' / f'{i}-{g}.json').stat().st_mtime_ns for g in ['dev', 'llm']))
    if len(complete) < 3:
        print(json.dumps({'status': 'waiting for at least three new complete commits',
                          'new_complete_indices': complete, 'preserved_records_verified': len(plan['preserved_raw_records'])}))
        return
    counts = collections.Counter()
    failures = []
    ids = {g: {r['id'] for r in denominator(g)} for g in ['dev', 'llm']}
    for i in complete:
        for g in ids:
            raw = json.loads((RESULTS / 'raw' / f'{i}-{g}.json').read_text())
            assert raw['runner_sha256'] == plan['resumed_runner_sha256']
            assert set(raw['requested_ids']) == ids[g]
            if raw['status'] == 'build-fail':
                failures.append({'index': i, 'group': g, 'state': 'build-fail'})
                continue
            assert len(raw['methods']) == len(ids[g]) and {r['id'] for r in raw['methods']} == ids[g]
            counts.update(r['state'] for r in raw['methods'])
            failures += [{'index': i, 'group': g, 'id': r['id'], 'state': r['state']}
                         for r in raw['methods'] if r['state'] != 'pass']
    result = {'passed': True, 'host_hw_ncpu': plan['host_hw_ncpu'], 'workers': 8,
              'preserved_raw_records': len(plan['preserved_raw_records']),
              'original_raw_bytes_and_mtimes_unchanged': True,
              'original_completed_commits': len(plan['completed_commit_indices']),
              'worker_layout': expected_layout, 'first_pending_by_worker': plan['first_pending_by_worker'],
              'first_new_completed_indices': complete[:8], 'new_completed_commits_at_verification': complete,
              'new_method_states': dict(counts), 'new_nonpassing_results': failures,
              'unapproved_runner_hash_change_rejected': True, 'phase_b_workers': 8}
    write_json(RESULTS / 'part3-worker-restart-verification.json', result)
    print(json.dumps({k: v for k, v in result.items() if k not in {'worker_layout', 'new_nonpassing_results'}}, indent=2))


if __name__ == '__main__':
    main()
