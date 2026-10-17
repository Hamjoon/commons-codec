#!/usr/bin/env python3
"""Report cumulative pass-three counts, exact body duplicates and the frozen set."""
import argparse
import csv
import hashlib
import json
from pathlib import Path
from validate_llm_round import source_methods, save_csv, save_json

EXP = Path(__file__).resolve().parents[1]
RESULTS = EXP / 'results'


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--freeze', action='store_true')
    args = parser.parse_args()
    reports = [json.loads(p.read_text()) for p in sorted(RESULTS.glob('llm-round*-validation.json'))]
    assert reports
    assert [r['round'] for r in reports] == list(range(1, len(reports) + 1))
    denominator_path = RESULTS / 'llm-denominator.csv'
    assert not denominator_path.exists(), 'The denominator is already frozen'
    dev = list(csv.DictReader((EXP / 'dev/dev-denominator.csv').open()))
    assert len(dev) == 143
    retained, duplicates, bodies, parsed = [], [], {}, {}
    counts = []
    all_files = []
    for report in reports:
        all_files += report['files']
        for f in report['files']:
            assert hashlib.sha256((EXP / f['file']).read_bytes()).hexdigest() == f['source_sha256']
        passing = [m for m in report['methods'] if m['status'] == 'pass3']
        before = len(retained)
        for row in sorted(passing, key=lambda m: (m['file'], m.get('source_line', 0), m['class'], m['method'])):
            if row['file'] not in parsed:
                parsed[row['file']] = source_methods((EXP / row['file']).read_bytes())
            candidates = parsed[row['file']].get((row['class'], row['method']), [])
            if not candidates:
                candidates = [v for (cls, name), values in parsed[row['file']].items() if name == row['method'] for v in values]
            assert len(candidates) == 1
            method = candidates[0]
            assert method['body_sha256'] == row['body_sha256']
            body = method['normalized_body']
            if body in bodies:
                earlier = bodies[body]
                duplicates.append({'file': row['file'], 'class': row['class'], 'method': row['method'], 'round': row['round'],
                                   'kept-file': earlier['file'], 'kept-class': earlier['class'],
                                   'kept-method': earlier['method'], 'kept-round': earlier['round'],
                                   'body-sha256': row['body_sha256']})
            else:
                bodies[body] = row
                retained.append(row)
        counts.append({'round': report['round'], 'files_produced': len(report['files']),
                       'files_compiled': sum(f['compile_status'] == 'compiled' for f in report['files']),
                       'methods_listed': len(report['methods']), 'pass3': len(passing),
                       'new_unique': len(retained) - before, 'cumulative_denominator': len(retained),
                       'duplicates_removed': len(passing) - (len(retained) - before)})
    save_csv(RESULTS / 'llm-duplicates.csv', ['file', 'class', 'method', 'round', 'kept-file', 'kept-class', 'kept-method', 'kept-round', 'body-sha256'], duplicates)
    lines = ['# LLM tests at t', '', 'Developer denominator: **143**.', '',
             '| Round | Files | Compiled | Methods listed | Pass all three | New unique | Cumulative unique |',
             '|---:|---:|---:|---:|---:|---:|---:|']
    for r in counts:
        lines.append('| ' + ' | '.join(str(r[k]) for k in ['round', 'files_produced', 'files_compiled', 'methods_listed', 'pass3', 'new_unique', 'cumulative_denominator']) + ' |')
    lines += ['', f"LLM denominator {'frozen' if args.freeze else 'so far'}: **{len(retained)}**, versus **143** developer methods. Duplicates removed: **{len(duplicates)}**. Shortfall: **{max(0, 143 - len(retained))}**.", '',
              'Each source is compiled independently against production classes at t and the 18 fixed test jars. Each compiled class is launched three times in separate JVMs with a 600-second wall-clock limit per run. Compile failures are recorded with the first javac error and contribute no listed methods. Files and commands are in llm-roundN-validation.json; per-case and per-method outcomes are in the corresponding CSVs. Raw logs/XML remain under ignored work/llm-roundN/.', '',
              'Parameterized/repeated invocations count as one method; every invocation must pass. Any class timeout marks all methods timeout for that run. Assertion failures are fail; other exceptions are error; disabled or unreported source test methods are skipped (or error when the launcher fails). Source-annotated tests and XML-discovered tests are included in the listed count.', '',
              'De-duplication compares exact Java method bodies after removing comments and formatting whitespace, retaining identifiers and literal contents. Order is round, file name, source line, class and method. Later identical bodies are excluded even across rounds; llm-duplicates.csv links each excluded method to the retained one. Source files are never rewritten.']
    (RESULTS / 'llm-funnel.md').write_text('\n'.join(lines) + '\n')
    summary = {'rounds': counts, 'llm_denominator': len(retained), 'developer_denominator': 143,
               'duplicates_removed': len(duplicates), 'shortfall': max(0, 143 - len(retained)), 'frozen': args.freeze}
    if args.freeze:
        assert len(retained) >= 143 or len(reports) == 3, 'Another full round is required'
        save_csv(denominator_path, ['file', 'class', 'method', 'round'], retained)
        manifest = ['# Frozen generated-test sources', '',
                    'Frozen after Part 2 validation. Do not edit or regenerate these sources. Only methods listed in ../results/llm-denominator.csv enter the LLM denominator.', '',
                    '| File | Round | Compile status | Listed methods | Pass three | Retained |',
                    '|---|---:|---|---:|---:|---:|']
        for f in all_files:
            kept = sum(r['file'] == f['file'] for r in retained)
            manifest.append(f"| {f['file']} | {f['round']} | {f['compile_status']} | {f['methods_listed']} | {f['pass3']} | {kept} |")
        manifest += ['', 'Content SHA-256 values are in frozen-source-sha256.json. First javac errors and exact commands are recorded in ../results/llm-roundN-validation.json.']
        (EXP / 'generated/MANIFEST.md').write_text('\n'.join(manifest) + '\n')
        save_json(EXP / 'generated/frozen-source-sha256.json', {f['file']: f['source_sha256'] for f in all_files})
    save_json(RESULTS / 'llm-funnel.json', summary)
    print(json.dumps(summary, indent=2))


if __name__ == '__main__':
    main()
