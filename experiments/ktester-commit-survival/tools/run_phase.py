"""Up to eight independent worker exports; resume from complete per-group JSON."""
import argparse
import datetime
import json
import multiprocessing
import time
import traceback
from run_commit import run_group, raw_path
from survival_common import EXP, RESULTS, check_inputs, read_csv, write_json


def worker(number, indices, phase, stop):
    try:
        alive = json.loads((RESULTS / 'alive-entering.json').read_text()) if phase == 'b' else None
        for index in indices:
            if stop.is_set():
                break
            for group in ['dev', 'llm']:
                selected = alive[str(index)][group] if alive is not None else None
                skipped = raw_path(index, group, phase).exists()
                result = run_group(index, group, f'w{number}', phase, selected, phase == 'b')
                print(json.dumps({'worker': number, 'phase': phase, 'index': index, 'group': group,
                                  'status': result['status'], 'seconds': result['seconds'], 'skipped_existing': skipped}), flush=True)
            failures = [p for p in (RESULTS / 'builds' / phase).glob('*.json') if json.loads(p.read_text())['status'] == 'build-fail']
            if len(failures) > 539 * 0.10:
                stop.set()
                raise RuntimeError('More than 10% of 539 commits failed to build')
    except BaseException:
        stop.set()
        log = EXP / f'work/phase-{phase}/worker-{number}-error.txt'
        log.parent.mkdir(parents=True, exist_ok=True)
        log.write_text(traceback.format_exc())
        raise


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--phase', choices=['a', 'b'], required=True)
    parser.add_argument('--workers', type=int, choices=range(1, 9), default=4)
    parser.add_argument('--start', type=int, default=1)
    parser.add_argument('--end', type=int, default=539)
    args = parser.parse_args()
    fingerprint = check_inputs()
    assert json.loads((RESULTS / 'part3-runner-check.json').read_text())['passed'], 'Step 0 must pass first'
    indices = list(range(args.start, args.end + 1)) if args.phase == 'a' else [int(r['index']) for r in read_csv(RESULTS / 'coverage-commits.csv') if args.start <= int(r['index']) <= args.end]
    assert indices and min(indices) >= 0 and max(indices) <= 539
    layout = {str(i + 1): indices[i::args.workers] for i in range(args.workers)}
    start = time.monotonic()
    timestamp = datetime.datetime.now(datetime.timezone.utc).isoformat()
    write_json(RESULTS / f'phase-{args.phase}-running.json', {'phase': args.phase, 'start': timestamp,
               'workers': args.workers, 'worker_layout': layout, 'indices': indices, 'input_sha256': fingerprint})
    stop = multiprocessing.Event()
    processes = [multiprocessing.Process(target=worker, args=(i + 1, layout[str(i + 1)], args.phase, stop)) for i in range(args.workers)]
    for process in processes:
        process.start()
    for process in processes:
        process.join()
    result = {'phase': args.phase, 'start': timestamp, 'end': datetime.datetime.now(datetime.timezone.utc).isoformat(),
              'seconds': round(time.monotonic() - start, 3), 'worker_layout': layout, 'input_sha256': fingerprint,
              'worker_exit_codes': [p.exitcode for p in processes], 'completed': not stop.is_set() and all(p.exitcode == 0 for p in processes),
              'indices': indices, 'resume_rule': 'Skip only existing complete atomic JSON matching commit, frozen inputs and method selection'}
    history = RESULTS / f'phase-{args.phase}-invocations'
    history.mkdir(exist_ok=True)
    write_json(history / f'{time.time_ns()}.json', result)
    write_json(RESULTS / f'phase-{args.phase}.json', result)
    print(json.dumps({k: v for k, v in result.items() if k not in {'worker_layout', 'indices'}}, indent=2))
    if not result['completed']:
        raise SystemExit('Phase interrupted; inspect worker logs before resuming')


if __name__ == '__main__':
    main()
