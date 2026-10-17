"""Resolve developer compile units once from frozen Java references at t."""
import collections
import hashlib
import json
import re
from tree_sitter import Language, Parser
import tree_sitter_java
from survival_common import EXP, RESULTS, denominator, fixed_inputs, relative, write_json


def main():
    parser = Parser(Language(tree_sitter_java.language()))
    source_root = EXP / 'dev/src-test-t/java'
    sources, references = {}, {}
    for path in source_root.rglob('*.java'):
        raw = path.read_bytes()
        package = re.search(rb'\bpackage\s+([\w.]+)\s*;', raw).group(1).decode()
        name = package + '.' + path.stem
        sources[name] = path
        identifiers = set()
        def walk(node):
            if node.type in {'identifier', 'type_identifier'}:
                identifiers.add(raw[node.start_byte:node.end_byte].decode())
            for child in node.named_children:
                walk(child)
        walk(parser.parse(raw).root_node)
        references[name] = identifiers
    units = []
    for cls in sorted({r['class'] for r in denominator('dev')}):
        seen, pending = set(), [cls]
        while pending:
            current = pending.pop()
            if current in seen:
                continue
            seen.add(current)
            package = current.rsplit('.', 1)[0]
            pending += [candidate for candidate in sources if candidate.rsplit('.', 1)[0] == package
                        and candidate.rsplit('.', 1)[1] in references[current] and candidate not in seen]
        helpers = sorted(seen - {cls})
        units.append({'class': cls, 'file': relative(sources[cls]),
                      'helpers': [relative(sources[c]) for c in helpers],
                      'source_sha256': {relative(sources[c]): hashlib.sha256(sources[c].read_bytes()).hexdigest() for c in sorted(seen)}})
    output = {'resolution': 'Transitive same-package Java identifier/type references from tree-sitter; comments and string literals excluded. Verified by isolated javac at t.',
              'additional_developer_jars': [], 'units': units}
    write_json(EXP / 'tools/dev-compile-units.json', output)
    snapshot = RESULTS / 'part3-input-sha256.json'
    if snapshot.exists():
        assert json.loads(snapshot.read_text()) == fixed_inputs()
    else:
        write_json(snapshot, fixed_inputs())
    print(json.dumps(output, indent=2))


if __name__ == '__main__':
    main()
