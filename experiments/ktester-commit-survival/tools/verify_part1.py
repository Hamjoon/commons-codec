#!/usr/bin/env python3
"""Verify preserved evidence without rebuilding or rerunning developer tests."""
import argparse,ast,csv,hashlib,json,subprocess
from pathlib import Path
from experiment import BASE,EXPERIMENT,REPO
from baseline import parse_reports,in_scope,inventory
if __name__=='__main__':
 ap=argparse.ArgumentParser();ap.add_argument('--kt',required=True,type=Path);args=ap.parse_args()
 for path in (EXPERIMENT/'tools').glob('*.py'):ast.parse(path.read_text(),filename=str(path))
 frozen=json.loads((EXPERIMENT/'dev/frozen-source-sha256.json').read_text())
 for rel,digest in frozen.items():
  raw=(EXPERIMENT/'dev/src-test-t'/rel).read_bytes();assert hashlib.sha256(raw).hexdigest()==digest
  assert subprocess.check_output(['git','-C',str(REPO),'show',BASE+':src/test/'+rel])==raw,rel
 rows=list(csv.DictReader((EXPERIMENT/'dev/dev-methods-all.csv').open()));denom=list(csv.DictReader((EXPERIMENT/'dev/dev-denominator.csv').open()));assert len({(r['class'],r['method']) for r in rows})==len(rows)
 assert denom==[r for r in rows if r['in-scope']=='yes' and all(r['run'+str(i)]=='pass' for i in range(1,4))]
 prod=inventory()
 for r in rows:assert (r['in-scope']=='yes')==in_scope(r['class'],prod)
 for i in range(1,4):
  obs,raw,totals,files=parse_reports(EXPERIMENT/'work'/f'surefire-run{i}')
  for r in rows:
   cases=obs[(r['class'],r['method'])];status='pass' if all(c=='pass' for c in cases) else '+'.join(sorted(set(cases)-{'pass'}));assert status==r['run'+str(i)]
   assert len(cases)==int(r['cases'])
 fetched=json.loads((EXPERIMENT/'results/classpath-fetch.json').read_text());items=json.loads((EXPERIMENT/'tools/jar-inventory.json').read_text());original={x['filename']:x for x in items}
 for r in fetched['entries']:
  if r['resolved']:
   data=(EXPERIMENT/'work/libs'/r['filename']).read_bytes();assert hashlib.sha256(data).hexdigest()==r['sha256']==original[r['filename']]['sha256']
  else:assert len(r['attempts'])==2 and all(a['exit_code']!=0 for a in r['attempts'])
 protected=subprocess.check_output(['git','-C',str(args.kt),'diff','89a1514','--','code','data/projects','data/project_index'],text=True);assert not protected
 forbidden=[p for p in subprocess.check_output(['git','-C',str(REPO),'ls-files','--cached'],text=True).splitlines() if p.startswith('experiments/ktester-commit-survival/') and (p.endswith('.jar') or '/work/' in p or '/docs/handover-' in p)]
 assert not forbidden,forbidden
 evidence={'invocation':'python3 experiments/ktester-commit-survival/tools/verify_part1.py --kt '+str(args.kt),'frozen_files_matching_upstream':len(frozen),'method_rows_rechecked':len(rows),'denominator_rows_rechecked':len(denom),'saved_surefire_runs_rechecked':3,'resolved_jars_matching_supplied':sum(x['resolved'] for x in fetched['entries']),'unresolved_with_exactly_two_failed_attempts':sum(not x['resolved'] for x in fetched['entries']),'protected_kt_tracked_diff_empty':True,'tracked_forbidden_paths':forbidden,'python_helper_syntax':'passed'}
 (EXPERIMENT/'results/part1-validation.json').write_text(json.dumps(evidence,indent=2)+'\n');print(json.dumps(evidence,indent=2))
