#!/usr/bin/env python3
"""Independently check resolved runtime jars; never treat this as a complete fetch."""
import hashlib,json,pathlib,shlex,shutil,subprocess,time
from experiment import EXPERIMENT,preserve_log
if __name__=='__main__':
 inventory=json.loads((EXPERIMENT/'tools/classpath-artifacts.json').read_text());fetched=json.loads((EXPERIMENT/'results/classpath-fetch.json').read_text());resolved={e['filename'] for e in fetched['entries'] if e['resolved']}
 runtime=sorted([e for e in inventory if e['role']=='test'],key=lambda e:e['filename'].startswith('junit-platform-console-standalone'))
 missing=[e['filename'] for e in runtime if e['filename'] not in resolved]
 if missing:raise SystemExit('Runtime jars unavailable: '+str(missing))
 source=pathlib.Path('/work/results/stage4c/commons-codec/test_classes/Md5Crypt_md5Crypt_Test.java');dest=EXPERIMENT/'work/smoke';(dest/'classes').mkdir(parents=True,exist_ok=True);shutil.copy2(source,dest/source.name)
 compile_cp='work/t/target/classes:'+':'.join('work/libs/'+e['filename'] for e in runtime)
 compile_cmd=['javac','-cp',compile_cp,'-d','work/smoke/classes','work/smoke/'+source.name]
 # Mirror KTester precedence: project libs first, bundled ConsoleLauncher last.
 cp='work/t/target/classes:work/smoke/classes:'+':'.join('work/libs/'+e['filename'] for e in runtime)
 run_cmd=['java','-cp',cp,'org.junit.platform.console.ConsoleLauncher','--disable-banner','--disable-ansi-colors','--fail-if-no-tests','--select-class','org.apache.commons.codec.digest.Md5Crypt_md5Crypt_Test','--details','summary','--reports-dir','work/smoke/reports']
 results=[]
 for label,cmd in [('compile',compile_cmd),('launch',run_cmd)]:
  log=EXPERIMENT/'results'/('classpath-smoke-'+label+'.txt');start=time.monotonic()
  with log.open('w') as f:r=subprocess.run(cmd,cwd=EXPERIMENT,stdout=f,stderr=subprocess.STDOUT,timeout=120)
  preserve_log(log)
  results.append({'stage':label,'command':cmd,'shell_command':shlex.join(cmd),'exit_code':r.returncode,'seconds':round(time.monotonic()-start,3),'log':str(log.relative_to(EXPERIMENT))})
  if label=='compile' and r.returncode:break
 evidence={'author_source':str(source),'source_sha256':hashlib.sha256(source.read_bytes()).hexdigest(),'cwd':'/repo/experiments/ktester-commit-survival','runtime_files':[e['filename'] for e in runtime],'runs':results,'complete_classpath_fetch':fetched['all_present']}
 if len(results)==2:
  import xml.etree.ElementTree as ET
  reports=list((dest/'reports').glob('TEST-*.xml'));counts={k:0 for k in ['tests','failures','errors','skipped']}
  for f in reports:
   root=ET.parse(f).getroot()
   for k in counts:counts[k]+=int(root.get(k,0))
  evidence['counts']=counts;evidence['launcher_executed_tests']=counts['tests']>0
 (EXPERIMENT/'results/classpath-smoke.json').write_text(json.dumps(evidence,indent=2)+'\n');print(json.dumps(evidence,indent=2))
