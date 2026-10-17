"""Wait for Phase A, repair quarantined groups with two workers, then resume handover."""
import argparse
import datetime
import fcntl
import hashlib
import json
import os
from pathlib import Path
import signal
import subprocess
import sys
import time
import traceback
from survival_common import EXP, REPO, RESULTS, check_inputs, write_json
from raw_integrity import archive_reports, assert_no_sigkill, quarantine
from finish_part3 import check_raw, container_running


def stop_continuation(pid):
    result = subprocess.run(['ps', '-p', str(pid), '-o', 'command='], capture_output=True, text=True)
    if result.returncode or str(EXP / 'tools/finish_part3.py') not in result.stdout:
        return
    os.kill(pid, signal.SIGTERM)
    for _ in range(100):
        result = subprocess.run(['ps', '-p', str(pid), '-o', 'command='], capture_output=True, text=True)
        if result.returncode:
            return
        time.sleep(.1)
    raise RuntimeError('Continuation did not exit after SIGTERM')


def verify_repaired(entries):
    results = []
    for index, group in sorted({(r['index'], r['group']) for r in entries}):
        path = RESULTS / 'raw' / f'{index}-{group}.json'
        raw = json.loads(path.read_text())
        assert_no_sigkill(raw, path.name)
        murmur = [m for m in raw['methods'] if m['class'] == 'org.apache.commons.codec.digest.MurmurHash3Test']
        if group == 'dev':
            assert len(murmur) == 27 and all(m['state'] in {'pass', 'fail', 'error'} for m in murmur), path.name
        results.append({'index': index, 'group': group, 'sha256': hashlib.sha256(path.read_bytes()).hexdigest(),
                        'sigkill_exits': 0, 'murmur_methods_evaluated': len(murmur),
                        'murmur_states': {state: sum(m['state'] == state for m in murmur) for state in sorted({m['state'] for m in murmur})}})
    return results


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--kt', type=Path, required=True)
    parser.add_argument('--phase-a-container', required=True)
    parser.add_argument('--continuation-pid', type=int, required=True)
    args = parser.parse_args()
    lock = (EXP / 'work/phase-a-repair.lock').open('a')
    fcntl.flock(lock, fcntl.LOCK_EX | fcntl.LOCK_NB)
    state_path = EXP / 'work/phase-a-repair.json'
    state = json.loads(state_path.read_text()) if state_path.exists() else {'steps': {}}
    def record(stage, **fields):
        state.update({'stage': stage, 'updated': datetime.datetime.now(datetime.timezone.utc).isoformat(), **fields})
        write_json(state_path, state)
        print(json.dumps({'stage': stage, **fields}), flush=True)
    try:
        check_inputs()
        if 'original_phase_finished' not in state['steps']:
            previous = None
            while True:
                manifest, moved = quarantine('a')
                if len(manifest['entries']) != previous:
                    record('waiting-phase-a', quarantined_records=len(manifest['entries']))
                    previous = len(manifest['entries'])
                completion = RESULTS / 'phase-a.json'
                if completion.exists():
                    done = json.loads(completion.read_text())
                    assert done['completed'] and set(done['indices']) == set(range(1, 540)), 'Original full Phase A did not complete'
                    state['steps']['original_phase_finished'] = done
                    record('phase-a-finished')
                    break
                assert container_running(args.phase_a_container), 'Phase A stopped without its completion record'
                time.sleep(30)
        stop_continuation(args.continuation_pid)
        continuation_lock = (EXP / 'work/part3-continuation.lock').open('a')
        fcntl.flock(continuation_lock, fcntl.LOCK_EX | fcntl.LOCK_NB)
        ledger_path = EXP / 'work/part3-continuation.json'
        ledger = json.loads(ledger_path.read_text())
        assert not ledger['steps'], 'Continuation already advanced; inspect before replacing measurements'
        manifest, moved = quarantine('a')
        bad = {(r['index'], r['group']) for r in manifest['entries']}
        missing = {(i, g) for i in range(1, 540) for g in ['dev', 'llm'] if not (RESULTS / 'raw' / f'{i}-{g}.json').exists()}
        assert missing <= bad, f'Unexplained missing records: {missing - bad}'
        healthy_path = EXP / 'work/phase-a-repair-preserved.json'
        if not healthy_path.exists():
            write_json(healthy_path, {p.name: {'sha256': hashlib.sha256(p.read_bytes()).hexdigest(), 'mtime_ns': p.stat().st_mtime_ns}
                                     for p in (RESULTS / 'raw').glob('*.json')})
        if missing:
            archive_reports('a', [r for r in manifest['entries'] if (r['index'], r['group']) in missing])
            write_json(RESULTS / 'phase-a-quarantine.json', manifest)
            indices = sorted({i for i, g in missing})
            command = f'python tools/run_phase.py --phase a --workers 2 --start {indices[0]} --end {indices[-1]}'
            record('repairing-with-two-workers', affected_indices=indices, command=command)
            log = args.kt / 'evaluation/logs/commit-survival-part3-phase-a-repair.log'
            with log.open('a') as stream:
                result = subprocess.run(['docker', 'compose', '-f', 'compose.yml', '-f', 'compose.commit-survival.yml',
                    'run', '--rm', '--name', 'ktester-commit-survival-phase-a-repair', 'ktester', 'bash', '-c',
                    'cd /repo/experiments/ktester-commit-survival && ' + command], cwd=args.kt / 'docker', stdout=stream, stderr=subprocess.STDOUT)
            assert result.returncode == 0, f'Repair failed; see {log}'
        manifest, new_bad = quarantine('a')
        assert not new_bad, 'Two-worker repair still produced signal-9 exits; kept in quarantine'
        repaired = verify_repaired(manifest['entries'])
        full = check_raw('a')
        healthy = json.loads(healthy_path.read_text())
        for name, expected in healthy.items():
            path = RESULTS / 'raw' / name
            assert hashlib.sha256(path.read_bytes()).hexdigest() == expected['sha256'] and path.stat().st_mtime_ns == expected['mtime_ns'], name
        verification = {'passed': True, 'workers': 2, 'corrected_records': repaired,
                        'healthy_records_preserved': len(healthy), 'full_phase_verification': full,
                        'verified_at': datetime.datetime.now(datetime.timezone.utc).isoformat()}
        write_json(RESULTS / 'phase-a-repair-verification.json', verification)
        manifest['status'] = 'all quarantined records repaired and full Phase A verified'
        write_json(RESULTS / 'phase-a-quarantine.json', manifest)
        if 'error' in ledger:
            state['old_continuation_error'] = ledger.pop('error')
        ledger['stage'] = 'ready-after-oom-repair'
        ledger['updated'] = verification['verified_at']
        write_json(ledger_path, ledger)
        record('repaired-and-verified', corrected_records=len(repaired), healthy_records_preserved=len(healthy))
        fcntl.flock(continuation_lock, fcntl.LOCK_UN)
        continuation_lock.close()
        log = args.kt / 'evaluation/logs/commit-survival-part3-continuation.log'
        with log.open('a') as stream:
            process = subprocess.Popen([sys.executable, str(EXP / 'tools/finish_part3.py'), '--kt', str(args.kt),
                '--phase-a-container', args.phase_a_container], cwd=REPO, stdin=subprocess.DEVNULL,
                stdout=stream, stderr=subprocess.STDOUT, start_new_session=True)
        checkpoint = Path('/private/tmp/ktester-part3-progress.json')
        saved = json.loads(checkpoint.read_text())
        saved.update({'continuation_pid': process.pid, 'phase_a_repair': 'verified',
                      'next_action': 'Repaired Phase A passed full verification; continuation owns aggregation and guarded Phase B.'})
        checkpoint.write_text(json.dumps(saved, indent=2) + '\n')
        record('complete', continuation_pid=process.pid)
    except BaseException:
        record('needs-inspection', error=traceback.format_exc())
        raise


if __name__ == '__main__':
    main()
