"""Run Phase B at eight workers; preserve evidence and resume at six on container OOM."""
import argparse
import datetime
import fcntl
import hashlib
import json
from pathlib import Path
import subprocess
import time
import traceback
from raw_integrity import archive_reports, quarantine
from survival_common import EXP, RESULTS, check_inputs, write_json

NAME = 'ktester-commit-survival-phase-b'
# Only multiprocessing workers are signalled. PID 1 joins them, records the
# interrupted invocation, and exits; the container then terminates their JVMs.
STOP_WORKERS = '''
import os, signal
from pathlib import Path
workers = []
for p in Path('/proc').iterdir():
    if not p.name.isdigit():
        continue
    try:
        argv = (p / 'cmdline').read_bytes().split(b'\\0')
        fields = (p / 'stat').read_text().rsplit(')', 1)[1].split()
        if int(fields[1]) == 1 and b'tools/run_phase.py' in argv:
            workers.append(int(p.name))
    except (FileNotFoundError, ProcessLookupError):
        pass
for sig in (signal.SIGSTOP, signal.SIGTERM, signal.SIGCONT):
    for pid in workers:
        try:
            os.kill(pid, sig)
        except ProcessLookupError:
            pass
'''


def stamp():
    return datetime.datetime.now(datetime.timezone.utc).isoformat()


def inspect(container):
    result = subprocess.run(['docker', 'inspect', container], capture_output=True, text=True, timeout=20)
    return json.loads(result.stdout)[0] if result.returncode == 0 else None


def memory_events(container):
    result = subprocess.run(['docker', 'exec', container, 'cat', '/sys/fs/cgroup/memory.events'],
                            capture_output=True, text=True, timeout=20)
    return {k: int(v) for k, v in (line.split() for line in result.stdout.splitlines())} if result.returncode == 0 else {}


def oom_events(container, since):
    result = subprocess.run(['docker', 'events', '--since', str(since), '--until', str(int(time.time()) + 1),
                             '--filter', 'container=' + container, '--filter', 'event=oom', '--format', '{{json .}}'],
                            capture_output=True, text=True, timeout=20, check=True)
    return [json.loads(line) for line in result.stdout.splitlines() if line.strip()]


def has_oom(attempt, info):
    return bool(attempt.get('oom_events') or info['State'].get('OOMKilled') or
                any(attempt.get('memory_events', {}).get(key, 0) for key in ['oom', 'oom_kill', 'oom_group_kill']))


def snapshot_healthy():
    paths = []
    for raw in (RESULTS / 'raw-coverage').glob('*.json'):
        paths.append(raw)
        coverage = json.loads(raw.read_text()).get('coverage')
        if coverage:
            paths += [EXP / coverage[key] for key in ['xml', 'lines_csv']]
    return {str(p.relative_to(EXP)): {'sha256': hashlib.sha256(p.read_bytes()).hexdigest(), 'mtime_ns': p.stat().st_mtime_ns}
            for p in paths}


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--kt', type=Path, required=True)
    args = parser.parse_args()
    lock = (EXP / 'work/phase-b-guard.lock').open('a')
    fcntl.flock(lock, fcntl.LOCK_EX | fcntl.LOCK_NB)
    path = RESULTS / 'phase-b-resource-events.json'
    state = json.loads(path.read_text()) if path.exists() else {'workers': 8, 'attempts': [], 'current': None}
    output = RESULTS / 'phase-b.json'
    def record(stage):
        state.update({'stage': stage, 'updated': stamp()})
        write_json(path, state)
        print(json.dumps({'stage': stage, 'workers': state['workers']}), flush=True)
    try:
        check_inputs()
        if state.get('stage') == 'complete':
            return
        while True:
            if state['current'] is None:
                # Only remove a container retained by one of this controller's attempts.
                existing = inspect(NAME)
                if existing:
                    assert not existing['State']['Running'] and any(a.get('container') == existing['Id'] for a in state['attempts'])
                    subprocess.run(['docker', 'rm', existing['Id']], check=True, capture_output=True)
                if output.exists():
                    previous = json.loads(output.read_text())
                    assert state['workers'] == 6 and state['attempts'], 'Unexpected existing Phase B completion record'
                    assert any(json.loads(p.read_text()) == previous for p in (RESULTS / 'phase-b-invocations').glob('*.json'))
                    output.unlink()
                command = f'python tools/run_phase.py --phase b --workers {state["workers"]} --start 0 --end 539'
                attempt = {'workers': state['workers'], 'started': stamp(), 'since': int(time.time()), 'command': command}
                # Retain the container until its OOM state, logs and completion have been saved.
                result = subprocess.run(['docker', 'compose', '-f', 'compose.yml', '-f', 'compose.commit-survival.yml',
                    'run', '-d', '--name', NAME, 'ktester', 'bash', '-c',
                    'cd /repo/experiments/ktester-commit-survival && exec ' + command],
                    cwd=args.kt / 'docker', capture_output=True, text=True, check=True)
                info = inspect(NAME)
                assert info is not None, result.stderr
                attempt['container'] = info['Id']
                state['attempts'].append(attempt)
                state['current'] = len(state['attempts']) - 1
                record('running')
            attempt = state['attempts'][state['current']]
            container = attempt['container']
            while True:
                info = inspect(container)
                assert info is not None, 'Managed Phase B container disappeared'
                if info['State']['Running']:
                    counters = memory_events(container)
                    attempt['memory_events'] = {k: max(v, attempt.get('memory_events', {}).get(k, 0)) for k, v in counters.items()} or attempt.get('memory_events', {})
                if has_oom(attempt, info) and info['State']['Running']:
                    record('oom-stopping-workers')
                    # The exec client may itself be terminated when PID 1 exits.
                    subprocess.run(['docker', 'exec', container, 'python', '-c', STOP_WORKERS], capture_output=True, timeout=20)
                    subprocess.run(['docker', 'wait', container], capture_output=True, timeout=30, check=True)
                    info = inspect(container)
                if not info['State']['Running']:
                    break
                time.sleep(5)
            attempt['oom_events'] = oom_events(container, attempt['since'])
            attempt.update({'finished': stamp(), 'container_state': info['State']})
            log = args.kt / 'evaluation/logs' / f'commit-survival-part3-phase-b-{attempt["workers"]}.log'
            with log.open('a') as stream:
                subprocess.run(['docker', 'logs', container], stdout=stream, stderr=subprocess.STDOUT, check=True)
            attempt['log'] = str(log)
            manifest, moved = quarantine('b')
            archive_reports('b', [r for r in manifest['entries'] if not (EXP / r['source']).exists()])
            write_json(RESULTS / 'phase-b-quarantine.json', manifest)
            if has_oom(attempt, info):
                attempt['oom_detected'] = True
                assert attempt['workers'] == 8, 'Phase B OOM recurred with six workers; quarantined evidence requires inspection'
                preserved = EXP / 'work/phase-b-six-worker-preserved.json'
                if not preserved.exists():
                    write_json(preserved, snapshot_healthy())
                state['workers'] = 6
                state['current'] = None
                record('resuming-with-six-workers')
                continue
            assert not moved, 'Phase B produced signal-9 exits without a reported OOM; verification remains blocked'
            assert info['State']['ExitCode'] == 0 and output.exists(), f'Phase B failed; see {log}'
            done = json.loads(output.read_text())
            assert done['completed'] and all(code == 0 for code in done['worker_exit_codes'])
            for entry in manifest['entries']:
                assert (EXP / entry['source']).exists(), entry['source']
            preserved = EXP / 'work/phase-b-six-worker-preserved.json'
            if preserved.exists():
                healthy = json.loads(preserved.read_text())
                for name, expected in healthy.items():
                    raw = EXP / name
                    assert hashlib.sha256(raw.read_bytes()).hexdigest() == expected['sha256'] and raw.stat().st_mtime_ns == expected['mtime_ns'], name
                state['healthy_artifacts_preserved'] = len(healthy)
            manifest['status'] = 'no signal-9 exits remain; all quarantined records replaced'
            write_json(RESULTS / 'phase-b-quarantine.json', manifest)
            state['current'] = None
            record('complete')
            subprocess.run(['docker', 'rm', container], capture_output=True, check=True)
            return
    except BaseException:
        state['error'] = traceback.format_exc()
        record('needs-inspection')
        raise


if __name__ == '__main__':
    main()
