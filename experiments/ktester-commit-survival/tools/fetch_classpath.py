#!/usr/bin/env python3
"""Resolve exact supplied coordinates from Central, with at most two attempts each."""
import hashlib,json,pathlib,shutil,subprocess,time
from experiment import EXPERIMENT,preserve_log
if __name__=='__main__':
 work=EXPERIMENT/'work';libs=work/'libs';libs.mkdir(parents=True,exist_ok=True);resolver=work/'maven-resolver';resolver.mkdir(exist_ok=True)
 # Isolate dependency resolution from upstream build plugins/repositories, while retaining shared cache.
 (resolver/'pom.xml').write_text('<project xmlns="http://maven.apache.org/POM/4.0.0"><modelVersion>4.0.0</modelVersion><groupId>experiment</groupId><artifactId>classpath-resolver</artifactId><version>1</version></project>\n')
 results=[];entries=json.loads((EXPERIMENT/'tools/classpath-artifacts.json').read_text())
 for e in entries:
  g,a,v=e['coordinate'].split(':');artifact=e['coordinate']+':jar'+(':'+e['classifier'] if e['classifier'] else '')
  cmd=['mvn','-q','-U','org.apache.maven.plugins:maven-dependency-plugin:3.8.1:get','-Dartifact='+artifact,'-Dtransitive=false','-DremoteRepositories=central::default::https://repo.maven.apache.org/maven2']
  attempts=[]
  for attempt in range(1,3):
   logfile=EXPERIMENT/'results'/f'classpath-{e["filename"]}-attempt{attempt}.txt';start=time.monotonic()
   with logfile.open('w') as log:
    try:cp=subprocess.run(cmd,cwd=resolver,stdout=log,stderr=subprocess.STDOUT,timeout=300);rc=cp.returncode
    except subprocess.TimeoutExpired:rc=124;log.write('\nDependency resolution timeout.\n')
   preserve_log(logfile)
   attempts.append({'command':cmd,'exit_code':rc,'seconds':round(time.monotonic()-start,3),'log':str(logfile.relative_to(EXPERIMENT))})
   if rc==0:break
  item={'filename':e['filename'],'coordinate':e['coordinate'],'classifier':e['classifier'],'role':e['role'],'attempts':attempts,'resolved':rc==0}
  if rc==0:
   filename=a+'-'+v+('-'+e['classifier'] if e['classifier'] else '')+'.jar';cached=pathlib.Path('/root/.m2/repository')/g.replace('.','/')/a/v/filename
   if not cached.is_file():item.update(resolved=False,error='Maven returned success but expected jar is missing: '+str(cached))
   else:
    target=libs/e['filename'];shutil.copy2(cached,target);digest=hashlib.sha256(target.read_bytes()).hexdigest();item.update(bytes=target.stat().st_size,sha256=digest,identical_to_supplied=digest==e['supplied_sha256'] if 'supplied_sha256' in e else None)
  results.append(item);print(e['filename'],'resolved' if item['resolved'] else 'FAILED after bounded attempts',flush=True)
 result={'entries':results,'requested':len(entries),'resolved':sum(r['resolved'] for r in results),'all_present':all(r['resolved'] for r in results),'shared_maven_repository':'/root/.m2/repository','central':'https://repo.maven.apache.org/maven2'}
 (EXPERIMENT/'results/classpath-fetch.json').write_text(json.dumps(result,indent=2)+'\n')
 if not result['all_present']:raise SystemExit('STOP at end of Step 3: approved artifact coordinates could not all be resolved; do not substitute versions.')
