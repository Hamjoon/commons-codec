# Part 1 stop report

Status: stopped at the end of Step 3 under the explicit repeated-failure rule. Steps 1 and 2 are complete; Step 3 has complete inventory/fetch failure evidence and a successful compile/launcher smoke criterion, but the exact artifact set is incomplete. Steps 4 and 5 are not started. This is not a completed Part 1.

All numbers below are rendered by `python3 experiments/ktester-commit-survival/tools/report_part1.py` from results/commits-check.json, results/developer-baseline.json, results/classpath-fetch.json and results/classpath-smoke.json; their measurement invocations are in results/commits-check.md, dev/README.md and tools/README.md.

- Fixed history: 338 commits after cd16988dd46ac7533771eb6856a5f56c7aeaffba; 21 change scoped source files. Range is unchanged.
- Developer methods: 144 in scope, 143 pass every run; whole-suite reference 970 methods, 968 pass every run.
- Classpath: 18/21 exact supplied artifacts resolved and byte-identical; remaining artifacts failed both attempts. See tools/README.md for coordinates and logs.
- Smoke criterion passed: compilation succeeds and the launcher executes 14 tests, with 2 assertion failures allowed by the instruction.
- Population count and overload uniqueness: not measured; no dataset_meta.json or dataset_info.json was created.
- No model calls, generation, project-index rebuild, or remote push occurred. KTester pipeline, settings and author data were only read.
- Temporary /repo mounts were supplied with compose run -v for Steps 2 and 3. Existing compose.yml and Maven cache mounts were unchanged; Step 5 persistent wiring was not created.
- Root ignore rules for work/ and docs/handover-*.md were brought forward solely to retain the mandated stop handover locally. The handover is written after the final commit so it contains actual final hashes.

TODO (Gary): Decide how the exact missing auxiliary artifacts should be obtained, or explicitly revise their inclusion/version policy before resuming. No substitution was made.
TODO (Gary): Review the scope-change milestones in commits-check.md; the prescribed range remains fixed.
TODO (Gary): After resolving the Step 3 blocker, resume with focal-population enumeration and DatasetPrepare overload/missing-entry checks, then container wiring.
