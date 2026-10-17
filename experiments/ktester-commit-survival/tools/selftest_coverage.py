"""Adversarial checks for the revised coverage population and all/scope accounting."""
import json
import contextlib
import io
import tempfile
from pathlib import Path
import aggregate_coverage
from aggregate_coverage import derive_coverage, table3
from coverage_common import SCOPE, file_group, line_rows, measure_lines
from prepare_coverage import changed_files, changed_lines
from survival_common import EXP, RESULTS, write_csv, write_json


def check_output_files():
    """Run the real aggregator on isolated XML/CSV/raw fixtures, never measured results."""
    original = aggregate_coverage.EXP, aggregate_coverage.RESULTS, aggregate_coverage.commits
    with tempfile.TemporaryDirectory() as temporary:
        root = Path(temporary)
        results = root / 'results'
        results.mkdir()
        cm = {i: {'sha': str(i), 'date': '2026-01-01', 'changes-src-main': 'yes' if i else 'no',
                  'changes-scope': 'yes' if i else 'no'} for i in [0, 1]}
        scope = 'org/apache/commons/codec/binary/Base64.java'
        other = 'org/apache/commons/codec/binary/BaseNCodec.java'
        for i in [0, 1]:
            for group in ['dev', 'llm']:
                path = results / f'{i}-{group}.xml'
                sources = ''.join(f'<package name="{file.rsplit("/", 1)[0]}"><sourcefile name="{file.rsplit("/", 1)[1]}"><line nr="1" mi="{int(group == "llm")}" ci="{int(group == "dev")}"/></sourcefile></package>' for file in sorted(SCOPE | {other}))
                path.write_text('<report>' + sources + '</report>')
                entries = line_rows(path)
                csv_path = path.with_suffix('.csv')
                write_csv(csv_path, ['file', 'line', 'instructions-missed', 'instructions-covered'], entries)
                write_json(results / 'raw-coverage' / f'{i}-{group}.json',
                           {'status': 'evaluated', 'coverage': {'xml': str(path.relative_to(root)), 'lines_csv': str(csv_path.relative_to(root))}})
        write_csv(results / 'coverage-commits.csv', ['index', 'sha', 'reason'],
                  [{'index': 0, 'sha': '0', 'reason': 'before-change'}, {'index': 1, 'sha': '1', 'reason': 'changed'}])
        for group in ['dev', 'llm']:
            write_csv(results / f'{group}-survival.csv', ['index', 'evaluated', 'newly_broken_count'],
                      [{'index': i, 'evaluated': 'yes', 'newly_broken_count': int(i == 1 and group == 'dev')} for i in [0, 1]])
        write_json(results / 'changes/1.json', {'subject': 'fixture', 'pre_image': {other: [1]},
                   'post_image': {scope: [1], other: [1, 2]}, 'scope_projection_matches_original_diff': True,
                   'files': [{'old_file': file, 'new_file': file} for file in [scope, other]]})
        (results / 'consistency.md').write_text('# Fixture consistency\n')
        try:
            aggregate_coverage.EXP, aggregate_coverage.RESULTS = root, results
            aggregate_coverage.commits = lambda: cm
            with contextlib.redirect_stdout(io.StringIO()):
                aggregate_coverage.main()
            table = (results / 'table3-changed-line-coverage.md').read_text()
            assert '| 2 (1) | 2 (100.0%) | 0 (0.0%) | 1 | 0 |' in table
            assert len([line for line in table.splitlines() if line.startswith('| Total:')]) == 2
            assert not list(results.glob('*table3b*'))
            baseline = (results / 'coverage-t.md').read_text()
            assert other not in baseline and '| Total | 5 | 5 (100.0%) | 0 (0.0%) |' in baseline
            table4 = (results / 'table4-exposure-vs-break.md').read_text()
            assert table4.count('## DEV') == table4.count('## LLM') == 1
            consistency = json.loads((results / 'coverage-consistency.json').read_text())
            assert consistency['xml_files_parsed'] == 4 and consistency['all_changed_commits_and_predecessors_present']
        finally:
            aggregate_coverage.EXP, aggregate_coverage.RESULTS, aggregate_coverage.commits = original


def main():
    scope = 'org/apache/commons/codec/binary/Base64.java'
    other = 'org/apache/commons/codec/binary/BaseNCodec.java'
    assert file_group(scope) == 'scope' and file_group(other) == 'other'
    assert changed_files('', {}, {}) == []
    diff = ('--- a/src/main/java/Old.java\n+++ b/src/main/java/New.java\n'
            '@@ -4,2 +4,1 @@\n-old\n-old\n+new\n'
            '--- a/src/main/java/Deleted.java\n+++ /dev/null\n@@ -1,2 +0,0 @@\n-a\n-b\n'
            '--- /dev/null\n+++ b/src/main/java/Added.java\n@@ -0,0 +1,1 @@\n+a\n')
    pre, post = changed_lines(diff)
    files = changed_files('R080\0src/main/java/Old.java\0src/main/java/New.java\0'
                          'D\0src/main/java/Deleted.java\0A\0src/main/java/Added.java\0', pre, post)
    assert files[0]['renamed'] and files[0]['pre_image_lines'] == [4, 5] and files[0]['post_image_lines'] == [4]
    assert files[1]['deleted'] and files[1]['new_file'] is None and files[1]['post_image_lines'] == []
    assert files[2]['old_file'] is None and files[2]['post_image_lines'] == [1]
    assert files[0]['group'] == 'other'
    crossing = changed_files(f'R100\0src/main/java/{scope}\0src/main/java/{other}\0', {}, {})[0]
    assert crossing['pre_image_group'] == 'scope' and crossing['post_image_group'] == 'other'
    with tempfile.TemporaryDirectory() as temporary:
        path = Path(temporary) / 'coverage.xml'
        path.write_text('''<report><package name="org/apache/commons/codec/binary">
          <sourcefile name="Base64.java"><line nr="4" mi="0" ci="2"/></sourcefile>
          <sourcefile name="BaseNCodec.java"><line nr="8" mi="3" ci="0"/></sourcefile>
          </package><package name="org/apache/commons/codec"><sourcefile name="Codec.java"><line nr="2" mi="1" ci="0"/></sourcefile></package>
          <package name="org/apache/commons/codecExtra"><sourcefile name="Outside.java"><line nr="1" mi="1" ci="0"/></sourcefile></package></report>''')
        entries = line_rows(path)
        assert len(entries) == 3 and {r['file'] for r in entries} == {scope, other, 'org/apache/commons/codec/Codec.java'}
        assert next(r for r in entries if r['file'] == other)['instructions-covered'] == 0
    # All/scope post-image coverage and pre-image exposure intentionally differ.
    cm = {i: {'index': i, 'sha': str(i), 'date': '2026-01-01', 'changes-src-main': 'yes'} for i in range(1, 7)}
    cm[0] = {'changes-src-main': 'no'}
    changes = {i: {'subject': 'synthetic', 'pre_image': {other: [8]},
                   'post_image': {scope: [4, 5], other: [8]},
                   'files': [{'old_file': scope, 'new_file': scope}, {'old_file': other, 'new_file': other}]} for i in range(1, 7)}
    changes[5]['post_image'] = {}  # Pure deletion: pre-image exposure still matters.
    dev = [{(scope, 4): False, (other, 8): True},
           {(scope, 4): True, (other, 8): False},
           {(scope, 4): False, (other, 8): False},
           {(scope, 4): False, (other, 8): True},
           {(scope, 4): False, (other, 8): False},
           {(scope, 4): False, (other, 8): False}, None]
    llm = [{(scope, 4): False, (other, 8): False},
           {(scope, 4): False, (other, 8): False},
           {(scope, 4): False, (other, 8): True},
           {(scope, 4): True, (other, 8): False},
           {(scope, 4): False, (other, 8): False},
           {(scope, 4): False, (other, 8): False}, None]
    maps = {'dev': dict(enumerate(dev)), 'llm': dict(enumerate(llm))}
    curves = {g: {i: {'newly_broken_count': int(i == 1), 'evaluated': 'no' if i == 6 else 'yes'} for i in range(1, 7)} for g in maps}
    rows, categories, exposure, unknown = derive_coverage(cm, changes, maps, curves)
    assert len(rows) == 6
    assert rows[0]['all_changed_post_image_lines'] == 3 and rows[0]['scope_changed_post_image_lines'] == 2
    assert rows[0]['all_executable_changed_lines'] == 2 and rows[0]['scope_executable_changed_lines'] == 1
    assert rows[0]['all_dev_pre_image_covered'] == 1 and rows[0]['all_llm_pre_image_covered'] == 0
    assert rows[4]['all_executable_changed_lines'] == 0 and rows[4]['all_pre_image_executable_lines'] == 1
    assert rows[5]['all_executable_changed_lines'] == '' and unknown == {'dev': 1, 'llm': 1}
    for category in ['covered by Dev only', 'covered by LLM only', 'covered by both', 'covered by neither group', 'zero executable changed lines', 'unavailable because build failed']:
        assert categories[category] == 1, categories
    assert exposure['dev'][(True, True)] == 1 and exposure['llm'][(False, True)] == 1
    text, totals = table3(rows, categories)
    assert totals == ['| Total: all files | — | — | — | 8 (4) | 2 (25.0%) | 2 (25.0%) | — | — |',
                      '| Total: five scope files only | — | — | — | 4 | 1 (25.0%) | 1 (25.0%) | — | — |']
    assert all(row in text for row in totals)
    assert measure_lines({(scope, 4)}, {'dev': None, 'llm': None})['dev_covered'] == ''
    # Verify expanded extraction preserves every measured Step 0 scope entry.
    from survival_common import line_rows as original_scope_rows
    checks = []
    for raw_path in sorted((RESULTS / 'runner-check-coverage').glob('*.json')):
        raw = json.loads(raw_path.read_text())
        xml = EXP / raw['coverage']['xml']
        expanded = line_rows(xml)
        original = original_scope_rows(xml)
        assert [r for r in expanded if r['file'] in SCOPE] == original
        assert len(expanded) > len(original)
        checks.append({'group': raw['group'], 'all_line_entries': len(expanded), 'scope_line_entries': len(original)})
    assert len(checks) == 2, 'Both measured Step 0 coverage XMLs are required'
    check_output_files()
    result = {'passed': True, 'checks': ['rename/deletion/addition metadata and image line ranges',
              'scope-crossing rename groups', 'codec package boundary and uncovered lines',
              'all/scope executable split excludes non-executable changes', 'pre-image exposure uses predecessor',
              'deletion-only commit remains in categories and exposure', 'build failure is unavailable, not zero',
              'all four coverage categories', 'two independently expected Table 3 totals',
              'expanded XML extraction preserves original scope entries',
              'real aggregation from isolated raw/XML/CSV fixtures emits revised outputs'], 'step0_xml_checks': checks}
    write_json(RESULTS / 'part3-coverage-selftest.json', result)
    print(json.dumps(result, indent=2))


if __name__ == '__main__':
    main()
