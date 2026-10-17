"""Fixed inputs and evidence utilities for Part 3; never load model settings."""
import collections
import csv
import hashlib
import io
import json
import os
from pathlib import Path
import re
import shutil
import signal
import subprocess
import tarfile
import time
import xml.etree.ElementTree as ET
from experiment import BASE, EXPERIMENT as EXP, REPO, SCOPE_FILES

RESULTS = EXP / 'results'
SKIPS = ['-Drat.skip', '-Dcheckstyle.skip', '-Dspotbugs.skip', '-Dpmd.skip', '-Dmaven.javadoc.skip']
STATES = {'pass', 'fail', 'error', 'timeout', 'compile-fail', 'absent'}


def relative(path):
    return str(Path(path).relative_to(EXP))


def write_json(path, value):
    path.parent.mkdir(parents=True, exist_ok=True)
    temporary = path.with_suffix(path.suffix + '.tmp')
    temporary.write_text(json.dumps(value, indent=2, ensure_ascii=False) + '\n')
    temporary.replace(path)


def write_csv(path, fields, rows):
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open('w', newline='') as stream:
        writer = csv.DictWriter(stream, fieldnames=fields, extrasaction='ignore', lineterminator='\n')
        writer.writeheader()
        writer.writerows(rows)


def read_csv(path):
    with path.open() as stream:
        return list(csv.DictReader(stream))


def commits():
    rows = read_csv(RESULTS / 'commits.csv')
    assert len(rows) == 539
    zero = {'index': '0', 'sha': BASE, 'date': git('show', '-s', '--format=%cI', BASE).strip(),
            'changes-scope': 'no', 'tags': '', 'subject': git('show', '-s', '--format=%s', BASE).strip()}
    return {int(r['index']): r for r in [zero, *rows]}


def git(*args):
    return subprocess.check_output(['git', '-C', str(REPO), *args], text=True)


def denominator(group):
    rows = read_csv(EXP / ('dev/dev-denominator.csv' if group == 'dev' else 'results/llm-denominator.csv'))
    for row in rows:
        if group == 'dev':
            row['file'] = 'dev/src-test-t/java/' + row['class'].replace('.', '/') + '.java'
        row['id'] = '|'.join(row[k] for k in ['file', 'class', 'method'])
    assert len(rows) == (143 if group == 'dev' else 290)
    assert len({r['id'] for r in rows}) == len(rows)
    return rows


def fixed_inputs():
    paths = [EXP / 'results/commits.csv', EXP / 'results/llm-denominator.csv', EXP / 'dev/dev-denominator.csv']
    for folder in ['generated', 'dev/src-test-t']:
        paths += sorted(p for p in (EXP / folder).rglob('*') if p.is_file())
    return {relative(p): hashlib.sha256(p.read_bytes()).hexdigest() for p in paths}


def check_inputs():
    expected = json.loads((RESULTS / 'part3-input-sha256.json').read_text())
    assert fixed_inputs() == expected, 'Frozen input changed'
    return hashlib.sha256(json.dumps(expected, sort_keys=True).encode()).hexdigest()


def runner_hashes():
    names = ['survival_common.py', 'run_commit.py', 'run_phase.py', 'MethodSelectors.java', 'dev-compile-units.json']
    return {name: hashlib.sha256((EXP / 'tools' / name).read_bytes()).hexdigest() for name in names}


def classpath():
    records = json.loads((EXP / 'tools/classpath-artifacts.json').read_text())
    records = sorted([r for r in records if r['role'] == 'test'], key=lambda r: r['filename'].startswith('junit-platform-console-standalone'))
    assert len(records) == 18
    jars = []
    for r in records:
        p = EXP / 'work/libs' / r['filename']
        assert hashlib.sha256(p.read_bytes()).hexdigest() == r['supplied_sha256']
        jars.append(str(p))
    return jars


def execute(command, cwd, log, limit=600):
    log.parent.mkdir(parents=True, exist_ok=True)
    start = time.monotonic()
    with log.open('w') as stream:
        process = subprocess.Popen([str(x) for x in command], cwd=cwd, stdout=stream, stderr=subprocess.STDOUT, start_new_session=True)
        timeout = False
        try:
            rc = process.wait(timeout=limit)
        except subprocess.TimeoutExpired:
            timeout = True
            os.killpg(process.pid, signal.SIGKILL)
            rc = process.wait()
    return {'command': [str(x) for x in command], 'cwd': str(cwd), 'exit_code': rc,
            'timeout': timeout, 'seconds': round(time.monotonic() - start, 3), 'log': relative(log)}


def export_build(index, worker, phase='a'):
    commit = commits()[index]
    folder = EXP / 'work/commits' / worker
    record = RESULTS / 'builds' / phase / f'{index}.json'
    stamp = folder / '.part3-sha'
    if record.exists():
        previous = json.loads(record.read_text())
        assert previous['sha'] == commit['sha']
        if previous['status'] == 'build-fail':
            return folder, previous
        if stamp.exists() and stamp.read_text() == commit['sha'] and (folder / 'target/classes').is_dir():
            return folder, previous
    if stamp.exists() and stamp.read_text() == commit['sha'] and (folder / 'target/classes').is_dir():
        for previous_record in sorted((RESULTS / 'builds').glob(f'*/{index}.json')):
            previous = json.loads(previous_record.read_text())
            if previous['status'] == 'built' and previous['sha'] == commit['sha']:
                reused = {**previous, 'worker': worker, 'reused_from': relative(previous_record)}
                write_json(record, reused)
                return folder, reused
    assert re.fullmatch(r'w\d+', worker), 'Invalid private worker name'
    if folder.exists():
        shutil.rmtree(folder)
    folder.mkdir(parents=True)
    archive = subprocess.check_output(['git', '-C', str(REPO), 'archive', commit['sha']])
    with tarfile.open(fileobj=io.BytesIO(archive)) as stream:
        stream.extractall(folder, filter='data')
    stamp.write_text(commit['sha'])
    runs = []
    for attempt in range(1, 3):
        offline = attempt == 1 or not any(x in (EXP / runs[-1]['log']).read_text(errors='replace') for x in ['offline mode', 'has not been downloaded', 'Cannot access central'])
        command = ['mvn', '-q', *(['-o'] if offline else []), '-DskipTests', *SKIPS, 'package']
        runs.append(execute(command, folder, EXP / f'work/phase-{phase}/{index}/build-{attempt}.txt', 1800))
        if runs[-1]['exit_code'] == 0 and (folder / 'target/classes').is_dir():
            break
    status = 'built' if runs[-1]['exit_code'] == 0 and (folder / 'target/classes').is_dir() else 'build-fail'
    errors = [line for line in (EXP / runs[-1]['log']).read_text(errors='replace').splitlines() if '[ERROR]' in line]
    result = {'index': index, 'sha': commit['sha'], 'worker': worker, 'status': status, 'attempts': runs,
              'message': '\n'.join(errors[:8]), 'cache_warmed_online': any('-o' not in r['command'] for r in runs)}
    write_json(record, result)
    return folder, result


def parse_reports(folder):
    observations = collections.defaultdict(list)
    cases = []
    for path in sorted(folder.glob('TEST-*.xml')):
        root = ET.parse(path).getroot()
        for case in root.iter('testcase'):
            output = '\n'.join(n.text or '' for n in case.findall('system-out'))
            match = re.search(r'\[(?:method|test-template|test-factory):([^\]]+)\]', output)
            label = match.group(1) if match else case.get('name', '')
            name = re.match(r'^([A-Za-z_$][\w$]*)(?:\(|\[|$)', label)
            if not name:
                raise ValueError('Unmappable JUnit case: ' + repr((case.get('classname'), label)))
            problem = case.find('error')
            if problem is None:
                problem = case.find('failure')
            kind = problem.get('type', '') if problem is not None else ''
            state = ('absent' if case.find('skipped') is not None else
                     ('fail' if 'Assertion' in kind or kind.endswith(('ComparisonFailure', 'MultipleFailuresError')) else 'error') if problem is not None else 'pass')
            item = {'class': case.get('classname'), 'method': name.group(1), 'state': state,
                    'message': (problem.get('message', '') or problem.text or '')[:4000] if problem is not None else '',
                    'problem_type': kind, 'raw_name': case.get('name'), 'xml': relative(path)}
            observations[(item['class'], item['method'])].append(item)
            cases.append(item)
    return observations, cases


def method_state(row, observations, run):
    found = observations.get((row['class'], row['method']), [])
    if run['timeout']:
        return 'timeout', 'Class run exceeded 600 seconds'
    if not found:
        return 'absent', 'Method not reported by launcher'
    for state in ['error', 'fail', 'absent']:
        for item in found:
            if item['state'] == state:
                return state, item['message']
    return 'pass', ''


def descriptor_types(descriptor):
    source = descriptor[1:descriptor.index(')')]
    result = []
    primitive = dict(zip('BCDFIJSZ', ['byte', 'char', 'double', 'float', 'int', 'long', 'short', 'boolean']))
    while source:
        dimensions = len(source) - len(source.lstrip('['))
        source = source[dimensions:]
        if source.startswith('L'):
            stop = source.index(';')
            name, source = source[1:stop].replace('/', '.'), source[stop + 1:]
        else:
            name, source = primitive[source[0]], source[1:]
        result.append(name + '[]' * dimensions)
    return result


def selectors(rows, classes, log, runtime):
    helper = EXP / 'tools/MethodSelectors.java'
    helper_classes = EXP / 'work/runner-tools'
    digest = hashlib.sha256(helper.read_bytes()).hexdigest()
    stamp = helper_classes / 'source.sha256'
    if not stamp.exists() or stamp.read_text() != digest:
        helper_classes.mkdir(parents=True, exist_ok=True)
        result = execute(['javac', '-d', helper_classes, helper], EXP, helper_classes / 'javac.txt')
        assert result['exit_code'] == 0, result
        stamp.write_text(digest)
    selected = {}
    for cls in sorted({r['class'] for r in rows}):
        result = execute(['java', '-cp', str(helper_classes) + ':' + runtime, 'MethodSelectors', cls], EXP, log.with_name(log.stem + '-' + cls.rsplit('.', 1)[-1] + '.txt'))
        if result['exit_code']:
            raise RuntimeError('Compiled signature inspection failed: ' + result['log'])
        candidates = collections.defaultdict(list)
        for line in (EXP / result['log']).read_text().splitlines():
            if '\t' in line:
                name, selector = line.split('\t', 1)
                candidates[name].append(selector)
        for row in rows:
            if row['class'] == cls:
                matches = candidates[row['method']]
                if not matches:
                    raise ValueError('No annotated test selector: ' + row['id'])
                selected[row['id']] = sorted(set(matches))
    return selected


def line_rows(xml):
    root = ET.parse(xml).getroot()
    scope = {p.removeprefix('src/main/java/') for p in SCOPE_FILES}
    rows = []
    for package in root.findall('package'):
        for source in package.findall('sourcefile'):
            filename = package.get('name') + '/' + source.get('name')
            if filename not in scope:
                continue
            for line in source.findall('line'):
                rows.append({'file': filename, 'line': int(line.get('nr')), 'instructions-missed': int(line.get('mi')), 'instructions-covered': int(line.get('ci'))})
    return sorted(rows, key=lambda r: (r['file'], r['line']))
