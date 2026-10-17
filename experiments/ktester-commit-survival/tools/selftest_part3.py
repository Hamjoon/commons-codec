"""Small adversarial checks for measurement semantics, not frozen test changes."""
import json
import sys
from survival_common import EXP, RESULTS, execute, method_state, parse_reports, write_json
from aggregate_survival import derive
from prepare_coverage import changed_lines


def main():
    work = EXP / 'work/part3-selftest'
    work.mkdir(parents=True, exist_ok=True)
    xml = '''<testsuite><testcase classname="A" name="ok()"/>
    <testcase classname="A" name="assertion()"><failure type="org.opentest4j.AssertionFailedError" message="expected"/></testcase>
    <testcase classname="A" name="exception()"><error type="java.lang.IllegalArgumentException" message="bad"/></testcase>
    <testcase classname="A" name="disabled()"><skipped/></testcase>
    <testcase classname="A" name="[1] x"><system-out>[test-template:param(java.lang.String)]</system-out></testcase>
    <testcase classname="A" name="[2] y"><system-out>[test-template:param(java.lang.String)]</system-out><failure type="java.lang.AssertionError"/></testcase>
    </testsuite>'''
    (work / 'TEST-synthetic.xml').write_text(xml)
    observations, cases = parse_reports(work)
    run = {'timeout': False}
    for name, expected in [('ok', 'pass'), ('assertion', 'fail'), ('exception', 'error'), ('disabled', 'absent'), ('param', 'fail'), ('missing', 'absent')]:
        assert method_state({'class': 'A', 'method': name}, observations, run)[0] == expected
    assert method_state({'class': 'A', 'method': 'ok'}, observations, {'timeout': True})[0] == 'timeout'
    process = execute([sys.executable, '-c', 'import time; time.sleep(60)'], work, work / 'timeout.txt', .1)
    assert process['timeout'] and process['exit_code'] != 0
    cm = {i: {'index': i, 'sha': str(i), 'date': '2026-01-01'} for i in range(4)}
    rows = [{'id': name, 'file': 'f', 'class': 'C', 'method': name} for name in ['a', 'b']]
    def record(index, states):
        return {'sha': str(index), 'status': 'evaluated', 'methods': [{'id': name, 'state': state, 'message': state, 'flaky': name == 'b' and index == 1} for name, state in zip(['a', 'b'], states)]}
    raw = {g: {1: record(1, ['compile-fail', 'pass']), 2: {'sha': '2', 'status': 'build-fail', 'methods': []}, 3: record(3, ['pass', 'error'])} for g in ['dev', 'llm']}
    curves, breaks, transient, alive, failed, flaky = derive(cm, raw, {g: rows for g in raw})
    assert failed == [2] and flaky == 2
    for group in raw:
        assert [r['alive_count'] for r in curves[group]] == [2, 1, 1, 0]
        assert [r['first_failing_index'] for r in breaks[group]] == [1, 3]
        assert transient[group][0]['failed_index'] == 1 and transient[group][0]['later_pass_index'] == 3
        assert alive['3'][group] == ['b']
    pre, post = changed_lines('--- a/src/main/java/A.java\n+++ b/src/main/java/A.java\n@@ -3,2 +3,1 @@\n-a\n-b\n+c\n@@ -8,0 +8,2 @@\n+x\n+y\n@@ -12,1 +13,0 @@\n-z\n')
    assert pre == {'A.java': [3, 4, 12]} and post == {'A.java': [3, 8, 9]}
    result = {'passed': True, 'checks': ['assertion versus exception', 'parameterized invocation reduction', 'absent and skipped methods',
              'class-timeout dominance and process termination', 'build-fail carryover', 'cumulative survival and first-break kinds',
              'transient recovery remains cumulatively lost', 'alive-entering off-by-one', 'flaky history', 'zero-context insertion/deletion/replacement line ranges']}
    write_json(RESULTS / 'part3-selftest.json', result)
    print(json.dumps(result, indent=2))


if __name__ == '__main__':
    main()
