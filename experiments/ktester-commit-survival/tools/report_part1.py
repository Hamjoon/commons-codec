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
    r = read('population/id-renumbering-validation.json')
    wiring = read('results/container-wiring.json')
    validation = read('results/part1-validation.json')
    assert r['valid'] and r['unique_ids'] == 66 and r['previously_unique_entries_unchanged']
    assert wiring['exit_code'] == 0 and wiring['original_mounts_preserved'] and validation['population_valid']
    return {
        'status': 'part1_complete', 'completed_steps': [1, 2, 3, 4, 5, 6],
        'developer_denominator': b['developer_denominator'], 'selected_commits': c['selected_rows'],
        'scope_changing_commits': c['selected_scope_changes'], 'population_eligible_methods': p['expected_count'],
        'population_classes': len(p['per_class']), 'dataset_prepare_run': True,
        'generated_count': r['entries'], 'unique_ids': r['unique_ids'], 'colliding_ids': 0,
        'renumbered_ids': r['changed_ids'], 'unchanged_ids': r['unchanged_ids'],
        'previously_unique_entries_unchanged': r['previously_unique_entries_unchanged'],
        'non_naming_fields_unchanged': r['non_naming_fields_unchanged'],
        'original_entries_matching_up_to_line_endings': validation['final_original_entries_equal_up_to_line_endings'],
        'excluded_private_original_methods': p['excluded_private_original_methods'],
        'persistent_container_override': 'docker/compose.commit-survival.yml; verified',
        'llm_calls': 0, 'pushes': 0, 'open_decisions': [],
        'prior_stops': [
            {'commit': '05e849ed', 'stop': 'Step 3 exact EvoSuite/JaCoCo snapshot resolution failures', 'resolved_by': 'addendum 1 Decision 1'},
            {'commit': 'f2368151', 'stop': 'Step 4 private original focal-method inclusion conflict', 'resolved_by': 'addendum 2: explicitly exclude both private methods'},
            {'commit': '09be111e', 'stop': 'Step 4 duplicate DatasetPrepare IDs', 'resolved_by': 'addendum 3 with Gary clarification: preserve unique IDs and renumber only colliding entries'}]}


def summary_lines(s):
    return [
        f"Part 1 is complete. The population has {s['generated_count']} entries and {s['unique_ids']} unique IDs. Renumbering changes {s['renumbered_ids']} IDs and preserves {s['unchanged_ids']} IDs, including every previously unique ID. The compose override is verified with the existing Maven cache retained.", '',
        f"All {s['population_eligible_methods']} candidate methods have non-empty focal-method and class-code fields. Only id, test-class and test-path were changed; all other fields and entry order are preserved. All {s['original_entries_matching_up_to_line_endings']} included original focal entries still match every author field up to CRLF/LF differences. The two private originals are excluded as authorized.", '',
        'Evidence: population/id-renumbering-validation.json, population/id-renumbering.csv, results/part1-validation.json and results/container-wiring.json. Use dataset_info.json for Part 2; dataset_info.raw.json retains the unmodified DatasetPrepare output. Historical population-enumeration.json and population-validation.json retain the earlier stop records.']


def stop_history():
    return ['## First attempt', '',
            'The first attempt stopped at Step 3 (commons-codec 05e849ed) after repeated Maven Central failures for EvoSuite 1.2.0 and JaCoCo 0.8.13-SNAPSHOT. Addendum 1 removed EvoSuite and selected released JaCoCo 0.8.12; all artifacts now resolve. It also extended the range to 539 commits.', '',
            '## Second attempt', '',
            'The next attempt stopped at Step 4 (commons-codec f2368151; KTester 2650707): the requirement to include original private focal methods conflicted with the non-private population rule. Addendum 2 explicitly excluded MurmurHash3.hash128x64Internal and Sha2Crypt.sha2Crypt and fixed membership at 66 methods.', '',
            '## Third attempt and resolution', '',
            'Addendum 2 stopped at Step 4 (commons-codec 09be111e; KTester 86fe574): DatasetPrepare emitted 66 entries but only 43 distinct IDs, with nine colliding IDs affecting 32 methods. The raw extraction has no missing/empty entries or author-field mismatches. Addendum 3 authorized post-processing the naming fields while leaving the authors’ Java and all other fields untouched.', '',
            'Gary clarified that every previously unique ID must be preserved: only entries with colliding raw IDs are sorted and renumbered. This clarification governs the ambiguous full-group sorting sentence in the addendum. The final mapping changes 23 IDs, preserves all previously unique entries, and yields 66 unique IDs. All three stops are resolved.', '',
            '## Next part', '',
            'No open Part 1 decisions remain. Stop here: Part 2 generation has not started and no LLM call was made. Use the renumbered dataset_info.json and the authors’ existing project index when Part 2 is authorized. The fixed model, prompts and settings remain unchanged.']


def report():
    s = state()
    (EXPERIMENT / 'results/part1-status.json').write_text(json.dumps(s, indent=2) + '\n')
    lines = ['# Part 1 final status', '', *summary_lines(s), '',
             'This report is rendered by `python3 experiments/ktester-commit-survival/tools/report_part1.py` from results/commits-check.json, results/developer-baseline.json, population/id-renumbering-validation.json, results/part1-validation.json and results/container-wiring.json.', '',
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
    lines = ['# Part 1 final handover', '', *summary_lines(s), '',
             'Measurements come from the JSON/report files cited below. Render this ignored local handover after committing with `python3 experiments/ktester-commit-survival/tools/report_part1.py --handover --kt ' + str(kt) + '`; Git identities use git branch --show-current and git rev-parse HEAD.', '',
             '## Final repositories', '',
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
    lines += ['', 'Final dataset locations (raw output retained beside each as dataset_info.raw.json):', '',
              '- KTester: `' + str(kt / 'data/commit-survival/commons-codec/dataset_info.json') + '`.',
              '- Public copy: `experiments/ktester-commit-survival/population/dataset_info.json`.', '',
              f"Every candidate has exactly one entry and both code fields are non-empty. All five original non-private entries match up to line endings. Historical raw-output ID check: {p['unique_id_count']} distinct IDs for {p['generated_count']} entries. These former colliding IDs affected {p['collision_affected_methods']} methods (full signatures in population/README.md):", '',
              '| ID | Affected methods |', '|---|---:|']
    for ident, methods in p['id_collisions'].items():
        lines.append(f'| {ident} | {len(methods)} |')
    renumbering = read('population/id-renumbering-validation.json')
    lines += ['', '### Final ID renumbering', '', renumbering['rule'], '',
              f"id-renumbering-validation.json: {renumbering['entries']} entries, {renumbering['unique_ids']} unique IDs, {renumbering['changed_ids']} changed IDs, {renumbering['unchanged_ids']} unchanged IDs. All previously unique entries are preserved. test-class/test-path are consistent, all non-naming fields and entry order are unchanged, and the transformation is idempotent. Full mapping: population/id-renumbering.csv.", '',
              'Gary’s clarification preserves the five original IDs, including Md5Crypt_md5Crypt. Every original non-private entry continues to match every author field up to line endings. The authors’ Java, dataset, project index, prompts and settings were not changed.', '',
              'Overload groups from population/population-validation.json:', '', '| Group | Methods |', '|---|---:|']
    for group, methods in p['overload_groups'].items():
        lines.append(f'| {group} | {len(methods)} |')
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
              '## Container wiring', '', 'From KTester/docker: `docker compose -f compose.yml -f compose.commit-survival.yml run --rm ktester bash -c \'ls /repo/experiments/ktester-commit-survival && mvn -v\'`. results/container-wiring.json and container-wiring.txt record exit 0, the experiment listing, Maven 3.9.9 and Java 17.0.20.1. The merged configuration differs only by the added /repo bind; /work, ktester-m2:/root/.m2 and all base service settings remain unchanged.', '', *stop_history()]
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
