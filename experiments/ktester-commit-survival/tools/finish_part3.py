"""Resume-safe host continuation after the already-running Phase A. Never starts Phase A."""
import argparse
import datetime
import fcntl
import hashlib
import importlib
import json
from pathlib import Path
import subprocess
import sys
import time
import traceback
import survival_common as sc

TRAILER = 'Co-authored-by: Codex <noreply@openai.com>'
BRANCH = 'experiment/2026-10-week3-ktester-commit-survival'
B_CONTAINER = 'ktester-commit-survival-phase-b'
SURVIVAL_OUTPUTS = [f'{g}-{kind}.csv' for g in ['dev', 'llm'] for kind in ['survival', 'first-break', 'transient']] + [
    'alive-entering.json', 'table1-survival-by-event.md', 'table2-breaking-commits.md',
    'figure1-survival-by-commit.png', 'figure1-survival-by-commit.svg', 'survival-consistency.json', 'consistency.md']
COVERAGE_OUTPUTS = ['changed-lines.csv', 'table3-changed-line-coverage.md', 'table4-exposure-vs-break.md',
                    'coverage-t.md', 'coverage-consistency.json', 'consistency.md']


def stamp():
    return datetime.datetime.now(datetime.timezone.utc).isoformat()


def container_running(name):
    result = subprocess.run(['docker', 'inspect', '--format', '{{.State.Running}}', name], text=True, capture_output=True)
    return result.returncode == 0 and result.stdout.strip() == 'true'


def check_raw(phase):
    fingerprint = sc.check_inputs()
    cm = sc.commits()
    indices = list(range(1, 540)) if phase == 'a' else [int(r['index']) for r in sc.read_csv(sc.RESULTS / 'coverage-commits.csv')]
    folder = sc.RESULTS / ('raw' if phase == 'a' else 'raw-coverage')
    assert len(list(folder.glob('*.json'))) == 2 * len(indices)
    alive = json.loads((sc.RESULTS / 'alive-entering.json').read_text()) if phase == 'b' else None
    full = {g: {r['id'] for r in sc.denominator(g)} for g in ['dev', 'llm']}
    failures = []
    for i in indices:
        records = {}
        for g in full:
            r = json.loads((folder / f'{i}-{g}.json').read_text())
            assert r['sha'] == cm[i]['sha'] and r['input_sha256'] == fingerprint
            assert r['runner_sha256'] == sc.runner_hashes()
            requested = set(alive[str(i)][g]) if alive is not None else full[g]
            assert set(r['requested_ids']) == requested
            if r['status'] != 'build-fail':
                assert len(r['methods']) == len(requested) and {m['id'] for m in r['methods']} == requested
                assert all(m['state'] in sc.STATES for m in r['methods'])
                if phase == 'b':
                    from coverage_common import line_rows
                    actual = line_rows(sc.EXP / r['coverage']['xml'])
                    saved = [{k: v if k == 'file' else int(v) for k, v in row.items()}
                             for row in sc.read_csv(sc.EXP / r['coverage']['lines_csv'])]
                    assert actual == saved, f'Incomplete production line CSV at {i}-{g}'
            records[g] = r
        assert (records['dev']['status'] == 'build-fail') == (records['llm']['status'] == 'build-fail')
        if records['dev']['status'] == 'build-fail':
            failures.append(i)
    assert len(failures) <= 53
    return {'indices': len(indices), 'raw_files': 2 * len(indices), 'build_fail_indices': failures}


def wire_coverage():
    """Only called after Phase A is verified and committed; preserve its Git version."""
    path = sc.EXP / 'tools/run_commit.py'
    source = path.read_text()
    old = '        lines = line_rows(xml)'
    new = "        lines = production_line_rows(xml) if phase == 'b' else line_rows(xml)"
    if old in source:
        assert source.count(old) == 1
        source = source.replace('import time\n', 'import time\nfrom coverage_common import line_rows as production_line_rows\n', 1).replace(old, new)
        path.write_text(source)
    else:
        assert new in source and 'from coverage_common import line_rows as production_line_rows' in source
    path = sc.EXP / 'tools/survival_common.py'
    source = path.read_text()
    old = "'MethodSelectors.java', 'dev-compile-units.json']"
    new = "'MethodSelectors.java', 'dev-compile-units.json', 'coverage_common.py']"
    if old in source:
        assert source.count(old) == 1
        path.write_text(source.replace(old, new))
    else:
        assert new in source
    importlib.reload(sc)
    plan_path = sc.RESULTS / 'part3-coverage-plan.json'
    plan = json.loads(plan_path.read_text())
    plan.pop('pending_before_phase_b', None)
    plan.update({'status': 'Phase B extraction wired after Phase A completion',
                 'phase_a_runner_version': sc.git('log', '-1', '--format=%H', '--grep=^Phase A: survival states at 539 commits$').strip(),
                 'phase_b_runner_sha256': sc.runner_hashes()})
    sc.write_json(plan_path, plan)


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--kt', type=Path, required=True)
    parser.add_argument('--phase-a-container', default='docker-ktester-run-49e84584d945')
    parser.add_argument('--check', action='store_true', help='Read-only preflight; do not wait, edit, run or commit')
    args = parser.parse_args()
    args.kt = args.kt.resolve()
    state_path = sc.EXP / 'work/part3-continuation.json'
    lock = (sc.EXP / 'work/part3-continuation.lock').open('a')
    fcntl.flock(lock, fcntl.LOCK_EX | fcntl.LOCK_NB)
    state = json.loads(state_path.read_text()) if state_path.exists() else {'created': stamp(), 'steps': {}}
    def record(stage, **values):
        state.update({'stage': stage, 'updated': stamp(), **values})
        sc.write_json(state_path, state)
        print(json.dumps({'stage': stage, **values}), flush=True)
    def preflight():
        assert sc.git('branch', '--show-current').strip() == BRANCH
        assert not sc.git('diff', '--cached', '--name-only').strip(), 'Unrelated staged changes require review'
        sc.check_inputs()
        plan = json.loads((sc.RESULTS / 'part3-coverage-plan.json').read_text())
        addendum = args.kt / 'docs/cc-instructions-part3-addendum-commit-survival.md'
        assert hashlib.sha256(addendum.read_bytes()).hexdigest() == plan['addendum_sha256'], 'Updated addendum needs review before continuing'
        return plan
    def run(command, label, cwd=sc.EXP):
        log = args.kt / 'evaluation/logs' / f'commit-survival-part3-{label}.log'
        log.parent.mkdir(parents=True, exist_ok=True)
        record(label, log=str(log))
        with log.open('a') as stream:
            result = subprocess.run(command, cwd=cwd, stdout=stream, stderr=subprocess.STDOUT)
        assert result.returncode == 0, f'{label} failed ({result.returncode}); see {log}'
    def container_command(command, label, name=None):
        argv = ['docker', 'compose', '-f', 'compose.yml', '-f', 'compose.commit-survival.yml', 'run', '--rm']
        if name:
            argv += ['--name', name]
        argv += ['ktester', 'bash', '-c', 'cd /repo/experiments/ktester-commit-survival && ' + command]
        run(argv, label, args.kt / 'docker')
    def commit(message, paths):
        assert not sc.git('diff', '--cached', '--name-only').strip(), 'Unrelated staged changes require review'
        sc.git('add', '--', *[str(p.relative_to(sc.REPO)) for p in paths])
        sc.git('diff', '--cached', '--check')
        if sc.git('diff', '--cached', '--name-only').strip():
            sc.git('commit', '-q', '-m', message, '-m', TRAILER)
        else:
            assert sc.git('log', '-1', '--format=%s').strip() == message, f'Nothing to commit for {message}'
        assert sc.git('log', '-1', '--format=%B').count(TRAILER) == 1
        return sc.git('rev-parse', 'HEAD').strip()
    def step(key, action):
        if key not in state['steps']:
            result = action()
            state['steps'][key] = {'finished': stamp(), 'result': result}
            record(key + '-complete')
        return state['steps'][key]['result']
    def wait_phase(phase, name):
        path = sc.RESULTS / f'phase-{phase}.json'
        previous_count = None
        while not path.exists():
            assert container_running(name), f'Phase {phase.upper()} container ended without a completion record'
            folder = sc.RESULTS / ('raw' if phase == 'a' else 'raw-coverage')
            count = len(list(folder.glob('*.json')))
            if count != previous_count:
                record('waiting-phase-' + phase, raw_records=count)
                previous_count = count
            time.sleep(30)
        result = json.loads(path.read_text())
        assert result['completed'] and all(code == 0 for code in result['worker_exit_codes']), f'Phase {phase.upper()} requires inspection'
        return check_raw(phase)
    try:
        plan = preflight()
        if args.check:
            print(json.dumps({'preflight': 'passed', 'phase_a_container_running': container_running(args.phase_a_container),
                              'coverage_commits': plan['coverage_commits'], 'steps_completed': list(state['steps'])}))
            return
        step('phase-a-verified', lambda: wait_phase('a', args.phase_a_container))
        step('phase-a-committed', lambda: commit('Phase A: survival states at 539 commits',
             [sc.RESULTS / p for p in ['raw', 'builds/a', 'phase-a.json', 'phase-a-invocations']]))
        step('survival-aggregated', lambda: container_command('python tools/aggregate_survival.py', 'aggregate-survival'))
        step('survival-committed', lambda: commit('Survival tables', [sc.RESULTS / p for p in SURVIVAL_OUTPUTS]))
        step('coverage-wired', wire_coverage)
        # Reload also when resuming after the wiring step was already recorded.
        importlib.reload(sc)
        step('coverage-helper-checked', lambda: run([sys.executable, 'tools/selftest_coverage.py'], 'coverage-selftest'))
        def phase_b():
            preflight()
            output = sc.RESULTS / 'phase-b.json'
            if not output.exists() and not container_running(B_CONTAINER):
                container_command('python tools/run_phase.py --phase b --workers 4 --start 0 --end 539', 'phase-b', B_CONTAINER)
            return wait_phase('b', B_CONTAINER)
        step('phase-b-verified', phase_b)
        def coverage_plan_done():
            path = sc.RESULTS / 'part3-coverage-plan.json'
            data = json.loads(path.read_text())
            data['status'] = 'Phase B complete and verified'
            data['coverage_commits_actually_run'] = state['steps']['phase-b-verified']['result']['indices']
            sc.write_json(path, data)
        step('coverage-plan-finalized', coverage_plan_done)
        step('phase-b-committed', lambda: commit(f"Phase B: line coverage at {plan['coverage_commits']} commits",
             [sc.RESULTS / p for p in ['raw-coverage', 'coverage', 'builds/b', 'phase-b.json', 'phase-b-invocations',
                                      'part3-coverage-plan.json', 'part3-coverage-selftest.json']] +
             [sc.EXP / 'tools' / p for p in ['run_commit.py', 'survival_common.py']]))
        # Read the instruction file again immediately before Aggregation 2; a changed digest requires review.
        preflight()
        step('coverage-aggregated', lambda: run([sys.executable, 'tools/aggregate_coverage.py'], 'aggregate-coverage'))
        step('coverage-committed', lambda: commit('Changed-line coverage tables', [sc.RESULTS / p for p in COVERAGE_OUTPUTS]))
        step('handover-written', lambda: run([sys.executable, 'tools/report_part3.py', '--kt', str(args.kt)], 'handover'))
        sc.check_inputs()
        record('complete', commons_codec_head=sc.git('rev-parse', 'HEAD').strip(),
               handover=str(sc.EXP / 'docs/handover-part3.md'))
    except BaseException:
        record('needs-inspection', error=traceback.format_exc())
        raise


if __name__ == '__main__':
    main()
