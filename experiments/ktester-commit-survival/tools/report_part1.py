#!/usr/bin/env python3
"""Render current Part 1 state and the local handover from preserved evidence."""
import argparse
import json
import subprocess
from pathlib import Path
from experiment import EXPERIMENT, REPO


def read(path):
    return json.loads((EXPERIMENT / path).read_text())


def git(repo, *args):
    return subprocess.check_output(['git', '-C', str(repo), *args], text=True).strip()


def state():
    b = read('results/developer-baseline.json')
    c = read('results/commits-check.json')
    p = read('population/population-validation.json')
    assert not p['valid'] and p['id_collisions'], 'Review report wording if the blocker changes'
    return {
        'status': p['status'], 'stopped_at_step': 4,
        'completed': ['original Steps 1-2', 'addendum 1: classpath and released coverage tools',
                      'addendum 1: extended commit range', 'addendum 2: fixed population, metadata and extraction'],
        'developer_denominator': b['developer_denominator'], 'selected_commits': c['selected_rows'],
        'scope_changing_commits': c['selected_scope_changes'], 'population_eligible_methods': p['expected_count'],
        'population_classes': len(p['per_class']), 'dataset_prepare_run': True,
        'generated_count': p['generated_count'], 'unique_ids': p['unique_id_count'],
        'colliding_ids': len(p['id_collisions']), 'collision_affected_methods': p['collision_affected_methods'],
        'missing_methods': p['missing_methods'], 'empty_fields': p['empty_fields'],
        'original_entries_matching_up_to_line_endings': sum(x['equal_up_to_line_endings'] for x in p['original_entry_comparisons']),
        'excluded_private_original_methods': p['excluded_private_original_methods'],
        'persistent_container_override': 'not started after mandatory collision stop',
        'llm_calls': 0, 'pushes': 0,
        'prior_stops': [
            {'commit': '05e849ed', 'stop': 'Step 3 exact EvoSuite/JaCoCo snapshot resolution failures', 'resolved_by': 'addendum 1 Decision 1'},
            {'commit': 'f2368151', 'stop': 'Step 4 private original focal-method inclusion conflict', 'resolved_by': 'addendum 2: explicitly exclude both private methods'}]}


def summary_lines(s):
    return [
        f"Part 1 remains incomplete at Step 4: DatasetPrepare produced {s['generated_count']} entries, but only {s['unique_ids']} distinct IDs. {s['colliding_ids']} IDs collide across {s['collision_affected_methods']} methods. The second addendum requires stopping on collisions, so Step 5 was not entered. Raw output is retained without ID repairs or Java patches.", '',
        f"All {s['population_eligible_methods']} candidate methods are present with non-empty focal-method and class-code fields. All {s['original_entries_matching_up_to_line_endings']} included original focal entries match every author field up to CRLF/LF differences. The two private originals are excluded as authorized.", '',
        'Evidence and exact invocation: population/population-validation.json and population/README.md. The latter lists every affected signature. The raw metadata and dataset output are tracked in both repositories as diagnostic evidence, not a generation-ready dataset.']


def stop_history():
    return ['## First attempt', '',
            'The first attempt stopped at Step 3 (commons-codec 05e849ed) after repeated Maven Central failures for EvoSuite 1.2.0 and JaCoCo 0.8.13-SNAPSHOT. Addendum 1 removed EvoSuite and selected released JaCoCo 0.8.12; all artifacts now resolve. It also extended the range to 539 commits.', '',
            '## Second attempt', '',
            'The next attempt stopped at Step 4 (commons-codec f2368151; KTester 2650707): the requirement to include original private focal methods conflicted with the non-private population rule. Addendum 2 explicitly excluded MurmurHash3.hash128x64Internal and Sha2Crypt.sha2Crypt and fixed membership at the enumerated 66 methods. That conflict is resolved. The historical enumeration JSON/driver retain their original stop record; population-validation.json records the current state.', '',
            '## Current stop and remaining work', '',
            'The unmodified packaged DatasetPrepare reused overload IDs. Its source updates the suffixed ID counter instead of the base counter in getMethodInfo; the observed collisions are recorded without repairing the output. No missing/empty entries or author-field mismatches were observed. The persistent compose override and final successful Part 1 completion remain pending under the explicit stop rule.', '',
            'TODO (Gary): Decide how to resolve the DatasetPrepare ID collisions while keeping all 66 methods. The current instructions prohibit patching DatasetPrepare.java; no Java patch or output-ID repair was applied.']


def report():
    s = state()
    (EXPERIMENT / 'results/part1-status.json').write_text(json.dumps(s, indent=2) + '\n')
    lines = ['# Part 1 status after addendum 2', '', *summary_lines(s), '',
             'This report is rendered by `python3 experiments/ktester-commit-survival/tools/report_part1.py` from results/commits-check.json, results/developer-baseline.json and population/population-validation.json.', '',
             f"Range: {s['selected_commits']} verified first-parent commits, including {s['scope_changing_commits']} scope-changing commits. Developer denominator: {s['developer_denominator']}, unchanged. The approved 20-artifact classpath and verified JaCoCo coverage mechanism remain unchanged (results/classpath-fetch.json and results/coverage-smoke.json).", '',
             'No model calls, pushes, source/settings/index changes, or test generation occurred.', '', *stop_history()]
    (EXPERIMENT / 'results/part1-status.md').write_text('\n'.join(lines) + '\n')


def handover(kt):
    s = state()
    b = read('results/developer-baseline.json')
    c = read('results/commits-check.json')
    f = read('results/classpath-fetch.json')
    cov = read('results/coverage-smoke.json')
    smoke = read('results/classpath-smoke.json')
    p = read('population/population-validation.json')
    artifacts = read('tools/classpath-artifacts.json')
    lines = ['# Part 1 handover after addendum 2', '', *summary_lines(s), '',
             'Measurements come from the JSON/report files cited below. Render this ignored local handover after committing with `python3 experiments/ktester-commit-survival/tools/report_part1.py --handover --kt ' + str(kt) + '`; Git identities use git branch --show-current and git rev-parse HEAD.', '',
             '## Repositories at the stop', '',
             f"- commons-codec: `{git(REPO, 'branch', '--show-current')}` at `{git(REPO, 'rev-parse', 'HEAD')}`.",
             f"- KTester: `{git(kt, 'branch', '--show-current')}` at `{git(kt, 'rev-parse', 'HEAD')}`.",
             '- Neither repository was pushed. New commits have the required Codex co-author trailer.',
             '- KTester additions are under data/commit-survival/commons-codec/ and docker/. Author data, snapshot, settings, Java and project index remain unchanged. The local source symlink is ignored.',
             '- Public experiment artifacts are under experiments/ktester-commit-survival/. work/ and this handover remain ignored. Existing untracked instruction files and Claude outputs/ in KTester remain untouched.', '',
             '## Verified commit range', '',
             f"results/commits-check.json: {c['selected_rows']} first-parent commits, {c['selected_src_changes']} changing src/main and {c['selected_scope_changes']} changing scope files. Last: {c['last_selected']['sha']} ({c['last_selected']['date']}). All first-parent links verified."]
    for tag in c['release_tags']:
        lines.append(f"- {tag['tag']}: index {tag['index']}, {tag['sha']}.")
    lines += ['', '## Developer baseline unchanged', '',
              f"results/developer-baseline.json and dev/dev-denominator.csv: denominator **{b['developer_denominator']}**, from {b['scope_methods']} scoped methods. Base64Test.testCodec265 is skipped in every run. Whole-suite reference: {b['whole_methods']} methods, {b['whole_pass_all']} passing every run. Frozen sources: dev/src-test-t/.", '',
              '| Run | Cases | Failures | Errors | Skipped | Seconds |', '|---:|---:|---:|---:|---:|---:|']
    for i, r in enumerate(b['runs'], 1):
        t = r['totals']
        lines.append(f"| {i} | {t['tests']} | {t['failures']} | {t['errors']} | {t['skipped']} | {r['seconds']} |")
    lines += ['', '## Population', '',
              f"population/population-validation.json: {p['expected_count']} non-private methods, within 60–100; {p['overload_group_count']} overload groups containing {p['methods_in_overload_groups']} methods. Enumeration at t is preserved in candidates.csv and population-enumeration.json. DatasetPrepare reads the authors’ snapshot via the prescribed symlink, using the Stage 0 JVM startup and unmodified jars.", '',
              '| Class | Methods |', '|---|---:|']
    for cls, n in p['per_class'].items():
        lines.append(f'| {cls} | {n} |')
    lines += ['', 'Excluded private original focal methods:', '']
    lines += ['- `' + m + '`.' for m in p['excluded_private_original_methods']]
    lines += ['', 'Raw dataset locations:', '',
              '- KTester: `' + str(kt / 'data/commit-survival/commons-codec/dataset_info.json') + '`.',
              '- Public copy: `experiments/ktester-commit-survival/population/dataset_info.json`.', '',
              f"Every candidate has exactly one entry and both code fields are non-empty. All five original non-private entries match up to line endings. ID uniqueness failed: {p['unique_id_count']} distinct IDs for {p['generated_count']} entries. These colliding IDs affect {p['collision_affected_methods']} methods (full signatures in population/README.md):", '',
              '| ID | Affected methods |', '|---|---:|']
    for ident, methods in p['id_collisions'].items():
        lines.append(f'| {ident} | {len(methods)} |')
    lines += ['', '## Classpath and exact smoke commands', '',
              f"results/classpath-fetch.json records SHA-256 for all {f['resolved']}/{f['requested']} artifacts: 18 test jars and 2 coverage tools. EvoSuite is removed; the JaCoCo runtime agent and nodeps CLI are version 0.8.12. Neither coverage jar is on the test compile or run classpath. results/coverage-smoke.json verifies Md5Crypt.java: {cov['line_entries']} line entries, {cov['counters']['LINE']['covered']} covered lines.", '']
    for a in artifacts:
        lines.append('- `' + a['coordinate'] + '`' + (' classifier=' + a['classifier'] if a['classifier'] else '') + ' — ' + a['role'] + '.')
    commands = [cov['runs'][0]['shell_command']]
    commands += [r['shell_command'] for r in smoke['runs'] if r['stage'] == 'launch']
    commands += [r['shell_command'] for r in cov['runs'] if r['stage'] != 'compile']
    lines += ['', 'Run from /repo/experiments/ktester-commit-survival inside the container. Exact commands from results/classpath-smoke.json (launcher) and results/coverage-smoke.json (explicit test-only compile, agent, report):', '',
              '```sh', *commands, '```', '',
              'The smoke tests compile and execute; their recorded assertion failures are allowed by the smoke criterion. Coverage is nonzero. The authored smoke source and coverage XML/exec remain only in ignored work/smoke/.', '',
              'Temporary mounts use `docker compose run --rm -v /Users/donggi/_projects/experiment-projects/commons-codec:/repo ktester bash -c ...` from KTester/docker. Existing /work and ktester-m2 mounts are retained; no persistent override was created after the mandatory stop.', '', *stop_history()]
    out = EXPERIMENT / 'docs/handover-part1.md'
    out.write_text('\n'.join(lines) + '\n')
    print(out)


if __name__ == '__main__':
    ap = argparse.ArgumentParser()
    ap.add_argument('--handover', action='store_true')
    ap.add_argument('--kt', type=Path)
    args = ap.parse_args()
    if args.handover:
        if args.kt is None:
            ap.error('--kt required')
        handover(args.kt)
    else:
        report()
