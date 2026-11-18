'use strict';
const fs = require('node:fs/promises');
const fsSync = require('node:fs');
const path = require('node:path');
const {spawn} = require('node:child_process');
const {chromium} = require('playwright');
const root = __dirname, base = 'http://127.0.0.1:18762', out = path.join(root,'evidence');
const nap = ms => new Promise(resolve => setTimeout(resolve,ms));
const report = {version:'6.2.0-M2',authentic:true,claims:'Unmodified RDF4J Workbench + Server WARs; native graph layout; Playwright recording',events:[],nativeActions:[]};
function stamp(what,details={}) {const e={timestamp:new Date().toISOString(),what,...details}; report.events.push(e);console.log(JSON.stringify(e));}
let app=null,browser=null;
(async()=>{
  await fs.mkdir(out,{recursive:true});
  const cp=['tomcat-embed-core-11.0.27.jar','tomcat-annotations-api-11.0.27.jar'].map(n=>path.join(root,'tools',n)).join(path.delimiter);
  const log=fsSync.createWriteStream(path.join(out,'tomcat.log'));
  app=spawn('java',['-cp',cp,path.join(root,'RunWorkbench.java')],{cwd:root,stdio:['ignore','pipe','pipe']});
  app.stdout.pipe(log);app.stderr.pipe(log);
  app.on('error',e=>stamp('startup-error',{error:String(e)}));
  let healthy=false;
  for(let i=0;i<150;i++) {
    if(app.exitCode!==null) throw Error('Tomcat exited '+app.exitCode+'; see tomcat.log');
    try {let r=await fetch(base+'/rdf4j-server/repositories',{signal:AbortSignal.timeout(2500)}); if(r.ok){healthy=true;break;}}catch{}
    await nap(400);
  }
  if(!healthy)throw Error('Timed out starting actual RDF4J Server');
  stamp('actual-server-started',{url:base});
  const config='@prefix rep: <http://www.openrdf.org/config/repository#> .\n@prefix sr: <http://www.openrdf.org/config/repository/sail#> .\n@prefix sail: <http://www.openrdf.org/config/sail#> .\n@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> .\n[] a rep:Repository; rep:repositoryID "knowledge-graph"; rdfs:label "Connected Knowledge"; rep:repositoryImpl [ rep:repositoryType "openrdf:SailRepository"; sr:sailImpl [ sail:sailType "openrdf:MemoryStore" ] ].';
  let r=await fetch(base+'/rdf4j-server/repositories/knowledge-graph',{method:'PUT',headers:{'content-type':'text/turtle'},body:config});
  if(!r.ok) throw Error('Create RDF4J repo: '+r.status+' '+await r.text());
  r=await fetch(base+'/rdf4j-server/repositories/knowledge-graph/statements',{method:'POST',headers:{'content-type':'text/turtle'},body:await fs.readFile(path.join(root,'demo.ttl'),'utf8')});
  if(!r.ok)throw Error('Import into RDF4J: '+r.status+' '+await r.text());
  stamp('real-repository-loaded',{repository:'knowledge-graph'});
  browser=await chromium.launch({headless:true,args:['--no-sandbox','--disable-dev-shm-usage']});
  async function context(){const c=await browser.newContext({viewport:{width:1600,height:900},recordVideo:{dir:out,size:{width:1600,height:900}},colorScheme:'dark'});await c.addInitScript(()=>localStorage.setItem('rdf4j-workbench-theme','dark'));return c;}
  async function completeVideo(page,ctx,target){let src=await page.video().path();await ctx.close();await fs.copyFile(src,path.join(out,target));stamp('native-playwright-video-saved',{path:target});}
  const query='PREFIX ex: <https://example.org/knowledge/>\nPREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n\nSELECT ?technology ?category ?connectsTo WHERE {\n  ?node rdfs:label ?technology ;\n        ex:category ?category ;\n        ex:connectsTo / rdfs:label ?connectsTo .\n}\nORDER BY DESC(?technology = "RDF4J") ?category ?technology\nLIMIT 50';
  const qctx=await context();const qpage=await qctx.newPage();
  qpage.on('pageerror',e=>stamp('workbench-pageerror',{error:String(e)}));
  await qpage.goto(base+'/rdf4j-workbench/repositories/knowledge-graph/query?query='+encodeURIComponent(query),{waitUntil:'domcontentloaded',timeout:40000});
  await qpage.locator('#exec').waitFor({timeout:30000});
  await nap(1100);
  await qpage.screenshot({path:path.join(out,'01-query-ready.png')});
  stamp('real-ui-execute-click',{page:'query'});
  await qpage.locator('#exec').click();
  await qpage.locator('button.query-results__fullscreen:visible').waitFor({timeout:30000});
  await nap(950);
  await qpage.screenshot({path:path.join(out,'02-query-result.png')});
  await qpage.locator('button.query-results__fullscreen:visible').click();
  await nap(1350);
  await qpage.screenshot({path:path.join(out,'03-query-fullscreen.png')});
  await qpage.keyboard.press('Escape');
  await nap(650);
  await completeVideo(qpage,qctx,'query-native.webm');
  const gctx=await context();const gpage=await gctx.newPage();
  gpage.on('pageerror',e=>stamp('graph-pageerror',{error:String(e)}));
  await gpage.goto(base+'/rdf4j-workbench/repositories/knowledge-graph/graph',{waitUntil:'domcontentloaded',timeout:30000});
  await nap(850);
  await gpage.getByRole('button',{name:'Query editor',exact:true}).click();
  const seed='PREFIX ex: <https://example.org/knowledge/>\nSELECT ?source ?predicate ?target WHERE {\n  BIND(ex:RDF4J AS ?source)\n  ?source ex:connectsTo ?target .\n  BIND(ex:connectsTo AS ?predicate)\n} LIMIT 2';
  await gpage.locator('#sparql').fill(seed);
  await nap(650);
  stamp('real-ui-graph-query-click',{query:seed});
  await gpage.getByRole('button',{name:'Run query',exact:true}).click();
  await nap(1200);
  await gpage.getByRole('button',{name:'Close query editor',exact:true}).click();
  await nap(650);
  await gpage.screenshot({path:path.join(out,'04-seed-graph.png')});
  const ns='https://example.org/knowledge/';
  async function expand(name){
    const selector='g.graph-node[data-node-id="'+ns+name+'"]';
    const node=gpage.locator(selector);const before=await gpage.locator('g.graph-node').count();
    if(await node.count()===0){stamp('node-not-yet-present',{name,before});return false;}
    try {
      const box=await node.boundingBox();
      if(!box || box.width<1){stamp('node-unavailable',{name,before});return false;}
      await node.dblclick({force:true,timeout:5000});
      stamp('native-double-click-expand',{name,nodeCountBefore:before});
      await nap(1400);
      const after=await gpage.locator('g.graph-node').count();
      await gpage.screenshot({path:path.join(out,'05-expanded-'+name+'.png')});
      report.nativeActions.push({action:'double-click',name,nodeCountBefore:before,nodeCountAfter:after,method:'SVG DOM event through Playwright'});
      stamp('native-layout-after-expansion',{name,nodeCountAfter:after});
      return true;
    }catch(e){stamp('native-expand-error',{name,error:String(e)});return false;}
  }
  await expand('RDF4J');
  await expand('SPARQL');
  await expand('Workbench');
  await expand('LMDB');
  await expand('RDF');
  await expand('KnowledgeGraphs');
  await nap(1700);
  await gpage.screenshot({path:path.join(out,'06-final-graph.png')});
  await completeVideo(gpage,gctx,'graph-native.webm');
  report.graphNodeCount=report.nativeActions.at(-1)?.nodeCountAfter??null;
  stamp('complete',{graphExpansionActions:report.nativeActions.length});
  await fs.writeFile(path.join(out,'capture.json'),JSON.stringify(report,null,2));
})().catch(async e=>{
  stamp('failure',{message:String(e),stack:e.stack});
  await fs.writeFile(path.join(out,'capture.json'),JSON.stringify(report,null,2));
  process.exitCode=1;
}).finally(async()=>{
  try{if(browser)await browser.close();}catch{}
  if(app)app.kill('SIGTERM');
});
