#!/usr/bin/env python3
"""Compile each frozen generated file and measure three isolated JUnit runs at t."""
import argparse
import collections
import csv
import hashlib
import json
import os
from pathlib import Path
import re
import shutil
import signal
import subprocess
import time
import xml.etree.ElementTree as ET

from tree_sitter import Language, Parser
import tree_sitter_java

EXP = Path(__file__).resolve().parents[1]
RESULTS = EXP / 'results'
TEST_ANNOTATIONS = {'Test', 'ParameterizedTest', 'RepeatedTest', 'TestFactory', 'TestTemplate'}


def save_json(path, value):
    path.write_text(json.dumps(value, indent=2, ensure_ascii=False) + '\n')


def save_csv(path, fields, rows):
    with path.open('w', newline='') as stream:
        writer = csv.DictWriter(stream, fieldnames=fields, lineterminator='\n', extrasaction='ignore')
        writer.writeheader()
        writer.writerows(rows)


def source_methods(source):
    """Use Java syntax nodes, preserving literal contents when normalizing bodies."""
    parser = Parser(Language(tree_sitter_java.language()))
    root = parser.parse(source).root_node
    if root.has_error:
        raise ValueError('Compiled source has a tree-sitter parse error')
    package = next((n for n in root.named_children if n.type == 'package_declaration'), None)
    pkg = re.sub(rb'\s+', b'', source[package.start_byte:package.end_byte]).decode()[7:-1] if package else ''
    methods = {}

    def compact(node):
        if node.type in {'line_comment', 'block_comment'}:
            return b''
        if node.type in {'string_literal', 'character_literal', 'text_block'} or not node.children:
            return source[node.start_byte:node.end_byte]
        return b''.join(compact(child) for child in node.children)

    def walk(node, classes=()):
        if node.type in {'class_declaration', 'interface_declaration', 'enum_declaration', 'record_declaration'}:
            name = node.child_by_field_name('name')
            classes += (source[name.start_byte:name.end_byte].decode(),)
        if node.type == 'method_declaration':
            body = node.child_by_field_name('body')
            if body is None:
                return
            name_node = node.child_by_field_name('name')
            name = source[name_node.start_byte:name_node.end_byte].decode()
            modifiers = next((n for n in node.named_children if n.type == 'modifiers'), None)
            annotations = []
            if modifiers:
                for child in modifiers.named_children:
                    if child.type in {'annotation', 'marker_annotation'}:
                        ann = child.child_by_field_name('name')
                        if ann:
                            annotations.append(source[ann.start_byte:ann.end_byte].decode().rsplit('.', 1)[-1])
            cls = (pkg + '.' if pkg else '') + '$'.join(classes)
            key = cls, name
            entry = {'class': cls, 'method': name, 'line': node.start_point.row + 1,
                     'annotated_test': bool(TEST_ANNOTATIONS.intersection(annotations)),
                     'body_sha256': hashlib.sha256(compact(body)).hexdigest(),
                     'normalized_body': compact(body).decode(), 'annotations': annotations}
            methods.setdefault(key, []).append(entry)
            return  # Ignore local anonymous implementations inside the method.
        for child in node.named_children:
            walk(child, classes)

    walk(root)
    return methods


def execute(command, log, limit):
    start = time.monotonic()
    with log.open('w') as stream:
        process = subprocess.Popen(command, cwd=EXP, stdout=stream, stderr=subprocess.STDOUT, start_new_session=True)
        timed_out = False
        try:
            rc = process.wait(timeout=limit)
        except subprocess.TimeoutExpired:
            timed_out = True
            os.killpg(process.pid, signal.SIGKILL)
            rc = process.wait()
    return {'command': command, 'exit_code': rc, 'timeout': timed_out,
            'seconds': round(time.monotonic() - start, 3), 'log': str(log.relative_to(EXP))}


def parse_xml(folder):
    observations = collections.defaultdict(list)
    cases = []
    for path in sorted(folder.glob('TEST-*.xml')):
        root = ET.parse(path).getroot()
        for case in root.iter('testcase'):
            cls, display = case.get('classname'), case.get('name')
            output = '\n'.join(n.text or '' for n in case.findall('system-out'))
            match = re.search(r'\[(?:method|test-template|test-factory):([^\]]+)\]', output)
            label = match.group(1) if match else display
            name = re.match(r'^([A-Za-z_$][\w$]*)(?:\(|\[|$)', label or '')
            if not name:
                raise ValueError('Cannot map JUnit case to source method: ' + repr((cls, display, output)))
            error, failure = case.find('error'), case.find('failure')
            problem = error if error is not None else failure
            if case.find('skipped') is not None:
                state = 'skipped'
            elif problem is not None:
                kind = problem.get('type', '')
                state = 'fail' if ('Assertion' in kind or kind.endswith(('ComparisonFailure', 'MultipleFailuresError'))) else 'error'
            else:
                state = 'pass'
            key = cls, name.group(1)
            observations[key].append(state)
            cases.append({'class': cls, 'method': key[1], 'raw-name': display, 'status': state,
                          'problem-type': problem.get('type', '') if problem is not None else '',
                          'report': str(path.relative_to(EXP))})
    return observations, cases


def reduce_status(states):
    return next((s for s in ['timeout', 'error', 'fail', 'skipped'] if s in states), 'pass') if states else 'skipped'


def validate_file(path, round_number, jars):
    raw = path.read_bytes()
    rel = str(path.relative_to(EXP))
    filename = re.sub(r'^r\d+-', '', path.name)
    pkg_match = re.search(rb'\bpackage\s+([\w.]+)\s*;', raw)
    main_class = (pkg_match.group(1).decode() + '.' if pkg_match else '') + filename[:-5]
    work = EXP / f'work/llm-round{round_number}' / path.stem
    assert not work.exists(), 'Refusing to overwrite validation evidence: ' + str(work)
    (work / 'input').mkdir(parents=True)
    (work / 'classes').mkdir()
    copied = work / 'input' / filename
    shutil.copyfile(path, copied)
    cp = ':'.join(['work/t/target/classes', *jars])
    compile_run = execute(['javac', '-cp', cp, '-d', str((work / 'classes').relative_to(EXP)),
                           str(copied.relative_to(EXP))], work / 'javac.txt', 600)
    file_result = {'file': rel, 'class': main_class, 'round': round_number,
                   'source_sha256': hashlib.sha256(raw).hexdigest(), 'compile': compile_run,
                   'compile_status': 'compiled' if compile_run['exit_code'] == 0 else 'compile-fail',
                   'first_javac_error': '', 'methods_listed': 0, 'pass3': 0, 'runs': []}
    if compile_run['exit_code'] != 0:
        lines = (work / 'javac.txt').read_text(errors='replace').splitlines()
        file_result['first_javac_error'] = next((x for x in lines if 'error:' in x), 'javac timeout' if compile_run['timeout'] else next(iter(lines), 'javac failed'))
        save_json(work / 'result.json', {'file': file_result, 'methods': [], 'cases': []})
        return file_result, [], []
    inventory = source_methods(raw)
    expected = {k for k, values in inventory.items() if any(v['annotated_test'] for v in values)}
    observations, all_cases = [], []
    runtime_cp = ':'.join(['work/t/target/classes', str((work / 'classes').relative_to(EXP)), *jars])
    for index in range(1, 4):
        folder = work / f'run{index}'
        folder.mkdir()
        command = ['java', '-cp', runtime_cp, 'org.junit.platform.console.ConsoleLauncher',
                   '--disable-banner', '--disable-ansi-colors', '--fail-if-no-tests',
                   '--select-class', main_class, '--details', 'summary',
                   '--reports-dir', str(folder.relative_to(EXP))]
        outcome = execute(command, folder / 'console.txt', 600)
        obs, cases = parse_xml(folder)
        outcome['xml_cases'] = len(cases)
        outcome['xml_files'] = len(list(folder.glob('TEST-*.xml')))
        file_result['runs'].append(outcome)
        observations.append(obs)
        all_cases += [{'file': rel, 'run': index, **case} for case in cases]
    observed = set().union(*(set(obs) for obs in observations))
    methods = []
    for cls, name in sorted(expected | observed):
        candidates = inventory.get((cls, name), [])
        if not candidates:
            # XML may name a concrete subclass for a locally declared inherited test.
            candidates = [v for (c, n), values in inventory.items() if n == name for v in values]
        if len(candidates) != 1:
            raise ValueError(f'Ambiguous or external test body for {rel}: {cls}#{name}')
        body = candidates[0]
        states = []
        for index, obs in enumerate(observations):
            run = file_result['runs'][index]
            if run['timeout']:
                state = 'timeout'
            elif (cls, name) in obs:
                state = reduce_status(obs[(cls, name)])
            elif run['exit_code'] not in (0, 2) or not run['xml_files']:
                state = 'error'
            else:
                state = 'skipped'
            states.append(state)
        methods.append({'file': rel, 'class': cls, 'method': name, 'round': round_number,
                        'source_line': body['line'],
                        **{f'run{i}': state for i, state in enumerate(states, 1)},
                        'status': 'pass3' if states == ['pass'] * 3 else 'not-pass3',
                        'body_sha256': body['body_sha256'], 'normalized_body': body['normalized_body']})
    file_result['methods_listed'] = len(methods)
    file_result['pass3'] = sum(m['status'] == 'pass3' for m in methods)
    assert hashlib.sha256(path.read_bytes()).hexdigest() == file_result['source_sha256']
    save_json(work / 'result.json', {'file': file_result, 'methods': methods, 'cases': all_cases})
    return file_result, methods, all_cases


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('--round', type=int, choices=[1, 2, 3], required=True)
    args = ap.parse_args()
    records = json.loads((EXP / 'tools/classpath-artifacts.json').read_text())
    runtime = sorted([r for r in records if r['role'] == 'test'], key=lambda r: r['filename'].startswith('junit-platform-console-standalone'))
    assert len(runtime) == 18
    jars = ['work/libs/' + r['filename'] for r in runtime]
    for record, jar in zip(runtime, jars):
        assert (EXP / jar).is_file()
        assert hashlib.sha256((EXP / jar).read_bytes()).hexdigest() == record['supplied_sha256']
    assert (EXP / 'work/t/target/classes/org/apache/commons/codec/binary/Base64.class').exists()
    sources = sorted((EXP / f'generated/round{args.round}').glob('*.java'))
    assert sources
    summary_path = RESULTS / f'llm-round{args.round}-validation.json'
    assert not summary_path.exists(), 'Round already validated'
    start = time.monotonic()
    files, methods, cases = [], [], []
    for position, source in enumerate(sources, 1):
        item, rows, raw = validate_file(source, args.round, jars)
        files.append(item)
        methods += rows
        cases += raw
        print(json.dumps({'file': source.name, 'position': position, 'total': len(sources),
                          'compile': item['compile_status'], 'listed': len(rows), 'pass3': item['pass3']}), flush=True)
    save_csv(RESULTS / f'llm-methods-round{args.round}.csv', ['file', 'class', 'method', 'run1', 'run2', 'run3', 'status'], methods)
    save_csv(RESULTS / f'llm-cases-round{args.round}.csv', ['file', 'run', 'class', 'method', 'raw-name', 'status', 'problem-type', 'report'], cases)
    save_json(summary_path, {'round': args.round, 'files': files,
                            'methods': [{k: v for k, v in m.items() if k != 'normalized_body'} for m in methods],
                            'seconds': round(time.monotonic() - start, 3), 'classpath': jars,
                            'normalization': 'Java syntax tree: remove comments and formatting whitespace, preserve identifiers and literal contents; compare complete bodies across rounds'})


if __name__ == '__main__':
    main()
