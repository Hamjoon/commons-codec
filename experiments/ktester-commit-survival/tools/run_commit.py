"""Measure one commit/group, with isolated compile units and atomic raw records."""
import argparse
import collections
import json
from pathlib import Path
import re
import shutil
import time
from survival_common import (EXP, RESULTS, check_inputs, classpath, commits, denominator,
    execute, export_build, line_rows, method_state, parse_reports, relative, selectors,
    write_csv, write_json, runner_hashes)


def raw_path(index, group, phase):
    return RESULTS / ('raw' if phase == 'a' else 'raw-coverage' if phase == 'b' else 'runner-' + phase) / f'{index}-{group}.json'


def run_group(index, group, worker, phase='a', selected_ids=None, coverage=False, force_class=False):
    started = time.monotonic()
    fingerprint = check_inputs()
    commit = commits()[index]
    rows = denominator(group)
    if selected_ids is not None:
        unknown = set(selected_ids) - {r['id'] for r in rows}
        assert not unknown, unknown
        rows = [r for r in rows if r['id'] in set(selected_ids)]
    output = raw_path(index, group, phase)
    if output.exists():
        saved = json.loads(output.read_text())
        assert saved['sha'] == commit['sha'] and saved['input_sha256'] == fingerprint
        assert saved['requested_ids'] == sorted(r['id'] for r in rows)
        assert saved['coverage_requested'] == coverage
        if phase in {'a', 'b'}:
            assert saved['runner_sha256'] == runner_hashes(), 'Runner changed since this measurement'
        return saved
    folder, build = export_build(index, worker, phase)
    record = {'index': index, 'sha': commit['sha'], 'date': commit['date'], 'group': group, 'phase': phase,
              'worker': worker, 'input_sha256': fingerprint, 'requested_ids': sorted(r['id'] for r in rows),
              'coverage_requested': coverage, 'build': build, 'files': [], 'methods': [], 'fallbacks': [],
              'runner_sha256': runner_hashes(),
              'status': 'build-fail' if build['status'] == 'build-fail' else 'evaluated'}
    if build['status'] == 'build-fail':
        record['seconds'] = round(time.monotonic() - started, 3)
        write_json(output, record)
        return record
    work = EXP / f'work/phase-{phase}/{index}/{group}'
    if work.exists():
        shutil.rmtree(work)  # Only incomplete evidence for this unrecorded group.
    work.mkdir(parents=True)
    execfile = work / 'jacoco.exec'
    jars = classpath()
    units = {u['file']: u for u in json.loads((EXP / 'tools/dev-compile-units.json').read_text())['units']}
    by_file = collections.defaultdict(list)
    for row in rows:
        by_file[row['file']].append(row)
    for number, (filename, members) in enumerate(sorted(by_file.items()), 1):
        part = work / f'{number:02d}-{Path(filename).stem}'
        classes = part / 'classes'
        classes.mkdir(parents=True)
        source = EXP / filename
        copied = part / re.sub(r'^r\d+-', '', source.name)
        shutil.copyfile(source, copied)
        sources = [copied, *[EXP / p for p in units[filename]['helpers']]] if group == 'dev' else [copied]
        cp = ':'.join([str(folder / 'target/classes'), *jars])
        compile_runs = []
        for attempt in range(1, 3):
            compile_runs.append(execute(['javac', '-cp', cp, '-d', classes, *sources], folder, part / f'javac-{attempt}.txt'))
            if compile_runs[-1]['exit_code'] == 0:
                break
        file_record = {'file': filename, 'compile_attempts': compile_runs,
                       'compile_status': 'compiled' if compile_runs[-1]['exit_code'] == 0 else 'compile-fail',
                       'first_javac_error': '', 'runs': []}
        record['files'].append(file_record)
        if file_record['compile_status'] == 'compile-fail':
            text = (EXP / compile_runs[-1]['log']).read_text(errors='replace').splitlines()
            message = next((s for s in text if 'error:' in s), 'javac timed out' if compile_runs[-1]['timeout'] else '\n'.join(text[:4]))
            file_record['first_javac_error'] = message
            record['methods'] += [{**{k: r[k] for k in ['id', 'file', 'class', 'method']}, 'state': 'compile-fail', 'message': message, 'flaky': False} for r in members]
            continue
        runtime = ':'.join([str(folder / 'target/classes'), str(classes), str(EXP / 'dev/src-test-t/resources'), *jars])
        selection = selectors(members, classes, part / 'selectors.txt', runtime)
        base = ['java']
        if coverage:
            base += [f'-javaagent:{EXP}/work/libs/org.jacoco.agent-0.8.12-runtime.jar=destfile={execfile},append=true,includes=org.apache.commons.codec.*']
        base += ['-cp', runtime, 'org.junit.platform.console.ConsoleLauncher', '--disable-banner', '--disable-ansi-colors', '--fail-if-no-tests', '--details', 'summary']
        def launch(chosen, label, whole_class=False):
            report = part / label
            report.mkdir()
            args = []
            if whole_class:
                for cls in sorted({r['class'].split('$')[0] for r in chosen}):
                    args += ['--select-class', cls]
            else:
                for r in chosen:
                    for selector in selection[r['id']]:
                        args += ['--select-method', selector]
            result = execute([*base, *args, '--reports-dir', report], folder, report / 'console.txt')
            observed, cases = parse_reports(report)
            result.update({'selection_mode': 'class' if whole_class else 'method', 'selected_ids': [r['id'] for r in chosen], 'xml_cases': len(cases)})
            file_record['runs'].append(result)
            return result, observed
        whole_class = force_class or (group == 'dev' and phase in {'a', 'check'} and selected_ids is None)
        run, obs = launch(members, 'initial', whole_class)
        text = (EXP / run['log']).read_text(errors='replace')
        if coverage and not whole_class and not obs and any(x in text for x in ['MethodSelector', 'Could not find method', 'failed to discover tests']):
            record['fallbacks'].append({'file': filename, 'reason': 'Method selector rejected by launcher', 'log': run['log'], 'selected_ids': [r['id'] for r in members]})
            run, obs = launch(members, 'class-fallback', True)
        for row in members:
            state, message = method_state(row, obs, run)
            method = {**{k: row[k] for k in ['id', 'file', 'class', 'method']}, 'state': state,
                      'message': message, 'flaky': False, 'initial_state': state, 'selector': selection[row['id']]}
            # Retry only the failing denominator method, once, keeping both outcomes.
            if state in {'fail', 'error'}:
                retry, retry_obs = launch([row], 'retry-' + row['method'])
                retry_state, retry_message = method_state(row, retry_obs, retry)
                method.update({'retry_state': retry_state, 'retry_message': retry_message, 'retry_log': retry['log']})
                if retry_state == 'pass':
                    method.update(state='pass', flaky=True)
            record['methods'].append(method)
        write_json(part / 'result.json', {'file': file_record, 'methods': record['methods'][-len(members):]})
    if coverage:
        xml = RESULTS / ('coverage' if phase == 'b' else 'runner-' + phase) / f'{index}-{group}-coverage.xml'
        if phase == 'b':
            xml = RESULTS / 'coverage' / f'{index}-{group}.xml'
        xml.parent.mkdir(parents=True, exist_ok=True)
        command = ['java', '-jar', str(EXP / 'work/libs/org.jacoco.cli-0.8.12-nodeps.jar'), 'report', *([str(execfile)] if execfile.exists() else []), '--classfiles', str(folder / 'target/classes'), '--sourcefiles', str(folder / 'src/main/java'), '--xml', str(xml)]
        coverage_run = execute(command, folder, work / 'coverage-report.txt')
        if coverage_run['exit_code']:
            coverage_run = execute(command, folder, work / 'coverage-report-retry.txt')
        if coverage_run['exit_code']:
            raise RuntimeError('Coverage report failed twice: ' + coverage_run['log'])
        lines = line_rows(xml)
        csv_path = xml.with_name(xml.stem + '-lines.csv')
        write_csv(csv_path, ['file', 'line', 'instructions-missed', 'instructions-covered'], lines)
        record['coverage'] = {'xml': relative(xml), 'lines_csv': relative(csv_path), 'report': coverage_run,
                              'line_entries': len(lines), 'covered_lines': sum(r['instructions-covered'] > 0 for r in lines)}
    record['seconds'] = round(time.monotonic() - started, 3)
    assert len(record['methods']) == len(rows)
    write_json(output, record)
    return record


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--index', type=int, required=True)
    parser.add_argument('--group', choices=['dev', 'llm'], required=True)
    parser.add_argument('--worker', default='w0')
    parser.add_argument('--phase', choices=['a', 'b', 'check', 'check-coverage'], default='a')
    parser.add_argument('--selection', type=Path, help='JSON array of frozen method ids')
    parser.add_argument('--coverage', action='store_true')
    parser.add_argument('--class-selection', action='store_true', help='Explicit whole-class smoke check only')
    args = parser.parse_args()
    ids = json.loads(args.selection.read_text()) if args.selection else None
    result = run_group(args.index, args.group, args.worker, args.phase, ids, args.coverage, args.class_selection)
    print(json.dumps({'index': args.index, 'group': args.group, 'status': result['status'], 'states': dict(collections.Counter(r['state'] for r in result['methods'])), 'seconds': result['seconds']}))


if __name__ == '__main__':
    main()
