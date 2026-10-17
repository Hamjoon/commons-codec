"""Coverage-only helpers for the Part 3 addendum; independent of Phase A."""
import xml.etree.ElementTree as ET
from survival_common import SCOPE_FILES

SCOPE = {p.removeprefix('src/main/java/') for p in SCOPE_FILES}


def file_group(path):
    return 'scope' if path in SCOPE else 'other'


def line_rows(xml):
    rows = []
    for package in ET.parse(xml).getroot().findall('package'):
        name = package.get('name')
        if name != 'org/apache/commons/codec' and not name.startswith('org/apache/commons/codec/'):
            continue
        for source in package.findall('sourcefile'):
            filename = name + '/' + source.get('name')
            for line in source.findall('line'):
                rows.append({'file': filename, 'line': int(line.get('nr')),
                             'instructions-missed': int(line.get('mi')),
                             'instructions-covered': int(line.get('ci'))})
    rows.sort(key=lambda r: (r['file'], r['line']))
    assert len({(r['file'], r['line']) for r in rows}) == len(rows), 'Duplicate source-line entries'
    return rows


def flatten(mapping):
    return {(file, number) for file, numbers in mapping.items() for number in numbers}


def measure_lines(changed, maps):
    """Count changes against a common executable universe, preserving unavailable data."""
    available = [m for m in maps.values() if m is not None]
    executable = changed & available[0].keys() if available else None
    if len(available) == 2:
        assert available[0].keys() == available[1].keys()
    return {'changed_lines': len(changed),
            'executable_lines': len(executable) if executable is not None else '',
            **{g + '_covered': sum(m[p] for p in executable) if executable is not None and m is not None else ''
               for g, m in maps.items()}}
