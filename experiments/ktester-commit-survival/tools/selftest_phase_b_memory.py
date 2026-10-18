"""Exercise Phase B command caps and killed-report retries without running Java."""
import contextlib
import json
from pathlib import Path
import tempfile
from unittest.mock import patch
import run_commit as runner
from raw_integrity import assert_no_sigkill
from survival_common import write_json


def check(phase, killed_report=False):
    with tempfile.TemporaryDirectory() as tmp, contextlib.ExitStack() as stack:
        exp = Path(tmp)
        results = exp / 'results'
        source = exp / 'generated/FooTest.java'
        source.parent.mkdir()
        source.write_text('class FooTest {}\n')
        write_json(exp / 'tools/dev-compile-units.json', {'units': []})
        folder = exp / 'export'
        folder.mkdir()
        row = {'id': 'fixture', 'file': 'generated/FooTest.java', 'class': 'FooTest', 'method': 'test'}
        commands = []
        report_calls = 0
        def execute(command, cwd, log, *args):
            nonlocal report_calls
            argv = [str(arg) for arg in command]
            commands.append(argv)
            log.parent.mkdir(parents=True, exist_ok=True)
            log.write_text('')
            rc = 0
            if 'report' in argv:
                report_calls += 1
                rc = -9 if killed_report and report_calls == 1 else 0
            return {'command': argv, 'exit_code': rc, 'timeout': False,
                    'seconds': .1, 'log': str(log.relative_to(exp))}
        replacements = {'EXP': exp, 'RESULTS': results,
                        'check_inputs': lambda: 'fixed', 'runner_hashes': lambda: {'fixture': 'fixed'},
                        'commits': lambda: {1: {'sha': 'abc', 'date': 'today'}},
                        'denominator': lambda group: [row], 'classpath': lambda: [],
                        'export_build': lambda *args: (folder, {'status': 'built'}),
                        'selectors': lambda *args: {'fixture': ['FooTest#test()']},
                        'parse_reports': lambda *args: ({('FooTest', 'test'): [{'state': 'pass'}]}, []),
                        'relative': lambda p: str(p.relative_to(exp)),
                        'production_line_rows': lambda xml: [], 'execute': execute}
        for name, value in replacements.items():
            stack.enter_context(patch.object(runner, name, value))
        raw = runner.run_group(1, 'llm', 'w1', phase, ['fixture'], phase == 'b')
        javac = [argv for argv in commands if argv[0] == 'javac']
        launchers = [argv for argv in commands if 'org.junit.platform.console.ConsoleLauncher' in argv]
        reports = [argv for argv in commands if 'report' in argv]
        assert len(javac) == len(launchers) == 1
        assert all(('-J-Xmx512m' in argv) == (phase == 'b') for argv in javac)
        for argv in launchers + reports:
            assert ('-Xmx1g' in argv) == (phase == 'b')
            assert ('-XX:+UseSerialGC' in argv) == (phase == 'b')
        assert raw['requested_ids'] == ['fixture'] and raw['methods'][0]['state'] == 'pass'
        if killed_report:
            assert [r['exit_code'] for r in raw['coverage']['report_attempts']] == [-9, 0]
            try:
                assert_no_sigkill(raw, 'successful report retry')
            except AssertionError:
                pass
            else:
                raise AssertionError('A successful retry hid the killed report JVM')
        else:
            assert_no_sigkill(raw, phase)


if __name__ == '__main__':
    check('a')
    check('b')
    check('b', killed_report=True)
    print(json.dumps({'passed': True, 'checks': ['Phase A commands retain defaults',
          'Phase B javac, launcher and report caps', 'alive method selection unchanged',
          'successful report retry retains earlier -9 for verification']}))
