"""Derive executable changed-line exposure from JaCoCo XML and exact git hunks."""
import collections
import json
from survival_common import EXP, RESULTS, SCOPE_FILES, commits, line_rows, read_csv, write_csv, write_json
from aggregate_survival import md, pct


def line_map(index, group):
    raw = json.loads((RESULTS / 'raw-coverage' / f'{index}-{group}.json').read_text())
    if raw['status'] == 'build-fail':
        return None
    xml = EXP / raw['coverage']['xml']
    rows = line_rows(xml)
    csv_rows = read_csv(EXP / raw['coverage']['lines_csv'])
    assert len(rows) == len(csv_rows)
    result = {}
    for row, saved in zip(rows, csv_rows):
        assert row == {k: (v if k == 'file' else int(v)) for k, v in saved.items()}
        if row['instructions-missed'] + row['instructions-covered'] > 0:
            result[(row['file'], row['line'])] = row['instructions-covered'] > 0
    return result


def flatten(mapping):
    return {(file, number) for file, numbers in mapping.items() for number in numbers}


def main():
    cm = commits()
    indices = [int(r['index']) for r in read_csv(RESULTS / 'coverage-commits.csv')]
    maps = {g: {i: line_map(i, g) for i in indices} for g in ['dev', 'llm']}
    executable_matches = []
    for i in indices:
        if maps['dev'][i] is not None and maps['llm'][i] is not None:
            assert maps['dev'][i].keys() == maps['llm'][i].keys(), f'Executable lines differ at {i}'
            executable_matches.append(i)
    curves = {g: {int(r['index']): r for r in read_csv(RESULTS / f'{g}-survival.csv')} for g in maps}
    rows = []
    categories = collections.Counter()
    exposure = {g: collections.Counter() for g in maps}
    unknown_exposure = collections.Counter()
    for i, commit in cm.items():
        if commit.get('changes-scope') != 'yes':
            continue
        change = json.loads((RESULTS / 'changes' / f'{i}.json').read_text())
        post, pre = flatten(change['post_image']), flatten(change['pre_image'])
        current = maps['dev'][i]
        previous = maps['dev'][i - 1]
        executable = post & current.keys() if current is not None else None
        pre_executable = pre & previous.keys() if previous is not None else None
        row = {'index': i, 'sha': commit['sha'], 'date': commit['date'], 'subject': change['subject'],
               'files_changed': ';'.join(sorted(set(change['pre_image']) | set(change['post_image']))),
               'changed_post_image_lines': len(post), 'executable_changed_lines': len(executable) if executable is not None else '',
               'pre_image_changed_lines': len(pre), 'pre_image_executable_lines': len(pre_executable) if pre_executable is not None else ''}
        categories['scope-changing commits total'] += 1
        if executable is None:
            categories['unavailable because build failed'] += 1
        elif not executable:
            categories['zero executable changed lines'] += 1
        for group in maps:
            m, before = maps[group][i], maps[group][i - 1]
            row[f'{group}_covered'] = sum(m[p] for p in executable) if executable is not None and m is not None else ''
            row[f'{group}_pre_image_covered'] = sum(before[p] for p in pre_executable) if pre_executable is not None and before is not None else ''
            row[f'{group}_broken'] = int(curves[group][i]['newly_broken_count'])
            if row[f'{group}_pre_image_covered'] == '' or curves[group][i]['evaluated'] == 'no':
                unknown_exposure[group] += 1
            else:
                exposure[group][(row[f'{group}_pre_image_covered'] > 0, row[f'{group}_broken'] > 0)] += 1
        if executable:
            dev, llm = row['dev_covered'] > 0, row['llm_covered'] > 0
            categories['covered by both' if dev and llm else 'covered by Dev only' if dev else 'covered by LLM only' if llm else 'covered by neither group'] += 1
        rows.append(row)
    fields = ['index', 'sha', 'date', 'subject', 'files_changed', 'changed_post_image_lines', 'executable_changed_lines',
              'dev_covered', 'llm_covered', 'dev_broken', 'llm_broken', 'pre_image_changed_lines', 'pre_image_executable_lines', 'dev_pre_image_covered', 'llm_pre_image_covered']
    write_csv(RESULTS / 'changed-lines.csv', fields, rows)
    lines = ['# Table 3. Changed-line coverage', '', '| Index | SHA | Date | Change | Executable changed lines | Dev covered | LLM covered | Dev broken | LLM broken |', '|---:|---|---|---|---:|---:|---:|---:|---:|']
    visible = [r for r in rows if isinstance(r['executable_changed_lines'], int) and r['executable_changed_lines'] > 0]
    for row in visible:
        n = row['executable_changed_lines']
        lines.append(f"| {row['index']} | {row['sha'][:7]} | {row['date'][:10]} | {md(row['subject'])} | {n} | {pct(row['dev_covered'], n)} | {pct(row['llm_covered'], n)} | {row['dev_broken']} | {row['llm_broken']} |")
    n = sum(r['executable_changed_lines'] for r in visible)
    coverage_pct = lambda g: pct(sum(r[f'{g}_covered'] for r in visible), n) if n else '0 (n/a)'
    lines += [f"| Total | — | — | — | {n} | {coverage_pct('dev')} | {coverage_pct('llm')} | {sum(r['dev_broken'] for r in visible)} | {sum(r['llm_broken'] for r in visible)} |", '',
              'Categories use any executable changed post-image line covered at the commit; zero-executable and unavailable commits are separate.', '']
    for key in ['scope-changing commits total', 'zero executable changed lines', 'covered by neither group', 'covered by Dev only', 'covered by LLM only', 'covered by both', 'unavailable because build failed']:
        lines.append(f'- {key}: {categories[key]}.')
    (RESULTS / 'table3-changed-line-coverage.md').write_text('\n'.join(lines) + '\n')
    lines = ['# Table 4. Pre-image exposure versus first break', '', 'Exposure means at least one executable pre-image changed line was covered at c−1. Break means at least one previously surviving method first broke at c.', '']
    for group in maps:
        lines += [f'## {group.upper()}', '', '| Covered pre-image changed line | Any method broken | No method broken |', '|---|---:|---:|',
                  f"| Yes | {exposure[group][(True, True)]} | {exposure[group][(True, False)]} |",
                  f"| No | {exposure[group][(False, True)]} | {exposure[group][(False, False)]} |", '',
                  f'Unavailable because c or c−1 failed to build: {unknown_exposure[group]}.', '']
        assert sum(exposure[group].values()) + unknown_exposure[group] == 50
    (RESULTS / 'table4-exposure-vs-break.md').write_text('\n'.join(lines) + '\n')
    lines = ['# Coverage at t (full frozen sets)', '', '| Scope source file | Executable lines | Dev covered | LLM covered |', '|---|---:|---:|---:|']
    baseline = {}
    assert maps['dev'][0] is not None and maps['llm'][0] is not None
    for path in SCOPE_FILES:
        file = path.removeprefix('src/main/java/')
        keys = [key for key in maps['dev'][0] if key[0] == file]
        counts = {g: sum(maps[g][0][key] for key in keys) for g in maps}
        baseline[file] = {'executable_lines': len(keys), **counts}
        lines.append(f"| {file} | {len(keys)} | {pct(counts['dev'], len(keys))} | {pct(counts['llm'], len(keys))} |")
    total = sum(r['executable_lines'] for r in baseline.values())
    lines.append(f"| Total | {total} | {pct(sum(r['dev'] for r in baseline.values()), total)} | {pct(sum(r['llm'] for r in baseline.values()), total)} |")
    (RESULTS / 'coverage-t.md').write_text('\n'.join(lines) + '\n')
    missing = [{'index': i, 'group': g} for g in maps for i, value in maps[g].items() if value is None]
    consistency = {'coverage_commits': len(indices), 'scope_changing_commits': len(rows), 'xml_files_parsed': 2 * len(indices) - len(missing),
                   'missing_xml_build_fail': missing, 'executable_line_sets_identical_between_groups': executable_matches,
                   'all_changed_commits_and_predecessors_present': not missing, 'categories': dict(categories), 'baseline': baseline}
    write_json(RESULTS / 'coverage-consistency.json', consistency)
    p = RESULTS / 'consistency.md'
    prior = p.read_text().split('\n## Coverage consistency')[0].rstrip()
    p.write_text(prior + '\n\n## Coverage consistency\n\n' + '\n'.join([
        f'- Scope-changing commits checked: {len(rows)}; coverage commits: {len(indices)}.',
        f'- Every required c and c−1 has XML: {not missing}; unavailable build-fail records: {missing}.',
        f'- Both groups have identical executable line sets for all five files at every jointly built commit: {len(executable_matches)} passed.',
        '- XML line records and committed line CSVs agree exactly: passed.',
        '- Each group’s exposure table accounts for all 50 scope-changing commits, including explicitly unavailable cases: passed.'
    ]) + '\n')
    print(json.dumps({k: v for k, v in consistency.items() if k not in {'baseline', 'executable_line_sets_identical_between_groups'}}, indent=2))


if __name__ == '__main__':
    main()
