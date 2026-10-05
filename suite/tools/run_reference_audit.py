#!/usr/bin/env python3
"""Build the exact fork comparator, then audit saved outputs without executing SPARQL."""
from __future__ import annotations
import json,os,re,subprocess,sys
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
REPORTS=ROOT/'reports'

def main():
    REPORTS.mkdir(exist_ok=True)
    outcome={'completed':False,'queryEvaluationPerformed':False,'originalJunitOutcomesModified':False}
    try:
        expected=json.loads((ROOT/'sources.json').read_text())['jena']
        inventory=json.loads((ROOT/'corpus/jena-source-inventory.json').read_text())
        # Source hashes have already been verified during import; retain the exact origin.
        outcome['source']=expected
        overrides=[]
        for file in (ROOT/'vendor/jena').rglob('*.java'):
            if '.git' in file.parts or 'target' in file.parts:continue
            text=file.read_text(errors='replace')
            if 'compareResultSetsByValue' in text:
                overrides.append({'path':str(file.relative_to(ROOT/'vendor/jena')),'lines':[{'line':i,'text':line.strip()} for i,line in enumerate(text.splitlines(),1) if 'compareResultSetsByValue' in line]})
        (REPORTS/'upstream-comparison-policy-sites.json').write_text(json.dumps(overrides,indent=2)+'\n')
        commands=[
            ['mvn','-B','-ntp','-f',str(ROOT/'vendor/jena/pom.xml'),'-pl','jena-arq','-am','-DskipTests','-Dmaven.javadoc.skip=true','install'],
            ['mvn','-B','-ntp','-f',str(ROOT/'reference-oracle/pom.xml'),'package','org.apache.maven.plugins:maven-dependency-plugin:3.8.1:build-classpath','-Dmdep.outputFile='+str(ROOT/'reference-oracle/target/classpath.txt')]
        ]
        with (REPORTS/'reference-oracle-build.log').open('w') as log:
            for command in commands:
                log.write('COMMAND '+repr(command)+'\n');log.flush()
                subprocess.run(command,stdout=log,stderr=subprocess.STDOUT,check=True,timeout=600)
        classpath=str(ROOT/'reference-oracle/target/classes')+os.pathsep+(ROOT/'reference-oracle/target/classpath.txt').read_text().strip()
        with (REPORTS/'reference-oracle-run.log').open('w') as log:
            subprocess.run(['java','-Xmx2g','-cp',classpath,'org.hasmac.reference.SourceOracleAudit',str(ROOT)],stdout=log,stderr=subprocess.STDOUT,check=True,timeout=180)
        outcome['completed']=True
    except (OSError,subprocess.SubprocessError,ValueError) as e:outcome['error']=str(e)
    (REPORTS/'reference-audit-execution.json').write_text(json.dumps(outcome,indent=2)+'\n')
    text=(REPORTS/'reference-oracle-build.log').read_text(errors='replace') if (REPORTS/'reference-oracle-build.log').exists() else ''
    (REPORTS/'reference-audit-tail.txt').write_text('\n'.join(text.splitlines()[-50:])[-12000:]+'\n')
    print(json.dumps(outcome));return 0 if outcome['completed'] else 1

if __name__=='__main__':sys.exit(main())
