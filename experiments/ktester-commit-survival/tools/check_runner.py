"""Finish the hard t gate and smoke-check full-class and empty-selection coverage."""
import collections
import json
from run_commit import run_group
from survival_common import EXP, RESULTS, SCOPE_FILES, check_inputs, denominator, line_rows, write_json


def main():
    checks = {}
    for group, expected in [('dev', 143), ('llm', 290)]:
        raw = json.loads((RESULTS / 'runner-check' / f'0-{group}.json').read_text())
        differences = [r for r in raw['methods'] if r['state'] != 'pass']
        checks[group] = {'files': len(raw['files']), 'expected_methods': expected,
                         'reported_methods': len(raw['methods']), 'states': dict(collections.Counter(r['state'] for r in raw['methods'])),
                         'differences': differences, 'seconds': raw['seconds']}
        if raw['status'] != 'evaluated' or len(raw['methods']) != expected or differences:
            write_json(RESULTS / 'part3-runner-check.json', {'passed': False, 'checks': checks})
            raise SystemExit('STOP: runner at t differs from frozen denominator; no loop is permitted')
    coverage = {}
    for group in ['dev', 'llm']:
        rows = denominator(group)
        filename = sorted({r['file'] for r in rows})[0]
        ids = [r['id'] for r in rows if r['file'] == filename]
        result = run_group(0, group, 'w0', 'check-coverage', ids, True, True)
        lines = line_rows(EXP / result['coverage']['xml'])
        assert {r['file'] for r in lines} == {p.removeprefix('src/main/java/') for p in SCOPE_FILES}
        assert any(r['instructions-covered'] > 0 for r in lines)
        coverage[group] = result['coverage']
    empty = run_group(0, 'dev', 'w0', 'check-empty', [], True)
    assert empty['coverage']['line_entries'] > 0 and empty['coverage']['covered_lines'] == 0
    result = {'passed': True, 'input_sha256': check_inputs(), 'checks': checks, 'coverage': coverage,
              'empty_alive_set_coverage': empty['coverage'], 'additional_developer_jars': [],
              'selection': 'Phase A developer classes; LLM frozen denominator methods selected by compiled annotated signatures. Phase B alive-entering methods only, with recorded class fallback if launcher rejects a selector.'}
    write_json(RESULTS / 'part3-runner-check.json', result)
    print(json.dumps(result, indent=2))


if __name__ == '__main__':
    main()
