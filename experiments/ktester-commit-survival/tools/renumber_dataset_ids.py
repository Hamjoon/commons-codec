#!/usr/bin/env python3
"""Assign deterministic naming fields from preserved DatasetPrepare output."""
import argparse
import collections
import copy
import csv
import hashlib
import io
import json
from pathlib import Path
import shutil

PUBLIC = Path(__file__).resolve().parents[1] / 'population'
NAMING = {'id', 'test-class', 'test-path'}
RULE = 'Within each (class, method name) group, sort by parameter count, then parameter-list string (case-sensitive lexicographic order); assign base ID, _2, _3, and so on.'


def sha(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def group_key(entry):
    return entry['class'], entry['method-name'].split('(', 1)[0]


def order_key(entry):
    signature = entry['method-name']
    assert signature.endswith(')')
    params = signature.split('(', 1)[1][:-1]
    return len(params.split(', ')) if params else 0, params


def renumber(raw):
    result = copy.deepcopy(raw)
    entries = result['commons-codec']['focal-methods']
    groups = collections.defaultdict(list)
    for entry in entries:
        groups[group_key(entry)].append(entry)
    for (cls, method), group in groups.items():
        assert len({m['method-name'] for m in group}) == len(group)
        for index, entry in enumerate(sorted(group, key=order_key), 1):
            ident = cls.rsplit('.', 1)[1] + '_' + method + ('' if index == 1 else '_' + str(index))
            if ident != entry['id']:
                entry['id'] = ident
                entry['test-class'] = entry['package'] + '.' + ident + '_Test'
                entry['test-path'] = 'src/test/java/' + entry['test-class'].replace('.', '/') + '.java'
    return result


def validate(raw, result):
    old = raw['commons-codec']['focal-methods']
    new = result['commons-codec']['focal-methods']
    assert len(old) == len(new) == 66
    assert len({m['id'] for m in new}) == 66
    assert all(m['test-class'] == m['package'] + '.' + m['id'] + '_Test' for m in new)
    assert all(m['test-path'] == 'src/test/java/' + m['test-class'].replace('.', '/') + '.java' for m in new)
    assert all(m['focal-method'].strip() and m['class-code'].strip() for m in new)
    restored = copy.deepcopy(result)
    for before, after in zip(old, restored['commons-codec']['focal-methods']):
        for field in NAMING:
            after[field] = before[field]
    assert restored == raw, 'Non-naming fields or entry order changed'
    assert renumber(result) == result, 'Renumbering must be idempotent'
    frequencies = collections.Counter(m['id'] for m in old)
    changed = [{'old-id': a['id'], 'new-id': b['id'], 'class': a['class'], 'method-signature': a['method-name']}
               for a, b in zip(old, new) if a['id'] != b['id']]
    return {'valid': True, 'rule': RULE, 'entries': len(new), 'unique_ids': len({m['id'] for m in new}),
            'raw_unique_ids': len(frequencies), 'changed_ids': len(changed), 'unchanged_ids': len(new) - len(changed),
            'previously_unique_ids_changed': [m for m in changed if frequencies[m['old-id']] == 1],
            'non_naming_fields_unchanged': True, 'entry_order_unchanged': True,
            'test_class_and_path_consistent': True, 'nonempty_code_fields': True, 'idempotent': True}


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('--kt', required=True, type=Path)
    args = ap.parse_args()
    directory = args.kt / 'data/commit-survival/commons-codec'
    raw_path = directory / 'dataset_info.raw.json'
    raw = json.loads(raw_path.read_text())
    previous = json.loads((directory / 'population-validation.json').read_text())
    assert sha(raw_path) == previous['output_sha256']['dataset_info.json'], 'Raw output changed'
    result = renumber(raw)
    validation = validate(raw, result)
    output = directory / 'dataset_info.json'
    output.write_text(json.dumps(result, indent=2, ensure_ascii=False) + '\n')
    mapping = io.StringIO()
    writer = csv.DictWriter(mapping, fieldnames=['old-id', 'new-id', 'class', 'method-signature'], lineterminator='\n')
    writer.writeheader()
    for old, new in zip(raw['commons-codec']['focal-methods'], result['commons-codec']['focal-methods']):
        writer.writerow({'old-id': old['id'], 'new-id': new['id'], 'class': old['class'], 'method-signature': old['method-name']})
    (directory / 'id-renumbering.csv').write_text(mapping.getvalue())
    validation['invocation'] = 'python3 experiments/ktester-commit-survival/tools/renumber_dataset_ids.py --kt ' + str(args.kt)
    validation['sha256'] = {name: sha(directory / name) for name in ['dataset_info.raw.json', 'dataset_info.json', 'id-renumbering.csv']}
    (directory / 'id-renumbering-validation.json').write_text(json.dumps(validation, indent=2) + '\n')
    readme = (directory / 'README.md').read_text().split('\n## Stop\n')[0].split('\n## Renumbering\n')[0]
    readme = readme.replace('Status: **blocked_dataset_prepare_validation**. The raw DatasetPrepare output is diagnostic evidence and is not ready for test generation.',
                            'Status: **validated after addendum 3 renumbering**. dataset_info.json contains 66 unique IDs; dataset_info.raw.json preserves the original extraction. Historical raw-output checks are retained below.')
    readme = readme.replace('No output IDs were rewritten.', 'That describes raw extraction order; addendum 3 naming changes are recorded below.')
    readme = readme.replace('## DatasetPrepare validation', '## Raw DatasetPrepare validation (historical)')
    readme = readme.replace('## Original-entry field comparison', '## Raw original-entry field comparison')
    readme = readme.replace('Raw dataset_meta.json and dataset_info.json are retained in this directory and copied byte-for-byte to the public experiment population directory.',
                            'dataset_meta.json, the preserved dataset_info.raw.json and the renumbered dataset_info.json are tracked in both repositories.')
    lines = [readme.rstrip(), '', '## Renumbering', '', RULE, '',
             'Invocation (from commons-codec): `' + validation['invocation'] + '`. Evidence: id-renumbering-validation.json; id-renumbering.csv lists all entries, including unchanged IDs.', '',
             f"Validation: {validation['entries']} entries, {validation['unique_ids']} distinct IDs; {validation['changed_ids']} IDs changed and {validation['unchanged_ids']} remained unchanged. Every test-class and test-path follows the author naming convention. No other field changed, including focal-method and class-code; entry order is preserved. Applying the transformation again is idempotent.", '',
             f"The explicit sort also changes {len(validation['previously_unique_ids_changed'])} previously unique IDs. The addendum’s sentence claiming only duplicates change does not hold for this ordering; the explicit sort rule governs. Affected previously unique IDs:", '',
             '| Method | Old ID | New ID |', '|---|---|---|']
    for item in validation['previously_unique_ids_changed']:
        lines.append(f"| {item['class']}.{item['method-signature']} | {item['old-id']} | {item['new-id']} |")
    lines += ['', 'The original Md5Crypt.md5Crypt(byte[], String, String, Random) entry is now Md5Crypt_md5Crypt_5. Its id, test-class and test-path change; all other original-entry fields still match the authors up to line endings. The four Base32/Base64 original entries keep their naming fields. No Java source, packaged jar, authors’ dataset, prompts or settings were changed.']
    (directory / 'README.md').write_text('\n'.join(lines) + '\n')
    for name in ['dataset_info.raw.json', 'dataset_info.json', 'id-renumbering.csv', 'id-renumbering-validation.json', 'README.md']:
        shutil.copyfile(directory / name, PUBLIC / name)
    print(json.dumps(validation, indent=2))


if __name__ == '__main__':
    main()
