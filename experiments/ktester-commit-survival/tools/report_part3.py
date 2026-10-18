"""Render the ignored Part 3 handover directly from measured tables and raw records."""
import argparse
import collections
import json
from pathlib import Path
import subprocess
from survival_common import EXP, REPO, RESULTS, check_inputs


def revision(root):
    return subprocess.check_output(['git', '-C', str(root), 'rev-parse', 'HEAD'], text=True).strip()


def host_git_deviation():
    path = RESULTS / 'part3-host-git-check.json'
    if not path.exists():
        return []
    evidence = json.loads(path.read_text())
    recovery = evidence['after_license_acceptance']
    assert evidence['license_accepted_by_user']
    assert all(row['exit_code'] == 0 for row in recovery['checks'])
    failure = next(row for row in evidence['before_license_acceptance']['checks'] if row['exit_code'] == 69)
    lines = ['### Host Git failure and recovery', '',
             'After Phase B finished, the host Git lookup in check_raw exited 69. A host git --version check reproduced the failure: Xcode required acceptance of its license agreement. The continuation remained stopped until the user accepted the license. No measurement was rerun for this recovery.', '',
             f"Successful host checks after license acceptance ({recovery['recorded_at']}):", '',
             '| Command | Exit | Standard output |', '|---|---:|---|']
    for row in recovery['checks']:
        lines.append(f"| `{' '.join(row['command'])}` | {row['exit_code']} | `{row['stdout'].strip()}` |")
    lines += ['', 'All successful checks had empty stderr. The earlier exit-69 stderr was:', '',
              '```text', failure['stderr'].rstrip('\n'), '```', '',
              'The commands, exit codes and exact output before and after recovery are retained in results/part3-host-git-check.json.', '']
    return lines


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--kt', type=Path, required=True)
    args = parser.parse_args()
    check_inputs()
    survival = json.loads((RESULTS / 'survival-consistency.json').read_text())
    coverage = json.loads((RESULTS / 'coverage-consistency.json').read_text())
    fallbacks = [{'index': r['index'], 'group': r['group'], **f} for p in sorted((RESULTS / 'raw-coverage').glob('*.json'))
                 for r in [json.loads(p.read_text())] for f in r['fallbacks']]
    builds = [json.loads(p.read_text()) for p in (RESULTS / 'builds/a').glob('*.json')]
    warmed = sorted(r['index'] for r in builds if r.get('cache_warmed_online'))
    lines = ['# Part 3 final handover', '',
             f"Part 3 is complete. Final cumulative survival: Dev {survival['final_alive']['dev']}/143; LLM {survival['final_alive']['llm']}/290. Coverage measured at {coverage['coverage_commits']} commits for {coverage['production_changing_commits']} production-changing commits and their predecessors.", '',
             f'- commons-codec: `{revision(REPO)}`.', f'- KTester: `{revision(args.kt)}`.',
             '- Branch: `experiment/2026-10-week3-ktester-commit-survival`. No pushes.',
             '- Frozen generated/developer sources, denominators and commits.csv match the Part 3 input SHA-256 manifest.', '',
             '## Runner and measurements', '',
             'Step 0 reproduced all 143 developer and 290 LLM passes. All six developer classes compiled separately with their recorded transitive helper files and the existing 18 jars. No extra developer test jar was required. Compiled runtime annotations distinguish actual tests from non-test overloads; exact parameter types select parameterized methods.', '',
             'Phase A runs all six developer classes and all frozen LLM denominator methods at every commit. The 46 LLM files are compiled separately; methods excluded by Part 2 are not selected. Phase B selects the alive-entering methods, with any required whole-class fallbacks listed below. Every class run has a 600-second process-group wall limit. Fail/error methods are retried once; a passing retry is pass with flaky=yes. Skipped/non-reported denominator methods are absent. All source copies retain their bytes; r2- prefixes are removed only from temporary compilation filenames.', '',
             'Each worker uses its own clean git-archive export and per-commit/group reports. Phase A used four Python workers initially, eight after the user-authorized restart, and two for the targeted repair of quarantined records. The initial uncapped Phase B attempts used eight and six workers and stopped on OOM. Capped Phase B started with four workers; after a stable run the user raised it to six, with OOM fallback to four. The existing four-to-two OOM fallback remains. Exact counts and layouts for every actual invocation are recorded below. Workers share only the Maven cache and immutable inputs. Completed group JSON is written atomically and checked before being skipped on resume. Verification rejects every signal-9 exit before aggregation.', '',
             f'Build-fail indices: {survival["build_fail_indices"]}. These commits carry over states and are excluded from survival evaluation.',
             f'Offline Maven cache misses warmed online on the second build attempt at indices: {warmed}. Build commands retain the Part 1 skip set. Exact commands, logs and timing are in results/builds/.', '',
             '## Phase timing and layout', '']
    for phase in ['a', 'b']:
        invocations = [json.loads(p.read_text()) for p in sorted((RESULTS / f'phase-{phase}-invocations').glob('*.json'))]
        seconds = sum(r['seconds'] for r in invocations)
        lines += [f'Phase {phase.upper()}: {seconds:.3f} wall seconds across {len(invocations)} invocation(s).', '']
        for row in invocations:
            lines += [f"- {row['start']} to {row['end']}; workers={len(row['worker_layout'])}; complete={row['completed']}; worker exit codes={row['worker_exit_codes']}.", '']
        lines += ['Worker layout is the exact interleaved index arrays in the committed phase invocation JSON, with a distinct export per worker.', '']
    lines += ['## Report tables', '']
    for filename in ['table1-survival-by-event.md', 'table2-breaking-commits.md', 'table3-changed-line-coverage.md', 'coverage-t.md']:
        lines += [(RESULTS / filename).read_text().strip(), '']
    table3 = (RESULTS / 'table3-changed-line-coverage.md').read_text()
    assert len(coverage['table3_totals_verbatim']) == 2
    assert all(row in table3 for row in coverage['table3_totals_verbatim'])
    lines += ['The single all-lines Table 4 is retained in results/table4-exposure-vs-break.md as repository-only supporting evidence, as directed by the updated addendum.', '']
    memory_file = RESULTS / 'part3-worker-restart-memory.json'
    if memory_file.exists():
        memory = json.loads(memory_file.read_text())
        repairs = json.loads((RESULTS / 'phase-a-repair-verification.json').read_text())
        quarantined = json.loads((RESULTS / 'phase-a-quarantine.json').read_text())
        assert repairs['passed'] and repairs['full_phase_verification']['raw_files'] == 1078
        lines += ['## Restart memory events', '',
                  f"The eight-worker container recorded {memory['memory_events']['oom_kill']} OOM kills as of {memory['observed_at']}. The initial affected developer records were 69, 70 and 77, each containing 27 unreported MurmurHash3Test methods after a killed launcher. Those records were quarantined unchanged while Phase A continued. All further signal-9 raw records were also quarantined. The manifest retains {len(quarantined['entries'])} original records and their hashes in results/phase-a-quarantine.json; original bytes and logs remain under ignored work/quarantine/.", '',
                  f"After Phase A finished, {len(repairs['corrected_records'])} affected records were rerun with two workers. All 27 MurmurHash3Test methods in each repaired developer record were evaluated. Verification found no signal-9 exits in the full 1,078-record Phase A set and preserved {repairs['healthy_records_preserved']} healthy raw records byte-for-byte, including modification times. The restarted continuation aggregated only the corrected records. Evidence is in results/phase-a-repair-verification.json; scoring and frozen inputs were unchanged.", '']
    resources = json.loads((RESULTS / 'phase-b-resource-events.json').read_text())
    lines += ['Phase B memory caps: JUnit with JaCoCo and jacococli report use -Xmx1g -XX:+UseSerialGC; test javac uses -J-Xmx512m. Phase A commands and measurements remain unchanged. The frozen MurmurHash3Test huge-array method attempts a roughly 2 GiB allocation and aborts via its existing assumption when the capped JVM cannot allocate it; Phase B records that skipped method as absent. Coverage reflects this memory limitation, and survival remains derived solely from Phase A.', '']
    lines += [f"Phase B worker counts by attempt: {[r['workers'] for r in resources['attempts']]}. OOM-triggered restarts: {sum(bool(r.get('oom_detected')) for r in resources['attempts'])}. Container memory counters, OOM events and exits are recorded in results/phase-b-resource-events.json. Any killed raw coverage records and associated XML/CSV were quarantined before resume; final verification rejects signal-9 exits.", '']
    lines += [(RESULTS / 'consistency.md').read_text().strip(), '', '## Fallbacks and deviations', '',
              f'Whole-class coverage fallbacks: {len(fallbacks)}.', '', '```json', json.dumps(fallbacks, indent=2), '```', '',
              'The initial selector implementation included a private non-test overload in Base64Test and stopped during runner development. It was corrected to inspect compiled JUnit annotations; the corrected Step 0 passed before any survival loop. The failed attempt and correction are recorded in part3-runner-development.json. No frozen input was changed.', '',
              'Matplotlib and its Python dependencies are isolated under ignored work/python-libs for the scientific figures; their versions are recorded. They do not enter Java test classpaths. XML records and line CSVs preserve zero-coverage executable lines; empty alive sets are explicitly supported and verified at t.', '',
              f"The addendum estimated 100 production-changing commits and at most 200 coverage commits. The immutable commits.csv actually identifies {coverage['production_changing_commits']} production-changing commits, yielding B with {coverage['coverage_commits']} distinct indices. The specified set formula was followed without truncating the population. Phase A was left unchanged. Phase B extracts all codec-package production source-line entries; the revised aggregation has one all/scope split, no table3b, one repository-only Table 4, and five-file baseline coverage only.", '',
              'The host and Docker each reported 12 CPUs. Gary authorized restarting Phase A with eight workers and setting Phase B to eight for the Monday deadline. The four-worker invocation ended with SIGTERM worker exits after 68 complete commits; all 136 completed raw records were preserved byte-for-byte and skipped on resume. The exact old/new runner hashes are approved only for this concurrency-only transition; timeout, sources, denominators, method selection, build settings and scoring remain unchanged. The continuation was restarted with explicit approval to reload the new validator and Phase B command. Restart evidence is in results/part3-worker-restart.json.', '',
              *host_git_deviation(),
              'The first Aggregation 2 commit check rejected an extra blank line at the end of generated Table 4. The generator was corrected and Aggregation 2 regenerated from the same raw records. All other output bytes and all table values were preserved; only the final blank line was removed. No measurement or survival aggregation was rerun.', '',
              'Raw results: results/raw/ and results/raw-coverage/. Coverage XML and line records: results/coverage/. Change hunks: results/changes/. Local process logs and JUnit XML remain under ignored work/phase-a/ and work/phase-b/.', '',
              'TODO (Gary): Review any recorded build failures or whole-class coverage fallbacks when interpreting exposure and survival. No denominator adjustment was made.']
    (EXP / 'docs/handover-part3.md').write_text('\n'.join(lines) + '\n')
    print(json.dumps({'handover': str(EXP / 'docs/handover-part3.md'), 'final_survival': survival['final_alive'], 'fallbacks': len(fallbacks)}))


if __name__ == '__main__':
    main()
