"""Reject signal-killed measurements and retain their original bytes in quarantine."""
import datetime
import fcntl
import hashlib
import json
from pathlib import Path
from survival_common import EXP, RESULTS, write_json


def sigkill_exits(value, path='$'):
    found = []
    if isinstance(value, dict):
        if value.get('exit_code') == -9:
            found.append({'path': path, 'command': value.get('command', []),
                          'log': value.get('log'), 'timeout': value.get('timeout')})
        for key, child in value.items():
            found.extend(sigkill_exits(child, path + '.' + key))
    elif isinstance(value, list):
        for i, child in enumerate(value):
            found.extend(sigkill_exits(child, f'{path}[{i}]'))
    return found


def assert_no_sigkill(record, label):
    hits = sigkill_exits(record)
    assert not hits, f'{label}: signal-9 process exit invalidates the measurement: {hits}'


def quarantine(phase):
    """Scan atomic completed raw files; never touch a live worker or a valid record."""
    folder = RESULTS / ('raw' if phase == 'a' else 'raw-coverage')
    target = EXP / 'work/quarantine'
    if phase != 'a':
        target /= 'phase-' + phase
    target.mkdir(parents=True, exist_ok=True)
    manifest = RESULTS / f'phase-{phase}-quarantine.json'
    lock_path = EXP / f'work/phase-{phase}-quarantine.lock'
    with lock_path.open('a') as lock:
        fcntl.flock(lock, fcntl.LOCK_EX)
        saved = json.loads(manifest.read_text()) if manifest.exists() else {'phase': phase, 'entries': []}
        moved = []
        for path in sorted(folder.glob('*.json')):
            raw = json.loads(path.read_text())
            hits = sigkill_exits(raw)
            if not hits:
                continue
            digest = hashlib.sha256(path.read_bytes()).hexdigest()
            destination = target / path.name
            if destination.exists():
                destination = target / (path.stem + '-' + digest[:16] + '.json')
            assert not destination.exists(), f'Quarantine destination already exists: {destination}'
            path.rename(destination)
            entry = {'index': raw['index'], 'group': raw['group'], 'source': str(path.relative_to(EXP)),
                     'quarantine': str(destination.relative_to(EXP)), 'sha256': digest,
                     'sigkill_exits': hits, 'quarantined_at': datetime.datetime.now(datetime.timezone.utc).isoformat()}
            # Coverage records must not leave stale XML/CSV at their eventual replacement paths.
            if phase == 'b' and raw.get('coverage'):
                artifacts = []
                archive = target / digest
                archive.mkdir(exist_ok=True)
                for key in ['xml', 'lines_csv']:
                    source = EXP / raw['coverage'][key]
                    if source.exists():
                        dest = archive / source.name
                        source.rename(dest)
                        artifacts.append({'source': str(source.relative_to(EXP)), 'quarantine': str(dest.relative_to(EXP))})
                entry['coverage_artifacts'] = artifacts
            saved['entries'].append(entry)
            moved.append(entry)
        if moved or not manifest.exists():
            saved['status'] = 'quarantined; repair required before verification'
            write_json(manifest, saved)
        return saved, moved


def archive_reports(phase, entries):
    """Call after the original workers stop, before missing groups are rerun."""
    for entry in entries:
        source = EXP / f"work/phase-{phase}/{entry['index']}/{entry['group']}"
        archive = EXP / 'work/quarantine/reports' / ('phase-' + phase) / entry['sha256']
        if source.exists() and not archive.exists():
            archive.parent.mkdir(parents=True, exist_ok=True)
            source.rename(archive)
        if archive.exists():
            entry['archived_reports'] = str(archive.relative_to(EXP))
    # A killed Maven invocation must not be reused through the build-fail cache.
    for path in (RESULTS / 'builds' / phase).glob('*.json'):
        build = json.loads(path.read_text())
        if not sigkill_exits(build):
            continue
        digest = hashlib.sha256(path.read_bytes()).hexdigest()
        archive = EXP / 'work/quarantine/builds' / phase / digest
        archive.mkdir(parents=True, exist_ok=True)
        assert not (archive / path.name).exists()
        path.rename(archive / path.name)
        for stamp in (EXP / 'work/commits').glob('w*/.part3-sha'):
            if stamp.read_text() == build['sha']:
                stamp.rename(archive / (stamp.parent.name + '-export-sha'))
        for attempt in build['attempts']:
            log = EXP / attempt['log']
            if log.exists():
                log.rename(archive / log.name)
