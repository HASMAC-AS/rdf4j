const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const vm = require('node:vm');
const path = require('node:path');
function api(extra = {}) {
 const context = {workbench:{}, ...extra};
 vm.runInNewContext(fs.readFileSync(path.resolve(__dirname, '../../../../target/frontend/scripts/backups.js'), 'utf8'), context);
 return context.workbench.backups;
}
test('only RUNNING managed jobs disable operations', () => {
 const b = api(); assert.equal(b.isRunning({state:'RUNNING'}), true);
 assert.equal(b.isRunning({state:'FAILED'}), false); assert.equal(b.isRunning(null), false);
});
test('restore IDs reject path syntax and require a distinct repository', () => {
 const b = api(); assert.equal(b.validTarget('restored-copy','source'), true);
 for(const id of ['', '../other', 'a/b', 'a\\b', '.', 'source']) assert.equal(b.validTarget(id,'source'), false);
});
test('leaving the route aborts status request and prevents later polling', async () => {
 let resolve, signal, scheduled = 0;
 const b = api({URL, AbortController, fetch:(url, options) => { signal = options.signal; return new Promise(r=>resolve=r); },
  clearTimeout:()=>{}, setTimeout:()=>{scheduled++;}});
 const node = () => ({textContent:'', appendChild(){}, setAttribute(){}, addEventListener(){}});
 const host=node(), outlet={ownerDocument:{createElement:node}, querySelector:()=>host};
 const dispose=b.mount(outlet,{url:new URL('https://example.test/workbench/repositories/source/backups')});
 dispose(); assert.equal(signal.aborted, true);
 resolve({ok:true,json:()=>Promise.resolve({supported:false})});
 await new Promise(r=>setImmediate(r)); assert.equal(scheduled,0);
});
function mounted() {
 const requests=[], timers=[];
 const document={activeElement:null,createElement:node};
 function node(tag) {
  let content='';
  return {tag,children:[],listeners:{},attributes:{},disabled:false,
   set textContent(value){content=String(value);this.children=[];}, get textContent(){return content;},
   appendChild(child){this.children.push(child);},setAttribute(key,value){this.attributes[key]=value;},
   getAttribute(key){return this.attributes[key] || null;}, focus(){document.activeElement=this;},
   contains(child){return this===child || this.children.some(n=>n.contains(child));},
   querySelectorAll(){return this.children.flatMap(n=>[...(n.attributes['data-backup-control']?[n]:[]),...n.querySelectorAll()]);},
   querySelector(){return this.children.find(n=>n.attributes.role==='status') || null;},
   addEventListener(type,handler){this.listeners[type]=handler;}};
 }
 const host=node('section'),outlet={ownerDocument:document,querySelector:()=>host};
 const b=api({URL,URLSearchParams,AbortController,workbench:{csrfHeaders:()=>({'X-RDF4J-CSRF-Token':'token'})},
  fetch:(url,options)=>new Promise(resolve=>requests.push({url,options,resolve})),
  setTimeout:fn=>{timers.push(fn);return timers.length;},clearTimeout:()=>{},window:{prompt:()=>null,confirm:()=>false}});
 const dispose=b.mount(outlet,{url:new URL('https://example.test/workbench/repositories/source/backups')});
 return {host,requests,timers,dispose,document};
}
const flush=()=>new Promise(r=>setImmediate(r));
test('create backup is a single CSRF-protected POST and running status disables actions', async () => {
 const h=mounted();h.requests[0].resolve({ok:true,json:()=>Promise.resolve({supported:true,backups:[],job:null})});await flush();
 const start=h.host.children.find(n=>n.tag==='button');start.listeners.click();start.listeners.click();
 assert.equal(h.requests.length,2);const post=h.requests[1];
 assert.equal(post.options.method,'POST');assert.equal(post.options.body.get('action'),'backup');
 assert.equal(post.options.headers['X-RDF4J-CSRF-Token'],'token');
 post.resolve({ok:true,json:()=>Promise.resolve({operation:'BACKUP',state:'RUNNING'})});await flush();
 assert.equal(h.host.children.find(n=>n.tag==='button').disabled,true);h.dispose();
});
test('unavailable stores have no mutating controls; server failure strings remain text', async () => {
 const h=mounted();h.requests[0].resolve({ok:true,json:()=>Promise.resolve({supported:false})});await flush();
 assert.equal(h.host.children.some(n=>n.tag==='button'),false);h.dispose();
 const j=mounted();j.requests[0].resolve({ok:true,json:()=>Promise.resolve({supported:true,backups:[],job:{operation:'BACKUP',state:'FAILED',message:'<img src=x onerror=alert(1)>'}})});await flush();
 assert.ok(j.host.children.some(n=>n.textContent.includes('<img src=x')));j.dispose();
});
test('service-wide direct or scheduled operation is visible without a Workbench job', async () => {
 const h=mounted();h.requests[0].resolve({ok:true,json:()=>Promise.resolve({supported:true,backups:[],job:null,
  activeOperation:{id:'external',operation:'BACKUP',phase:'VERIFYING',startedAt:'2026-11-12T09:00:00Z'}})});await flush();
 assert.ok(h.host.children.some(n=>n.textContent.includes('VERIFYING')));
 assert.equal(h.host.children.find(n=>n.tag==='button').disabled,true);h.dispose();
});
test('unchanged polling preserves existing controls instead of replacing focused DOM', async () => {
 const h=mounted();const status={supported:true,backups:[],job:null,activeOperation:null};
 h.requests[0].resolve({ok:true,json:()=>Promise.resolve(status)});await flush();
 const original=h.host.children.find(n=>n.tag==='button');
 h.timers[h.timers.length-1]();h.requests[1].resolve({ok:true,json:()=>Promise.resolve(status)});await flush();
 assert.equal(h.host.children.find(n=>n.tag==='button'),original);h.dispose();
});
test('changed status preserves keyboard focus on the matching enabled control', async () => {
 const h=mounted();h.requests[0].resolve({ok:true,json:()=>Promise.resolve({supported:true,backups:[],job:null})});await flush();
 const oldRefresh=h.host.children.filter(n=>n.tag==='button')[1];oldRefresh.focus();
 h.timers[h.timers.length-1]();h.requests[1].resolve({ok:true,json:()=>Promise.resolve({supported:true,backups:[],job:null,
 activeOperation:{id:'direct',operation:'BACKUP',phase:'COPYING',startedAt:'now'}})});await flush();
 const currentRefresh=h.host.children.filter(n=>n.tag==='button')[1];
 assert.notEqual(currentRefresh,oldRefresh);assert.equal(h.document.activeElement,currentRefresh);h.dispose();
});
test('when a focused action becomes disabled focus moves to and stays on activity status', async () => {
 const h=mounted();h.requests[0].resolve({ok:true,json:()=>Promise.resolve({supported:true,backups:[],job:null})});await flush();
 h.host.children.find(n=>n.tag==='button').focus();
 for(const phase of ['COPYING','CHECKSUMMING']) {
  h.timers[h.timers.length-1]();h.requests[h.requests.length-1].resolve({ok:true,json:()=>Promise.resolve({supported:true,backups:[],job:null,
   activeOperation:{id:'direct',operation:'BACKUP',phase,startedAt:'now'}})});await flush();
  assert.equal(h.document.activeElement.attributes.role,'status');
  assert.ok(h.document.activeElement.textContent.includes(phase));
 }
 h.dispose();
});
