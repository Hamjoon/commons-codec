"""Compute the exact coverage commit set and pre/post-image changed line ranges."""
import collections
import re
from survival_common import RESULTS, SCOPE_FILES, commits, git, write_csv, write_json
from coverage_common import file_group


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


def changed_files(name_status, pre, post):
    """Use NUL-delimited Git status to retain renames, deletions and empty changes."""
    if not name_status:
        assert not pre and not post
        return []
    tokens = iter(name_status.rstrip('\0').split('\0'))
    files = []
    for status in tokens:
        first = next(tokens).removeprefix('src/main/java/')
        if status.startswith(('R', 'C')):
            old, new = first, next(tokens).removeprefix('src/main/java/')
        else:
            old, new = (None if status == 'A' else first), (None if status == 'D' else first)
        files.append({'status': status, 'old_file': old, 'new_file': new,
                      'renamed': status.startswith('R'), 'deleted': status == 'D',
                      'group': file_group(new or old),
                      'pre_image_group': file_group(old) if old else None,
                      'post_image_group': file_group(new) if new else None,
                      'pre_image_lines': pre.get(old, []), 'post_image_lines': post.get(new, [])})
    assert set(pre) <= {f['old_file'] for f in files}
    assert set(post) <= {f['new_file'] for f in files}
    return files


def main():
    cm = commits()
    changed = [i for i, r in cm.items() if r.get('changes-src-main') == 'yes']
    reasons = collections.defaultdict(set)
    for index in changed:
        reasons[index].add('changed')
        reasons[index - 1].add('before-change')
        sha = cm[index]['sha']
        parent = git('rev-parse', sha + '^1').strip()
        assert parent == cm[index - 1]['sha']
        command = ['diff', '--no-ext-diff', '--find-renames', '-U0', parent, sha, '--', 'src/main/java']
        diff = git(*command)
        pre, post = changed_lines(diff)
        status = git('diff', '--no-ext-diff', '--find-renames', '--name-status', '-z', parent, sha, '--', 'src/main/java')
        files = changed_files(status, pre, post)
        # Independent check that the scope projection retains the original measurement.
        scope_pre, scope_post = changed_lines(git('diff', '--no-ext-diff', '--no-renames', '-U0', parent, sha, '--', *SCOPE_FILES))
        projection_matches = all({k: v for k, v in image.items() if file_group(k) == 'scope'} == original
                                 for image, original in [(pre, scope_pre), (post, scope_post)])
        assert projection_matches, f'Scope diff projection differs at {index}'
        write_json(RESULTS / 'changes' / f'{index}.json', {'index': index, 'sha': sha, 'parent': parent,
                   'subject': git('show', '-s', '--format=%s', sha).strip(), 'pre_image': pre, 'post_image': post,
                   'files': files, 'scope_projection_matches_original_diff': projection_matches,
                   'command': ['git', *command], 'diff': diff})
    rows = [{'index': i, 'sha': cm[i]['sha'], 'reason': 'both' if len(reasons[i]) == 2 else next(iter(reasons[i]))} for i in sorted(reasons)]
    assert 0 in reasons, 'Index 0 is required for the requested baseline table'
    write_csv(RESULTS / 'coverage-commits.csv', ['index', 'sha', 'reason'], rows)
    print({'coverage_commits': len(rows), 'production_changing_commits': len(changed),
           'scope_changing_commits': sum(r.get('changes-scope') == 'yes' for r in cm.values())})


if __name__ == '__main__':
    main()
