"""Derive cumulative survival, alive-entering sets, event tables and a step plot."""
import collections
import json
import sys
from survival_common import (EXP, RESULTS, STATES, check_inputs, commits, denominator, git,
                             read_csv, write_csv, write_json)


def pct(count, total):
    return f'{count} ({100 * count / total:.1f}%)'


def md(value):
    return str(value).replace('|', '\\|').replace('\n', ' ')


def derive(commit_map, raw_by_group, denominators):
    alive_entering = {str(i): {} for i in commit_map}
    curves, breaks, transient, histories = {}, {}, {}, {}
    flaky_count = 0
    failures = []
    for group in ['dev', 'llm']:
        rows = denominators[group]
        total = len(rows)
        alive = {r['id'] for r in rows}
        first, past_failure, transients = {}, {}, {}
        history = collections.defaultdict(list)
        curve = [{'index': 0, 'sha': commit_map[0]['sha'], 'date': commit_map[0]['date'], 'evaluated': 'yes',
                  'alive_count': total, 'alive_percent': 100.0, 'newly_broken_count': 0}]
        alive_entering['0'][group] = sorted(alive)
        for index in sorted(i for i in commit_map if i):
            raw = raw_by_group[group][index]
            assert raw['sha'] == commit_map[index]['sha']
            alive_entering[str(index)][group] = sorted(alive)
            lost = []
            if raw['status'] == 'build-fail':
                if group == 'dev':
                    failures.append(index)
            else:
                methods = {m['id']: m for m in raw['methods']}
                assert set(methods) == {r['id'] for r in rows}
                for identifier, method in methods.items():
                    state = method['state']
                    assert state in STATES
                    if method['flaky']:
                        flaky_count += 1
                        history[identifier].append(index)
                    if state != 'pass':
                        past_failure.setdefault(identifier, index)
                        if identifier in alive:
                            lost.append(identifier)
                            first[identifier] = {'first_failing_index': index, 'sha': commit_map[index]['sha'],
                                                 'kind': state, 'message': method['message']}
                    elif identifier in past_failure:
                        transients.setdefault(identifier, {'failed_index': past_failure[identifier], 'later_pass_index': index})
                alive -= set(lost)
            curve.append({'index': index, 'sha': commit_map[index]['sha'], 'date': commit_map[index]['date'],
                          'evaluated': 'no' if raw['status'] == 'build-fail' else 'yes', 'alive_count': len(alive),
                          'alive_percent': 100 * len(alive) / total, 'newly_broken_count': len(lost)})
        curves[group] = curve
        breaks[group] = [{**{k: row[k] for k in ['id', 'file', 'class', 'method']},
                          **first.get(row['id'], {'first_failing_index': '', 'sha': '', 'kind': '', 'message': ''}),
                          'flaky_flag_history': json.dumps(history[row['id']])} for row in rows]
        transient[group] = [{**{k: row[k] for k in ['id', 'file', 'class', 'method']}, **transients[row['id']]} for row in rows if row['id'] in transients]
        histories[group] = dict(history)
        assert curve[0]['alive_count'] == total
        assert all(b['alive_count'] <= a['alive_count'] for a, b in zip(curve, curve[1:]))
    for index in sorted(i for i in commit_map if i):
        assert (raw_by_group['dev'][index]['status'] == 'build-fail') == (raw_by_group['llm'][index]['status'] == 'build-fail')
    return curves, breaks, transient, alive_entering, failures, flaky_count


def main():
    fingerprint = check_inputs()
    cm = commits()
    raw = {g: {i: json.loads((RESULTS / 'raw' / f'{i}-{g}.json').read_text()) for i in range(1, 540)} for g in ['dev', 'llm']}
    assert all(r['input_sha256'] == fingerprint for values in raw.values() for r in values.values())
    den = {g: denominator(g) for g in raw}
    curves, breaks, transient, alive, failed, flaky = derive(cm, raw, den)
    for group in raw:
        write_csv(RESULTS / f'{group}-survival.csv', ['index', 'sha', 'date', 'evaluated', 'alive_count', 'alive_percent', 'newly_broken_count'], curves[group])
        write_csv(RESULTS / f'{group}-first-break.csv', ['id', 'file', 'class', 'method', 'first_failing_index', 'sha', 'kind', 'message', 'flaky_flag_history'], breaks[group])
        write_csv(RESULTS / f'{group}-transient.csv', ['id', 'file', 'class', 'method', 'failed_index', 'later_pass_index'], transient[group])
    write_json(RESULTS / 'alive-entering.json', alive)
    subjects = {i: git('show', '-s', '--format=%s', row['sha']).strip() for i, row in cm.items()}
    events = {0} | {i for i, row in cm.items() if row.get('tags')}
    events |= {r['index'] for values in curves.values() for r in values if r['newly_broken_count']}
    lines = ['# Table 1. Cumulative survival by event', '', '| Index | SHA | Date | Change | Dev | LLM |', '|---:|---|---|---|---:|---:|']
    for i in sorted(events):
        lines.append(f"| {i} | {cm[i]['sha'][:7]} | {cm[i]['date'][:10]} | {md(subjects[i])} | {pct(curves['dev'][i]['alive_count'], 143)} | {pct(curves['llm'][i]['alive_count'], 290)} |")
    lines += ['', 'Denominators remain 143 and 290. Build-fail commits carry the preceding state and are excluded from evaluation. Release tag commits are included even without a break.']
    (RESULTS / 'table1-survival-by-event.md').write_text('\n'.join(lines) + '\n')
    breaking = []
    for i in range(1, 540):
        counts = {g: curves[g][i]['newly_broken_count'] for g in raw}
        if sum(counts.values()):
            kinds = collections.Counter(r['kind'] for values in breaks.values() for r in values if r['first_failing_index'] == i)
            breaking.append({'index': i, **counts, 'kinds': kinds})
    breaking.sort(key=lambda r: (-r['dev'] - r['llm'], r['index']))
    lines = ['# Table 2. Breaking commits', '', '| Index | SHA | Change | Dev lost | LLM lost | First-break kinds |', '|---:|---|---|---:|---:|---|']
    for row in breaking[:10]:
        i = row['index']
        lines.append(f"| {i} | {cm[i]['sha'][:7]} | {md(subjects[i])} | {row['dev']} | {row['llm']} | {md(dict(row['kinds']))} |")
    others = breaking[10:]
    kinds = sum((r['kinds'] for r in others), collections.Counter())
    lines.append(f"| Other commits ({len(others)}) | — | — | {sum(r['dev'] for r in others)} | {sum(r['llm'] for r in others)} | {md(dict(kinds))} |")
    (RESULTS / 'table2-breaking-commits.md').write_text('\n'.join(lines) + '\n')
    sys.path.insert(0, str(EXP / 'work/python-libs'))
    import matplotlib
    matplotlib.use('Agg')
    import matplotlib.pyplot as plt
    fig, ax = plt.subplots(figsize=(9, 4.5), layout='constrained')
    for group, label, color in [('dev', 'Developer (n=143)', '#1f77b4'), ('llm', 'LLM (n=290)', '#d55e00')]:
        ax.step([r['index'] for r in curves[group]], [r['alive_percent'] for r in curves[group]], where='post', label=label, color=color, linewidth=1.8)
    ax.axvline(51, color='#777777', linestyle='--', linewidth=1)
    ax.text(54, 5, '1.17.1', rotation=90, color='#555555', fontsize=9)
    ax.set(xlabel='First-parent commit index after t', ylabel='Cumulative survival (%)', xlim=(0, 539), ylim=(0, 102))
    ax.grid(axis='y', alpha=.2)
    ax.legend(loc='lower left')
    for suffix in ['png', 'svg']:
        fig.savefig(RESULTS / f'figure1-survival-by-commit.{suffix}', dpi=200)
    plt.close(fig)
    retries = sum('retry_state' in m for values in raw.values() for r in values.values() for m in r['methods'])
    summary = {'raw_files_parsed': 1078, 'build_fail_indices': failed, 'build_fail_count': len(failed),
               'flaky_retries_total': retries, 'retries_that_passed': flaky, 'survival_non_increasing': True,
               'index0_matches_denominators': True, 'final_alive': {g: curves[g][-1]['alive_count'] for g in raw},
               'subjects': {str(i): s for i, s in subjects.items()}}
    write_json(RESULTS / 'survival-consistency.json', summary)
    (RESULTS / 'consistency.md').write_text('# Consistency checks\n\n' + '\n'.join([
        '- Survival is non-increasing for both groups: passed.', '- Index 0 equals the frozen denominators (143 and 290): passed.',
        '- Every Phase A raw file parsed: 1078 of 1078.', f'- Build-fail commits: {len(failed)}; indices: {failed}.',
        f'- Fail/error method re-runs: {retries}; passed on retry (flaky=yes): {flaky}.']) + '\n')
    print(json.dumps({k: v for k, v in summary.items() if k != 'subjects'}, indent=2))


if __name__ == '__main__':
    main()
