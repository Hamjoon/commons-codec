#!/usr/bin/env python3
"""Freeze the prescribed range and verify its first-parent and scope properties."""
import argparse,csv,hashlib,json,subprocess
from pathlib import Path
from experiment import BASE,EXPERIMENT,REPO,SCOPE_FILES

def git(*args):return subprocess.check_output(['git','-C',str(REPO),*args],text=True).strip()
if __name__=='__main__':
 parser=argparse.ArgumentParser();parser.add_argument('--kt',type=Path,required=True);args=parser.parse_args()
 source=args.kt/'results/stage0-survey/history/commons-codec-commits.csv';rows=list(csv.DictReader(source.open()));assert len(rows)==830
 head=git('rev-parse','upstream/master');line=git('rev-list','--first-parent',head).splitlines();assert BASE in line
 previous=BASE;scope_count=0;milestones={}
 for i,row in enumerate(rows,1):
  assert int(row['index'])==i
  assert git('rev-parse',row['sha']+'^1')==previous,(i,row['sha'])
  files=set(git('diff-tree','--no-commit-id','--name-only','-r',previous,row['sha']).splitlines());scoped=files&set(SCOPE_FILES)
  row['changes-scope']='yes' if scoped else 'no';row['scope-files']=len(scoped);scope_count+=bool(scoped)
  if scoped and scope_count in [25,50,75,100]:milestones[str(scope_count)]={'index':i,'sha':row['sha'],'date':row['date']}
  previous=row['sha']
 selected=rows[:539];assert selected[-1]['sha'].startswith('f9808d1f')
 with (EXPERIMENT/'results/commits.csv').open('w') as f:
  writer=csv.DictWriter(f,fieldnames=list(selected[0]),lineterminator='\n');writer.writeheader();writer.writerows(selected)
 result={'base':BASE,'upstream_master_observed':head,'source_sha256':hashlib.sha256(source.read_bytes()).hexdigest(),'source_rows':len(rows),'selected_rows':len(selected),'all_first_parent_links_verified':True,'selected_src_changes':sum(r['changes-src-main']=='yes' for r in selected),'selected_scope_changes':sum(r['changes-scope']=='yes' for r in selected),'full_scope_changes':scope_count,'scope_milestones':milestones,'last_selected':selected[-1],'scope_files':SCOPE_FILES}
 result['release_tags']=[{'index':int(row['index']),'sha':row['sha'],'date':row['date'],'tag':tag} for row in selected for tag in row['tags'].split(';') if tag.startswith('rel/')]
 assert result['selected_scope_changes']==50
 assert any(r['tag']=='rel/commons-codec-1.17.1' for r in result['release_tags'])
 (EXPERIMENT/'results/commits-check.json').write_text(json.dumps(result,indent=2)+'\n')
 command=f'python3 experiments/ktester-commit-survival/tools/check_commits.py --kt {args.kt}'
 lines=['# Fixed commit range','','Every number below is produced by `'+command+'` from the Stage 0 CSV and this repository’s git objects. Input digest and observed upstream/master SHA are preserved in commits-check.json. The script verifies BASE occurs in `git rev-list --first-parent upstream/master`, and for every row checks `git rev-parse <sha>^1` against the preceding SHA (starting at BASE). Scope uses `git diff-tree --no-commit-id --name-only -r <parent> <sha>`.','',f"Verified {len(selected)} selected links; additionally checked all {len(rows)} source-row links. The selected range contains {result['selected_src_changes']} src/main-changing commits and {result['selected_scope_changes']} scope-changing commits. Its last SHA is {selected[-1]['sha']} ({selected[-1]['date']}). The full source list contains {scope_count} scope-changing commits.",'','| Scope-changing milestone | First-parent index | Date | SHA |','|---:|---:|---|---|']
 for n in [25,50,75,100]:
  m=milestones.get(str(n));lines.append(f"| {n} | {m['index'] if m else 'not reached'} | {m['date'] if m else ''} | {m['sha'] if m else ''} |")
 lines+=['','Release tags pointing directly onto the selected first-parent line (same verification invocation):','']
 for r in result['release_tags']:lines.append(f"- {r['tag']}: index {r['index']}, {r['sha']}, {r['date']}.")
 lines+=['','The range was extended by the Part 1 addendum to the selected endpoint. These scope-change counts are facts for later review, not an instruction to extend it.']
 (EXPERIMENT/'results/commits-check.md').write_text('\n'.join(lines)+'\n')
 (EXPERIMENT/'README.md').write_text('# KTester commit-survival experiment\n\nThis experiment compares KTester-generated tests with developer tests frozen at `'+BASE+'` on Apache Commons Codec, following the prescribed first-parent commits on master and restricting reported survival and changed-line coverage to Base32, Base64, Md5Crypt, MurmurHash3 and Sha2Crypt. The fixed range is the first '+str(len(selected))+' rows after the anchor, ending at `'+selected[-1]['sha']+'`; its verified list and measurements are in `results/`, frozen developer sources and the method denominator in `dev/`, the focal population in `population/`, future generated tests in `generated/`, and reproducible commands in `tools/`. Part 1 makes no model calls. Range counts are produced by `'+command+'` and recorded in `results/commits-check.json`.\n')
 print(json.dumps(result,indent=2))
