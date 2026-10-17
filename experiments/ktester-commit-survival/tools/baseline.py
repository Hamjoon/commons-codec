#!/usr/bin/env python3
"""Build a clean t export, freeze sources, and measure method-level developer baseline."""
import collections,csv,hashlib,io,json,pathlib,re,shutil,subprocess,tarfile,time,xml.etree.ElementTree as ET
from experiment import BASE,EXPERIMENT,REPO,SCOPE
WORK=EXPERIMENT/'work';DEV=EXPERIMENT/'dev';RESULTS=EXPERIMENT/'results';TARGET=WORK/'t'
SKIPS=['-Drat.skip','-Dcheckstyle.skip','-Dspotbugs.skip','-Dpmd.skip','-Dmaven.javadoc.skip']
def run(command,label):
 start=time.monotonic();log=RESULTS/(label+'.txt')
 with log.open('w') as f:cp=subprocess.run(command,cwd=TARGET,stdout=f,stderr=subprocess.STDOUT,timeout=1800)
 result={'command':command,'cwd':str(TARGET),'exit_code':cp.returncode,'seconds':round(time.monotonic()-start,3),'log':str(log.relative_to(EXPERIMENT))}
 if cp.returncode:raise RuntimeError('Command failed; inspect '+str(log)+'; no automatic repetition.')
 return result

def method_name(name):
 # Actual Surefire names retain the Java method prefix before parameter type/display suffixes.
 m=re.match(r'^([A-Za-z_$][\w$]*)(?:\(|\[|$)',name)
 if not m:raise ValueError('Cannot infer a method from Surefire display name: '+repr(name))
 return m.group(1)

def inventory():
 production=collections.defaultdict(set)
 for f in (TARGET/'src/main/java').rglob('*.java'):
  code=f.read_text();pkg=re.search(r'\bpackage\s+([\w.]+)\s*;',code)
  if pkg:production[pkg.group(1)].add(f.stem)
 return production

def in_scope(cls,production):
 pkg,_,simple=cls.rpartition('.')
 for scope in SCOPE:
  scope_pkg,_,name=scope.rpartition('.')
  if pkg!=scope_pkg or not simple.endswith('Test') or not simple.startswith(name):continue
  prefix=simple[:-4]
  if prefix==name or prefix not in production[pkg]:return True
 return False

def parse_reports(folder):
 methods=collections.defaultdict(list);raw=[];totals=collections.Counter();files=sorted(folder.glob('TEST-*.xml'))
 for f in files:
  suite=ET.parse(f).getroot()
  for key in ['tests','failures','errors','skipped']:totals[key]+=int(suite.get(key,0))
  for t in suite.iter('testcase'):
   cls=t.get('classname');name=t.get('name');method=method_name(name)
   state='error' if t.find('error') is not None else 'failure' if t.find('failure') is not None else 'skipped' if t.find('skipped') is not None else 'pass'
   methods[(cls,method)].append(state);raw.append({'class':cls,'raw-name':name,'method':method,'status':state})
 assert len(raw)==totals['tests']
 return methods,raw,dict(totals),len(files)

if __name__=='__main__':
 if (TARGET/'target').exists():raise SystemExit('Refusing to reuse a previously built export; preserve existing run evidence.')
 TARGET.mkdir(parents=True,exist_ok=True)
 arc=subprocess.check_output(['git','-C',str(REPO),'archive',BASE]);tarfile.open(fileobj=io.BytesIO(arc)).extractall(TARGET,filter='data')
 for name in ['java','resources']:
  source=TARGET/'src/test'/name
  if source.exists():shutil.copytree(source,DEV/'src-test-t'/name,dirs_exist_ok=True)
 build=run(['mvn','-q','-DskipTests',*SKIPS,'package'],'baseline-package')
 runs=[];observations=[];raws=[]
 for index in range(1,4):
  result=run(['mvn','-q',*SKIPS,'test'],f'baseline-run{index}')
  src=TARGET/'target/surefire-reports';dest=WORK/f'surefire-run{index}';shutil.copytree(src,dest)
  methods,raw,totals,files=parse_reports(dest);observations.append(methods);raws.append(raw);result.update(totals=totals,xml_files=files,method_count=len(methods));runs.append(result)
 production=inventory();keys=sorted(set().union(*(set(x) for x in observations)));rows=[]
 for cls,name in keys:
  statuses=[];counts=[]
  for obs in observations:
   cases=obs.get((cls,name),[]);counts.append(len(cases))
   statuses.append('missing' if not cases else 'pass' if all(s=='pass' for s in cases) else '+'.join(sorted(set(cases)-{'pass'})))
  rows.append({'class':cls,'method':name,'in-scope':'yes' if in_scope(cls,production) else 'no',**{f'run{i}':status for i,status in enumerate(statuses,1)},'cases':str(counts[0]) if len(set(counts))==1 else '/'.join(map(str,counts))})
 passed=lambda r:all(r['run'+str(i)]=='pass' for i in range(1,4))
 scoped=[r for r in rows if r['in-scope']=='yes'];denom=[r for r in scoped if passed(r)]
 for path,values in [('dev-methods-all.csv',rows),('dev-denominator.csv',denom)]:
  with (DEV/path).open('w',newline='') as f:
   w=csv.DictWriter(f,fieldnames=list(rows[0]),lineterminator='\n');w.writeheader();w.writerows(values)
 with (DEV/'surefire-method-map.csv').open('w',newline='') as f:
  w=csv.DictWriter(f,fieldnames=['run','class','raw-name','method','status'],lineterminator='\n');w.writeheader()
  for i,raw in enumerate(raws,1):
   for row in raw:w.writerow({'run':i,**row})
 per_class={cls:{'methods':sum(r['class']==cls for r in scoped),'pass_all':sum(r['class']==cls for r in denom)} for cls in sorted({r['class'] for r in scoped})}
 excluded={}
 for f in (TARGET/'src/test/java').rglob('*.java'):
  code=f.read_text();pkg=re.search(r'\bpackage\s+([\w.]+)\s*;',code)
  if not pkg:continue
  cls=pkg.group(1)+'.'+f.stem
  if any(cls.rpartition('.')[0]==s.rpartition('.')[0] and f.stem.startswith(s.rpartition('.')[2]) for s in SCOPE) and not in_scope(cls,production):
   excluded[cls]={'methods':sum(r['class']==cls for r in rows),'reason':'prefix before Test names a production class' if f.stem.endswith('Test') else 'helper/non-Test class'}
 bad=[r for r in rows if not passed(r)];result={'base':BASE,'jdk':subprocess.check_output(['java','-version'],stderr=subprocess.STDOUT,text=True),'maven':subprocess.check_output(['mvn','-version'],stderr=subprocess.STDOUT,text=True),'build':build,'runs':runs,'whole_methods':len(rows),'whole_pass_all':sum(passed(r) for r in rows),'scope_methods':len(scoped),'developer_denominator':len(denom),'scope_classes':per_class,'excluded_prefix_classes':excluded,'not_passing_all':bad,'case_normalization':'Leading Java identifier; strip first (parameter types) or [case suffix]. Unrecognized names cause a hard failure rather than guessed mappings.'}
 (RESULTS/'developer-baseline.json').write_text(json.dumps(result,indent=2)+'\n')
 frozen={str(f.relative_to(DEV/'src-test-t')):hashlib.sha256(f.read_bytes()).hexdigest() for f in sorted((DEV/'src-test-t').rglob('*')) if f.is_file()};(DEV/'frozen-source-sha256.json').write_text(json.dumps(frozen,indent=2)+'\n')
 lines=['# Frozen developer baseline','','All counts and timings: from KTester/docker, `docker compose run --rm -v /Users/donggi/_projects/experiment-projects/commons-codec:/repo ktester bash -c \'python /repo/experiments/ktester-commit-survival/tools/baseline.py\'`. The shared compose ktester-m2 volume is used by every Maven invocation. Exact Maven argument arrays, JDK/Maven versions and timings are in ../results/developer-baseline.json. No offline mode was used.','',f"The clean upstream export is at `{BASE}`. Package wall time: {build['seconds']} seconds. Whole suite: {len(rows)} (class, method) units, {result['whole_pass_all']} pass all runs (reference only). In scope: {len(scoped)} methods; **developer denominator: {len(denom)}** pass all runs.",'','| Run | Cases reported | Executed (not skipped) | Failures | Errors | Skipped | Wall seconds |','|---:|---:|---:|---:|---:|---:|---:|']
 for i,r in enumerate(runs,1):
  c=r['totals'];lines.append(f"| {i} | {c['tests']} | {c['tests']-c['skipped']} | {c['failures']} | {c['errors']} | {c['skipped']} | {r['seconds']} |")
 lines+=['','## Method normalization and status','','Surefire actually writes names such as `testCustomCharset(String)[1]`; the Java identifier before the first parenthesis or bracket is the method. Plain identifiers remain unchanged. No display-only name beginning with [case] was observed; such a name would stop this script rather than be guessed. Every testcase/raw-name mapping is recorded in surefire-method-map.csv. All testcases of a (class, method) pair must pass; any skipped/failing/error case rejects that method for the run. Missing methods reject it too. Parameterized methods count once. The cases column is the per-run case count (or slash-separated run counts if they differ).','','The whole-suite unit includes the concrete execution class, so inherited tests executed in different classes are distinct units; source-annotation grep estimates are not the execution denominator. The scoped count is within the specified tolerance.','', '## Scope classes','','The package must equal a scope package and the test name must be ClassTest or ClassSuffixTest. ClassSuffix is excluded when it is another production class at t. Production names are the Java source file names in that package, which include all candidate stream classes. Helpers without a Test suffix do not enter the denominator.','','| Class | Methods | Pass all runs |','|---|---:|---:|']
 for cls,c in per_class.items():lines.append(f"| {cls} | {c['methods']} | {c['pass_all']} |")
 lines+=['','Excluded scope-prefix classes (same invocation):','']
 for cls,r in excluded.items():lines.append(f"- {cls}: {r['methods']} executed method units; {r['reason']}.")
 lines+=['','Methods not passing every run (same invocation):','']
 for r in bad:lines.append(f"- {r['class']}#{r['method']}: "+', '.join(f"run{i}={r['run'+str(i)]}" for i in range(1,4))+f"; in-scope={r['in-scope']}.")
 lines+=['','Frozen Java sources and test resources are in src-test-t/, with original relative paths beneath java/ and resources/. Their content digests are in frozen-source-sha256.json. Raw Surefire reports remain locally in ignored work/surefire-run*/. The tracked testcase-to-method map preserves the method-level evidence.']
 (DEV/'README.md').write_text('\n'.join(lines)+'\n')
 print(json.dumps({k:result[k] for k in ['whole_methods','whole_pass_all','scope_methods','developer_denominator','scope_classes','not_passing_all']},indent=2))
 if abs(len(denom)-144)>20:raise SystemExit('STOP: denominator outside expected tolerance; explain before continuing.')
