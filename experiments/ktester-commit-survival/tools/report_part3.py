"""Render the ignored Part 3 handover directly from measured tables and raw records."""
import argparse
import collections
import json
from pathlib import Path
import subprocess
from survival_common import EXP, REPO, RESULTS, check_inputs


def revision(root):
    return subprocess.check_output(['git', '-C', str(root), 'rev-parse', 'HEAD'], text=True).strip()


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
             'Each worker uses its own clean git-archive export and per-commit/group reports. The driver used four Python workers initially, then eight after the user-authorized restart; Phase B uses eight workers inside the prescribed container, sharing only the Maven cache and immutable inputs. Completed group JSON is written atomically and checked before being skipped on resume. Tables are derived after the workers finish.', '',
             f'Build-fail indices: {survival["build_fail_indices"]}. These commits carry over states and are excluded from survival evaluation.',
             f'Offline Maven cache misses warmed online on the second build attempt at indices: {warmed}. Build commands retain the Part 1 skip set. Exact commands, logs and timing are in results/builds/.', '',
             '## Phase timing and layout', '']
    for phase in ['a', 'b']:
        invocations = [json.loads(p.read_text()) for p in sorted((RESULTS / f'phase-{phase}-invocations').glob('*.json'))]
        seconds = sum(r['seconds'] for r in invocations)
        lines += [f'Phase {phase.upper()}: {seconds:.3f} wall seconds across {len(invocations)} invocation(s).', '']
        for row in invocations:
            lines += [f"- {row['start']} to {row['end']}; complete={row['completed']}; worker exit codes={row['worker_exit_codes']}.", '']
        lines += ['Worker layout is the exact interleaved index arrays in the committed phase invocation JSON, with a distinct export per worker (w1–w4 initially, w1–w8 after the restart).', '']
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
        lines += ['## Restart memory events', '',
                  f"The eight-worker container recorded {memory['memory_events']['oom_kill']} OOM kills as of {memory['observed_at']}. Developer MurmurHash3Test JVMs at indices 69 and 70 exited with signal 9, producing 54 absent method records in the first completed batch. These are infrastructure-associated outcomes; raw states and scoring were retained unchanged. Exact recorded launcher exits and the memory-counter snapshot are in results/part3-worker-restart-memory.json.", '',
                  'TODO (Gary): Review memory-associated absent states before interpreting the survival results.', '']
    lines += [(RESULTS / 'consistency.md').read_text().strip(), '', '## Fallbacks and deviations', '',
              f'Whole-class coverage fallbacks: {len(fallbacks)}.', '', '```json', json.dumps(fallbacks, indent=2), '```', '',
              'The initial selector implementation included a private non-test overload in Base64Test and stopped during runner development. It was corrected to inspect compiled JUnit annotations; the corrected Step 0 passed before any survival loop. The failed attempt and correction are recorded in part3-runner-development.json. No frozen input was changed.', '',
              'Matplotlib and its Python dependencies are isolated under ignored work/python-libs for the scientific figures; their versions are recorded. They do not enter Java test classpaths. XML records and line CSVs preserve zero-coverage executable lines; empty alive sets are explicitly supported and verified at t.', '',
              f"The addendum estimated 100 production-changing commits and at most 200 coverage commits. The immutable commits.csv actually identifies {coverage['production_changing_commits']} production-changing commits, yielding B with {coverage['coverage_commits']} distinct indices. The specified set formula was followed without truncating the population. Phase A was left unchanged. Phase B extracts all codec-package production source-line entries; the revised aggregation has one all/scope split, no table3b, one repository-only Table 4, and five-file baseline coverage only.", '',
              'The host and Docker each reported 12 CPUs. Gary authorized restarting Phase A with eight workers and setting Phase B to eight for the Monday deadline. The four-worker invocation ended with SIGTERM worker exits after 68 complete commits; all 136 completed raw records were preserved byte-for-byte and skipped on resume. The exact old/new runner hashes are approved only for this concurrency-only transition; timeout, sources, denominators, method selection, build settings and scoring remain unchanged. The continuation was restarted with explicit approval to reload the new validator and Phase B command. Restart evidence is in results/part3-worker-restart.json.', '',
              'Raw results: results/raw/ and results/raw-coverage/. Coverage XML and line records: results/coverage/. Change hunks: results/changes/. Local process logs and JUnit XML remain under ignored work/phase-a/ and work/phase-b/.', '',
              'TODO (Gary): Review any recorded build failures or whole-class coverage fallbacks when interpreting exposure and survival. No denominator adjustment was made.']
    (EXP / 'docs/handover-part3.md').write_text('\n'.join(lines) + '\n')
    print(json.dumps({'handover': str(EXP / 'docs/handover-part3.md'), 'final_survival': survival['final_alive'], 'fallbacks': len(fallbacks)}))


if __name__ == '__main__':
    main()
