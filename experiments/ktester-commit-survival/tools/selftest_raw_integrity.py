"""Isolated regression checks for quarantine, verification and the OOM fallback."""
import contextlib
import copy
import hashlib
import io
import json
from pathlib import Path
import tempfile
from types import SimpleNamespace
from unittest.mock import patch
import finish_part3
import raw_integrity as integrity
import repair_phase_a as repair
import run_coverage_guarded as coverage
from survival_common import write_json


def rejects(action, message=None):
    try:
        action()
    except AssertionError as error:
        if message:
            assert message in str(error), str(error)
    else:
        raise AssertionError('Expected verification to reject the fixture')


def main():
    bad = {'index': 69, 'group': 'dev', 'files': [{'runs': [{'exit_code': -9, 'timeout': False}]}], 'methods': []}
    assert len(integrity.sigkill_exits(bad)) == 1
    rejects(lambda: integrity.assert_no_sigkill(bad, 'fixture'), 'signal-9')
    rejects(lambda: integrity.assert_no_sigkill({'methods': [{'retry': {'exit_code': -9}}]}, 'method'), 'signal-9')
    integrity.assert_no_sigkill({'files': [{'runs': [{'exit_code': 0}, {'exit_code': 1}]}]}, 'healthy')
    with tempfile.TemporaryDirectory() as tmp, contextlib.ExitStack() as stack:
        exp = Path(tmp)
        results = exp / 'results'
        (exp / 'work').mkdir()
        for module in [integrity, repair, coverage]:
            stack.enter_context(patch.object(module, 'EXP', exp))
            stack.enter_context(patch.object(module, 'RESULTS', results))
        good = {'index': 70, 'group': 'dev', 'files': [], 'methods': []}
        write_json(results / 'raw/69-dev.json', bad)
        write_json(results / 'raw/70-dev.json', good)
        original = (results / 'raw/69-dev.json').read_bytes()
        healthy = (results / 'raw/70-dev.json').read_bytes()
        mtime = (results / 'raw/70-dev.json').stat().st_mtime_ns
        manifest, moved = integrity.quarantine('a')
        assert len(moved) == 1 and not (results / 'raw/69-dev.json').exists()
        assert (exp / moved[0]['quarantine']).read_bytes() == original
        assert moved[0]['sha256'] == hashlib.sha256(original).hexdigest()
        assert (results / 'raw/70-dev.json').read_bytes() == healthy
        assert (results / 'raw/70-dev.json').stat().st_mtime_ns == mtime
        assert not integrity.quarantine('a')[1]
        repaired = copy.deepcopy(good)
        repaired['methods'] = [{'class': 'org.apache.commons.codec.digest.MurmurHash3Test', 'method': str(i),
                                'state': ['pass', 'fail', 'error'][i % 3]} for i in range(27)]
        write_json(results / 'raw/69-dev.json', repaired)
        assert repair.verify_repaired(manifest['entries'])[0]['murmur_methods_evaluated'] == 27
        repaired['methods'][0]['state'] = 'absent'
        write_json(results / 'raw/69-dev.json', repaired)
        rejects(lambda: repair.verify_repaired(manifest['entries']))
        # Reach the real check_raw guard with a complete-sized raw fixture.
        for i in range(1, 540):
            for group in ['dev', 'llm']:
                write_json(results / 'raw' / f'{i}-{group}.json', bad if (i, group) == (1, 'dev') else {})
        with patch.object(finish_part3.sc, 'RESULTS', results), patch.object(finish_part3.sc, 'check_inputs', return_value='fixed'), \
             patch.object(finish_part3.sc, 'commits', return_value={}), patch.object(finish_part3.sc, 'denominator', return_value=[]):
            rejects(lambda: finish_part3.check_raw('a'), 'signal-9')
        # Exercise both full Phase B attempts using a fake Docker boundary. The
        # real controller must remove the killed raw/XML/CSV, then preserve the
        # healthy group while resuming with two workers.
        kt = exp / 'kt'
        (kt / 'evaluation/logs').mkdir(parents=True)
        containers = {}
        commands = []
        group = {'index': 1, 'group': 'llm', 'files': [], 'methods': []}
        write_json(results / 'raw-coverage/1-llm.json', group)
        good_bytes = (results / 'raw-coverage/1-llm.json').read_bytes()
        bad_b = copy.deepcopy(bad)
        bad_b.update({'index': 1, 'coverage': {'xml': 'results/coverage/1-dev.xml', 'lines_csv': 'results/coverage/1-dev-lines.csv'}})
        write_json(results / 'raw-coverage/1-dev.json', bad_b)
        (results / 'coverage').mkdir()
        (results / 'coverage/1-dev.xml').write_text('<report/>')
        (results / 'coverage/1-dev-lines.csv').write_text('file,line\n')
        def fake_run(argv, **kwargs):
            if argv[1] == 'compose':
                command = argv[-1]
                commands.append(command)
                workers = 4 if '--workers 4' in command else 2
                cid = 'fake-' + str(workers)
                containers[cid] = {'Id': cid, 'State': {'Running': workers == 4, 'OOMKilled': False, 'ExitCode': 1 if workers == 4 else 0}}
                containers[coverage.NAME] = containers[cid]
                done = {'completed': workers == 2, 'worker_exit_codes': [0] * workers if workers == 2 else [-15] * workers}
                write_json(results / 'phase-b.json', done)
                write_json(results / 'phase-b-invocations' / f'{workers}.json', done)
                if workers == 2:
                    assert not (results / 'raw-coverage/1-dev.json').exists()
                    assert not (results / 'coverage/1-dev.xml').exists()
                    assert not (results / 'coverage/1-dev-lines.csv').exists()
                    assert (results / 'raw-coverage/1-llm.json').read_bytes() == good_bytes
                    write_json(results / 'raw-coverage/1-dev.json', group)
            elif argv[1] == 'exec':
                containers[argv[2]]['State']['Running'] = False
            elif argv[1] == 'rm':
                containers.pop(argv[2])
                containers.pop(coverage.NAME, None)
            else:
                assert argv[1] in {'wait', 'logs'}, argv
            return SimpleNamespace(returncode=0, stdout='', stderr='')
        with patch.object(coverage, 'inspect', side_effect=lambda name: containers.get(name)), \
             patch.object(coverage, 'memory_events', return_value={'oom_kill': 1}), \
             patch.object(coverage, 'oom_events', return_value=[]), \
             patch.object(coverage, 'check_inputs', return_value='fixed'), \
             patch.object(coverage.subprocess, 'run', side_effect=fake_run), \
             patch('sys.argv', ['run_coverage_guarded.py', '--kt', str(kt)]), contextlib.redirect_stdout(io.StringIO()):
            coverage.main()
        state = json.loads((results / 'phase-b-resource-events.json').read_text())
        assert state['stage'] == 'complete' and [a['workers'] for a in state['attempts']] == [4, 2]
        assert state['attempts'][0]['oom_detected'] and state['healthy_artifacts_preserved'] == 1
        assert len(commands) == 2 and '--workers 4' in commands[0] and '--workers 2' in commands[1]
        assert (results / 'raw-coverage/1-llm.json').read_bytes() == good_bytes
        assert coverage.has_oom({'memory_events': {'oom_kill': 1}}, {'State': {}})
        assert not coverage.has_oom({}, {'State': {}})
    print(json.dumps({'passed': True, 'checks': ['recursive signal-9 rejection', 'check_raw signal-9 guard',
          'byte-preserving quarantine', '27 evaluated MurmurHash3Test methods', 'Phase B OOM four-to-two resume',
          'quarantined coverage artifacts', 'healthy raw preservation']}))


if __name__ == '__main__':
    main()
