#!/usr/bin/env python3
"""Verify preserved evidence without rebuilding or rerunning developer tests."""
import argparse,ast,collections,csv,hashlib,json,subprocess
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
 fetched=json.loads((EXPERIMENT/'results/classpath-fetch.json').read_text());items=json.loads((EXPERIMENT/'tools/classpath-artifacts.json').read_text());original={x['filename']:x for x in items}
 for r in fetched['entries']:
  if r['resolved']:
   data=(EXPERIMENT/'work/libs'/r['filename']).read_bytes();assert hashlib.sha256(data).hexdigest()==r['sha256']
   if 'supplied_sha256' in original[r['filename']]:assert r['sha256']==original[r['filename']]['supplied_sha256']
  else:assert len(r['attempts'])==2 and all(a['exit_code']!=0 for a in r['attempts'])
 assert fetched['requested']==20 and fetched['all_present']
 coordinate_lines=[line.strip() for line in (EXPERIMENT/'tools/llm-classpath.txt').read_text().splitlines() if line.strip() and not line.strip().startswith('#')]
 assert coordinate_lines==[x['coordinate'] for x in items]
 coverage=json.loads((EXPERIMENT/'results/coverage-smoke.json').read_text());assert coverage['counters']['LINE']['covered']>0 and coverage['coverage_jars_excluded_from_test_classpaths']
 population=json.loads((EXPERIMENT/'population/population-validation.json').read_text());assert population['status']=='blocked_dataset_prepare_validation' and population['dataset_prepare_run'] and not population['valid']
 for name in ['README.md','candidates.csv','population-enumeration.json','population-validation.json','dataset_meta.json','dataset_info.json']:
  assert (EXPERIMENT/'population'/name).read_bytes()==(args.kt/'data/commit-survival/commons-codec'/name).read_bytes()
 for rel,digest in population['protected_input_sha256'].items():assert hashlib.sha256((args.kt/rel).read_bytes()).hexdigest()==digest,rel
 for name,digest in population['output_sha256'].items():assert hashlib.sha256((EXPERIMENT/'population'/name).read_bytes()).hexdigest()==digest,name
 candidates=list(csv.DictReader((EXPERIMENT/'population/candidates.csv').open()))
 expected={r['class']+'.'+r['method']+'('+r['parameter-types']+')' for r in candidates}
 metadata=json.loads((EXPERIMENT/'population/dataset_meta.json').read_text());assert len(metadata)==1
 assert metadata[0]['project_name']=='commons-codec' and metadata[0]['put_path']=='/root/experiments/puts/commons-codec'
 assert set(metadata[0]['method_name_to_idx'])==expected and len(expected)==66
 dataset=json.loads((EXPERIMENT/'population/dataset_info.json').read_text())['commons-codec'];entries=dataset['focal-methods']
 actual={m['class']+'.'+m['method-name']:m for m in entries};assert set(actual)==expected and len(entries)==len(actual)==66
 assert all(m['focal-method'].strip() and m['class-code'].strip() for m in entries)
 ids=collections.defaultdict(list)
 for m in entries:ids[m['id']].append(m['class']+'.'+m['method-name'])
 collisions={k:v for k,v in ids.items() if len(v)>1}
 assert collisions==population['id_collisions'] and len(ids)==population['unique_id_count']==43
 assert len(collisions)==9 and sum(map(len,collisions.values()))==population['collision_affected_methods']==32
 def normalize(value):
  if isinstance(value,str):return value.replace('\r\n','\n')
  if isinstance(value,dict):return {k:normalize(v) for k,v in value.items()}
  return value
 authors=json.loads((args.kt/'data/dataset_info.json').read_text())['commons-codec'];checked=[]
 for m in authors['focal-methods']:
  sig=m['class']+'.'+m['method-name']
  if sig in actual:assert normalize(m)==normalize(actual[sig]),sig;checked.append(sig)
 assert len(checked)==5 and all(c['equal_up_to_line_endings'] for c in population['original_entry_comparisons'])
 assert (args.kt/'data/commit-survival/commons-codec/commons-codec').resolve()==(args.kt/'data/projects/commons-codec').resolve()
 protected=subprocess.check_output(['git','-C',str(args.kt),'diff','89a1514','--','code','data/projects','data/project_index','data/dataset_info.json'],text=True);assert not protected
 forbidden=[p for p in subprocess.check_output(['git','-C',str(REPO),'ls-files','--cached'],text=True).splitlines() if p.startswith('experiments/ktester-commit-survival/') and (p.endswith('.jar') or '/work/' in p or '/docs/handover-' in p)]
 assert not forbidden,forbidden
 evidence={'invocation':'python3 experiments/ktester-commit-survival/tools/verify_part1.py --kt '+str(args.kt),'frozen_files_matching_upstream':len(frozen),'method_rows_rechecked':len(rows),'denominator_rows_rechecked':len(denom),'saved_surefire_runs_rechecked':3,'resolved_jars_sha256_verified':sum(x['resolved'] for x in fetched['entries']),'original_runtime_jars_matching_supplied':sum(x.get('identical_to_supplied') is True for x in fetched['entries']),'unresolved_with_exactly_two_failed_attempts':sum(not x['resolved'] for x in fetched['entries']),'protected_kt_tracked_diff_empty':True,'tracked_forbidden_paths':forbidden,'python_helper_syntax':'passed'}
 evidence.update({'population_entries_rechecked':len(entries),'population_unique_ids':len(ids),'population_colliding_ids':len(collisions),'population_collision_affected_methods':sum(map(len,collisions.values())),'original_entries_equal_up_to_line_endings':len(checked),'protected_input_hashes_verified':len(population['protected_input_sha256']),'population_valid':False,'mandatory_stop_correctly_recorded':True})
 (EXPERIMENT/'results/part1-validation.json').write_text(json.dumps(evidence,indent=2)+'\n');print(json.dumps(evidence,indent=2))
