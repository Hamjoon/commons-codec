#!/usr/bin/env python3
"""Check released JaCoCo agent and XML lines, keeping coverage jars off test classpaths."""
import json,shlex,subprocess,time,xml.etree.ElementTree as ET
from experiment import EXPERIMENT,preserve_log
if __name__=='__main__':
 artifacts=json.loads((EXPERIMENT/'tools/classpath-artifacts.json').read_text());fetch=json.loads((EXPERIMENT/'results/classpath-fetch.json').read_text());assert fetch['all_present'] and fetch['resolved']==20
 old=json.loads((EXPERIMENT/'results/classpath-smoke.json').read_text());runtime=sorted([e for e in artifacts if e['role']=='test'],key=lambda e:e['filename'].startswith('junit-platform-console-standalone'))
 compile_cmd=old['runs'][0]['command'].copy();compile_cmd[compile_cmd.index('-cp')+1]='work/t/target/classes:'+':'.join('work/libs/'+e['filename'] for e in runtime)
 java_cmd=old['runs'][1]['command'].copy();assert not any('jacoco' in p for p in java_cmd[java_cmd.index('-cp')+1].split(':'))
 java_cmd.insert(1,'-javaagent:work/libs/org.jacoco.agent-0.8.12-runtime.jar=destfile=work/smoke/jacoco.exec,includes=org.apache.commons.codec.*')
 cli_cmd=['java','-jar','work/libs/org.jacoco.cli-0.8.12-nodeps.jar','report','work/smoke/jacoco.exec','--classfiles','work/t/target/classes','--sourcefiles','work/t/src/main/java','--xml','work/smoke/jacoco.xml']
 dest=EXPERIMENT/'work/smoke/jacoco.exec'
 if dest.exists():raise SystemExit('Refusing to append to a pre-existing smoke exec file.')
 runs=[]
 for label,cmd in [('compile',compile_cmd),('agent',java_cmd),('xml',cli_cmd)]:
  log=EXPERIMENT/'results'/f'coverage-smoke-{label}.txt';start=time.monotonic()
  with log.open('w') as f:cp=subprocess.run(cmd,cwd=EXPERIMENT,stdout=f,stderr=subprocess.STDOUT,timeout=120)
  preserve_log(log);runs.append({'stage':label,'command':cmd,'shell_command':shlex.join(cmd),'exit_code':cp.returncode,'seconds':round(time.monotonic()-start,3),'log':str(log.relative_to(EXPERIMENT))})
  if label!='agent' and cp.returncode:raise SystemExit(f'{label} failed; see {log}')
 root=ET.parse(EXPERIMENT/'work/smoke/jacoco.xml').getroot();source=root.find("./package[@name='org/apache/commons/codec/digest']/sourcefile[@name='Md5Crypt.java']");assert source is not None
 lines=source.findall('line');covered=[int(x.get('nr')) for x in lines if int(x.get('ci','0'))>0];counters={x.get('type'):{'missed':int(x.get('missed')),'covered':int(x.get('covered'))} for x in source.findall('counter')};assert lines and covered and counters['LINE']['covered']>0
 result={'runs':runs,'sourcefile':'org/apache/commons/codec/digest/Md5Crypt.java','line_entries':len(lines),'covered_line_numbers':covered,'counters':counters,'coverage_jars_excluded_from_test_classpaths':all('jacoco' not in cmd[cmd.index('-cp')+1] for cmd in [compile_cmd,java_cmd]),'xml_path':'work/smoke/jacoco.xml','exec_bytes':dest.stat().st_size}
 (EXPERIMENT/'results/coverage-smoke.json').write_text(json.dumps(result,indent=2)+'\n');print(json.dumps(result,indent=2))
