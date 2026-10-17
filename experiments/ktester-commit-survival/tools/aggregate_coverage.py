"""All-production changed-line coverage, with the five-file scope split."""
import collections
import json
from survival_common import EXP, RESULTS, commits, read_csv, write_csv, write_json
from coverage_common import SCOPE, file_group, flatten, line_rows, measure_lines
from aggregate_survival import md, pct

GROUPS = ['dev', 'llm']
CATEGORIES = ['production-changing commits total', 'zero executable changed lines',
              'covered by neither group', 'covered by Dev only', 'covered by LLM only', 'covered by both']


def line_map(index, group):
    raw = json.loads((RESULTS / 'raw-coverage' / f'{index}-{group}.json').read_text())
    if raw['status'] == 'build-fail':
        return None
    rows = line_rows(EXP / raw['coverage']['xml'])
    csv_rows = read_csv(EXP / raw['coverage']['lines_csv'])
    assert len(rows) == len(csv_rows), f'Incomplete all-production line CSV at {index}-{group}'
    result = {}
    for row, saved in zip(rows, csv_rows):
        assert row == {k: (v if k == 'file' else int(v)) for k, v in saved.items()}
        if row['instructions-missed'] + row['instructions-covered'] > 0:
            result[(row['file'], row['line'])] = row['instructions-covered'] > 0
    return result


def derive_coverage(cm, changes, maps, curves):
    rows, categories = [], collections.Counter()
    exposure = {g: collections.Counter() for g in GROUPS}
    unknown_exposure = collections.Counter()
    for i, commit in cm.items():
        if commit.get('changes-src-main') != 'yes':
            continue
        change = changes[i]
        post, pre = flatten(change['post_image']), flatten(change['pre_image'])
        current, previous = ({g: maps[g][index] for g in GROUPS} for index in [i, i - 1])
        row = {'index': i, 'sha': commit['sha'], 'date': commit['date'], 'subject': change['subject'],
               'files_changed': ';'.join(sorted({f[k] for f in change['files'] for k in ['old_file', 'new_file'] if f[k]}))}
        for label, changed in [('all', post), ('scope', {p for p in post if file_group(p[0]) == 'scope'})]:
            measured = measure_lines(changed, current)
            row[label + '_changed_post_image_lines'] = measured['changed_lines']
            row[label + '_executable_changed_lines'] = measured['executable_lines']
            for g in GROUPS:
                row[label + '_' + g + '_covered'] = measured[g + '_covered']
        measured = measure_lines(pre, previous)
        row['all_pre_image_changed_lines'] = measured['changed_lines']
        row['all_pre_image_executable_lines'] = measured['executable_lines']
        for g in GROUPS:
            row[g + '_broken'] = int(curves[g][i]['newly_broken_count'])
            row['all_' + g + '_pre_image_covered'] = measured[g + '_covered']
            if measured[g + '_covered'] == '' or current[g] is None or curves[g][i]['evaluated'] == 'no':
                unknown_exposure[g] += 1
            else:
                exposure[g][(measured[g + '_covered'] > 0, row[g + '_broken'] > 0)] += 1
        categories[CATEGORIES[0]] += 1
        if any(row[k] == '' for k in ['all_executable_changed_lines', 'all_dev_covered', 'all_llm_covered']):
            categories['unavailable because build failed'] += 1
        elif row['all_executable_changed_lines'] == 0:
            categories[CATEGORIES[1]] += 1
        else:
            dev, llm = row['all_dev_covered'] > 0, row['all_llm_covered'] > 0
            categories['covered by both' if dev and llm else 'covered by Dev only' if dev else 'covered by LLM only' if llm else 'covered by neither group'] += 1
        rows.append(row)
    assert sum(categories[k] for k in CATEGORIES[1:]) + categories['unavailable because build failed'] == len(rows)
    for g in GROUPS:
        assert sum(exposure[g].values()) + unknown_exposure[g] == len(rows)
    return rows, categories, exposure, unknown_exposure


def coverage_pct(count, total):
    return pct(count, total) if total else '0 (n/a)'


def table3(rows, categories):
    lines = ['# Table 3. Changed-line coverage', '',
             '| Index | SHA | Date | Change | Executable changed lines: all (of which scope) | Dev covered | LLM covered | Dev broken | LLM broken |',
             '|---:|---|---|---|---:|---:|---:|---:|---:|']
    visible = [r for r in rows if isinstance(r['all_executable_changed_lines'], int) and r['all_executable_changed_lines'] > 0]
    for row in visible:
        n = row['all_executable_changed_lines']
        covered = {g: coverage_pct(row['all_' + g + '_covered'], n) if row['all_' + g + '_covered'] != '' else 'unavailable' for g in GROUPS}
        lines.append(f"| {row['index']} | {row['sha'][:7]} | {row['date'][:10]} | {md(row['subject'])} | {n} ({row['scope_executable_changed_lines']}) | {covered['dev']} | {covered['llm']} | {row['dev_broken']} | {row['llm_broken']} |")
    # Shared measured population gives both groups the same totals denominator.
    known = [r for r in visible if r['all_dev_covered'] != '' and r['all_llm_covered'] != '']
    total_rows = []
    for label, description in [('all', 'all files'), ('scope', 'five scope files only')]:
        n = sum(r[label + '_executable_changed_lines'] for r in known)
        covered = {g: coverage_pct(sum(r[label + '_' + g + '_covered'] for r in known), n) for g in GROUPS}
        number = f"{n} ({sum(r['scope_executable_changed_lines'] for r in known)})" if label == 'all' else str(n)
        total_rows.append(f"| Total: {description} | — | — | — | {number} | {covered['dev']} | {covered['llm']} | — | — |")
    lines += total_rows + ['', '; '.join(f'{key}: {categories[key]}' for key in CATEGORIES) + '.']
    if categories['unavailable because build failed']:
        lines += ['', f"Unavailable because build failed: {categories['unavailable because build failed']}. Totals exclude unavailable measurements for both groups."]
    return '\n'.join(lines) + '\n', total_rows


def main():
    cm = commits()
    changed = [i for i, r in cm.items() if r.get('changes-src-main') == 'yes']
    indices = [int(r['index']) for r in read_csv(RESULTS / 'coverage-commits.csv')]
    assert len(indices) == len(set(indices))
    assert set(indices) == set(changed) | {i - 1 for i in changed}
    maps = {g: {i: line_map(i, g) for i in indices} for g in GROUPS}
    executable_matches = []
    for i in indices:
        if maps['dev'][i] is not None and maps['llm'][i] is not None:
            assert maps['dev'][i].keys() == maps['llm'][i].keys(), f'Executable lines differ at {i}'
            executable_matches.append(i)
    curves = {g: {int(r['index']): r for r in read_csv(RESULTS / f'{g}-survival.csv')} for g in GROUPS}
    changes = {i: json.loads((RESULTS / 'changes' / f'{i}.json').read_text()) for i in changed}
    assert all(c['scope_projection_matches_original_diff'] for c in changes.values())
    rows, categories, exposure, unknown_exposure = derive_coverage(cm, changes, maps, curves)
    fields = ['index', 'sha', 'date', 'subject', 'files_changed']
    for label in ['all', 'scope']:
        fields += [label + '_' + k for k in ['changed_post_image_lines', 'executable_changed_lines', 'dev_covered', 'llm_covered']]
    fields += ['dev_broken', 'llm_broken', 'all_pre_image_changed_lines', 'all_pre_image_executable_lines', 'all_dev_pre_image_covered', 'all_llm_pre_image_covered']
    write_csv(RESULTS / 'changed-lines.csv', fields, rows)
    text, total_rows = table3(rows, categories)
    (RESULTS / 'table3-changed-line-coverage.md').write_text(text)
    lines = ['# Table 4. Pre-image exposure versus first break', '',
             'Repository-only supporting table. Exposure means at least one executable pre-image changed line in any production file was covered at c−1. Break means at least one previously surviving method first broke at c.', '']
    for g in GROUPS:
        lines += [f'## {g.upper()}', '', '| Covered pre-image changed line | Any method broken | No method broken |', '|---|---:|---:|',
                  f"| Yes | {exposure[g][(True, True)]} | {exposure[g][(True, False)]} |",
                  f"| No | {exposure[g][(False, True)]} | {exposure[g][(False, False)]} |", '',
                  f'Unavailable because c or c−1 failed to build: {unknown_exposure[g]}.', '']
    (RESULTS / 'table4-exposure-vs-break.md').write_text('\n'.join(lines) + '\n')
    lines = ['# Coverage at t (full frozen sets)', '', '| Scope source file | Executable lines | Dev covered | LLM covered |', '|---|---:|---:|---:|']
    baseline = {}
    assert maps['dev'][0] is not None and maps['llm'][0] is not None
    for file in sorted(SCOPE):
        keys = [key for key in maps['dev'][0] if key[0] == file]
        assert keys, f'Missing baseline scope file {file}'
        counts = {g: sum(maps[g][0][key] for key in keys) for g in GROUPS}
        baseline[file] = {'executable_lines': len(keys), **counts}
        lines.append(f"| {file} | {len(keys)} | {coverage_pct(counts['dev'], len(keys))} | {coverage_pct(counts['llm'], len(keys))} |")
    total = sum(r['executable_lines'] for r in baseline.values())
    lines.append(f"| Total | {total} | {coverage_pct(sum(r['dev'] for r in baseline.values()), total)} | {coverage_pct(sum(r['llm'] for r in baseline.values()), total)} |")
    (RESULTS / 'coverage-t.md').write_text('\n'.join(lines) + '\n')
    missing = [{'index': i, 'group': g} for g in GROUPS for i, value in maps[g].items() if value is None]
    consistency = {'coverage_commits': len(indices), 'production_changing_commits': len(rows),
                   'scope_changing_commits': sum(r.get('changes-scope') == 'yes' for r in cm.values()),
                   'xml_files_parsed': 2 * len(indices) - len(missing), 'missing_xml_build_fail': missing,
                   'executable_line_sets_identical_between_groups': executable_matches,
                   'all_changed_commits_and_predecessors_present': not missing,
                   'scope_projection_matches_original_diff': True,
                   'categories': dict(categories), 'baseline': baseline, 'table3_totals_verbatim': total_rows}
    write_json(RESULTS / 'coverage-consistency.json', consistency)
    p = RESULTS / 'consistency.md'
    prior = p.read_text().split('\n## Coverage consistency')[0].rstrip()
    p.write_text(prior + '\n\n## Coverage consistency\n\n' + '\n'.join([
        f'- Production-changing commits checked: {len(rows)}; coverage commits: {len(indices)}.',
        f'- Every required c and c−1 has XML: {not missing}; unavailable build-fail records: {missing}.',
        f'- Both groups have identical executable line sets for every reported production file at each jointly built commit: {len(executable_matches)} passed.',
        '- All codec-package XML line records and committed line CSVs agree exactly: passed.',
        '- Scope line projections equal the original five-file diff definition at every production-changing commit: passed.',
        f'- Each group’s single all-lines exposure table accounts for all {len(rows)} production-changing commits, including explicitly unavailable cases: passed.'
    ]) + '\n')
    print(json.dumps({k: v for k, v in consistency.items() if k not in {'baseline', 'executable_line_sets_identical_between_groups'}}, indent=2))


if __name__ == '__main__':
    main()
