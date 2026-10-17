"""Compute the exact coverage commit set and pre/post-image changed line ranges."""
import collections
import re
from survival_common import RESULTS, SCOPE_FILES, commits, git, write_csv, write_json


def changed_lines(diff):
    pre, post = collections.defaultdict(set), collections.defaultdict(set)
    old, new = None, None
    for line in diff.splitlines():
        if line.startswith('--- '):
            old = line[4:].removeprefix('a/')
        elif line.startswith('+++ '):
            new = line[4:].removeprefix('b/')
        elif line.startswith('@@ '):
            match = re.match(r'@@ -(\d+)(?:,(\d+))? \+(\d+)(?:,(\d+))? @@', line)
            assert match, line
            a, ac, b, bc = match.groups()
            if old != '/dev/null':
                pre[old.removeprefix('src/main/java/')].update(range(int(a), int(a) + int(ac or 1)))
            if new != '/dev/null':
                post[new.removeprefix('src/main/java/')].update(range(int(b), int(b) + int(bc or 1)))
    return {k: sorted(v) for k, v in pre.items()}, {k: sorted(v) for k, v in post.items()}


def main():
    cm = commits()
    changed = [i for i, r in cm.items() if r.get('changes-scope') == 'yes']
    assert len(changed) == 50
    reasons = collections.defaultdict(set)
    for index in changed:
        reasons[index].add('changed')
        reasons[index - 1].add('before-change')
        sha = cm[index]['sha']
        parent = git('rev-parse', sha + '^1').strip()
        assert parent == cm[index - 1]['sha']
        diff = git('diff', '--no-ext-diff', '--no-renames', '-U0', parent, sha, '--', *SCOPE_FILES)
        pre, post = changed_lines(diff)
        write_json(RESULTS / 'changes' / f'{index}.json', {'index': index, 'sha': sha, 'parent': parent,
                   'subject': git('show', '-s', '--format=%s', sha).strip(), 'pre_image': pre, 'post_image': post,
                   'command': ['git', 'diff', '--no-ext-diff', '--no-renames', '-U0', parent, sha, '--', *SCOPE_FILES], 'diff': diff})
    rows = [{'index': i, 'sha': cm[i]['sha'], 'reason': 'both' if len(reasons[i]) == 2 else next(iter(reasons[i]))} for i in sorted(reasons)]
    assert 0 in reasons, 'Index 0 is required for the requested baseline table'
    write_csv(RESULTS / 'coverage-commits.csv', ['index', 'sha', 'reason'], rows)
    print({'coverage_commits': len(rows), 'scope_changing_commits': len(changed)})


if __name__ == '__main__':
    main()
