import {readFileSync,writeFileSync,existsSync,copyFileSync,mkdirSync,readdirSync} from 'node:fs';
import {resolve,join} from 'node:path';
import {createHash} from 'node:crypto';
const root=resolve(import.meta.dirname,'../..'),out=join(root,'.build/minecraft-mcp/evidence',process.env.NGW_EVIDENCE??'scope-effects');
const read=name=>JSON.parse(readFileSync(join(out,name),'utf8'));
const models=read('models.json'),clips=read('animations.json'),casts=read('casts.json'),session=read('verification-session.json');
if(existsSync(join(out,'models-retry.json')))for(const row of read('models-retry.json')){const i=models.findIndex(c=>c.id===row.id);if(i>=0)models[i]=row;}
const baseline=read('baseline-session.json');
const changedModels=new Set(readFileSync(join(root,'plugin/src/main/resources/effect-art.tsv'),'utf8').split(/\r?\n/)
  .filter(r=>/\|(pages|steam|forge)$/.test(r)).map(r=>r.split('|')[0]));
if(existsSync(join(out,'casts-retry.json')))for(const row of read('casts-retry.json')){const i=casts.findIndex(c=>c.id===row.id);if(i>=0)casts[i]=row;}
const errors=[];
function check(ok,message){if(!ok)errors.push(message);}
function file(path){check(typeof path==='string'&&existsSync(path),'Missing evidence: '+path);}
function sameIds(actual,expected,label){
  check(actual.length===new Set(actual).size,'Duplicate '+label);
  check(actual.length===expected.length&&actual.every(id=>expected.includes(id)),'Incomplete '+label);
}
const catalog=read('catalog.json');
sameIds(models.map(m=>m.id),catalog.models.map(m=>m.id),'model IDs');
sameIds(clips.filter(c=>c.kind==='model').map(c=>c.id),catalog.models.map(m=>m.id),'model animation IDs');
sameIds(casts.map(c=>c.id),catalog.abilities.map(a=>a.id),'ability IDs');
check(models.length===97,'Expected all 97 visual models');
for(const m of models){check(m.ok&&m.cleanup&&m.captures.length===5,'Model conditions failed: '+m.id);sameIds(m.captures.map(c=>c.name),['front','first','up','minimal','fallback'],m.id+' conditions');m.captures.forEach(c=>file(c.path));}
check(clips.filter(c=>c.kind==='model').length===97,'Expected 97 model animations');
for(const c of clips){
  check(c.ok&&c.fps>=10&&c.frames.length>=20,'Animation failed: '+c.id);
  const retained=c.kind==='model'&&!changedModels.has(c.id)&&c.pluginSha256===baseline.pluginSha256;
  check((c.pluginSha256===session.pluginSha256||retained)&&c.artPackSha1===session.artPackSha1,'Unverified animation build: '+c.id);
  for(const f of c.frames??[])file(f.path);
}
check(casts.length===93,'Expected all 93 ability input scenarios');
const attempts={};
for(const row of casts){
  check(!row.error,'Ability error: '+row.id+' '+row.error);
  sameIds(row.attempts.map(a=>a.kind),['normal','advanced'],row.id+' inputs');
  for(const a of row.attempts){attempts[a.status]=(attempts[a.status]??0)+1;check(a.status!=='needs-review','Unconfirmed input: '+row.id+' '+a.kind);if(a.picture)file(a.picture);}
  if(row.passive)file(row.passive.picture);
  if(row.delayed)file(row.delayed.picture);
}
const food=read('foods/foods.json');check(food.ok&&food.items.length===6,'Food rendering missing');file(food.gallery);food.items.forEach(f=>file(f.path));
check(food.tooltips?.length===6,'Expected six food tooltip captures');
for(const path of food.tooltips??[])file(path);
check(food.session.pluginSha256===session.pluginSha256&&food.session.artPackSha1===session.artPackSha1,'Food build differs');
const deployed=join(root,'.build/minecraft-mcp/server/plugins/NewGodWar.jar');
const digest=createHash('sha256').update(readFileSync(deployed)).digest('hex');check(digest===session.pluginSha256,'Deployed snapshot changed');
const archive=join(out,'tested');mkdirSync(archive,{recursive:true});copyFileSync(deployed,join(archive,'NewGodWar.jar'));
const packName=readdirSync(join(root,'build/effect-pack/dist')).find(n=>/^NewGodWar-Art-[0-9a-f]{8}\.zip$/.test(n));
check(packName,'Build the combined art pack first');
const pack=join(root,'build/effect-pack/dist',packName);
check(createHash('sha1').update(readFileSync(pack)).digest('hex')===session.artPackSha1,'Pack differs from the captured build');
copyFileSync(pack,join(archive,packName));
const unitTests={tests:0,failures:0,errors:0,skipped:0};
for(const name of readdirSync(join(archive,'unit-results')).filter(n=>n.endsWith('.xml'))){
  const header=readFileSync(join(archive,'unit-results',name),'utf8').match(/<testsuite\s[^>]+>/)?.[0]??'';
  for(const key of Object.keys(unitTests))unitTests[key]+=Number(header.match(new RegExp(key+'="(\\d+)"'))?.[1]??0);
}
check(unitTests.tests>0&&unitTests.failures===0&&unitTests.errors===0,'Unit test snapshot failed');
const serverRegressions=['1.12.2','26.3'].map(version=>{
  const path=join(archive,'paper-'+version+'.log'),log=readFileSync(path,'utf8');
  const ok=log.includes('ABILITY REGRESSION PASS')&&!log.includes('ABILITY REGRESSION FAIL');
  check(ok,'Server regression failed: '+version);return {version,ok,log:path};
});
const hiddenRuntimeStopped=!existsSync(join(root,'.build/minecraft-mcp/session.json'));
const result={generated:new Date().toISOString(),ok:errors.length===0,errors,session,
  unitTests,serverRegressions,hiddenRuntimeStopped,
  modelConditions:models.reduce((n,m)=>n+m.captures.length,0),modelAnimations:97,
  actualCastAnimations:clips.filter(c=>c.kind==='cast').length,originalFrames:clips.reduce((n,c)=>n+(c.frames?.length??0),0),
  minimumFps:Math.min(...clips.map(c=>c.fps)),abilityInputScenarios:casts.length,attempts,passiveCallbacks:casts.filter(c=>c.passive).length,
  foodItems:food.items.length,foodTooltips:food.tooltips?.length??0,
  retainedModelBuild:baseline.pluginSha256,recapturedModels:[...changedModels],
  revisionNote:'After the baseline sweep, only pages/steam/forge hand placements changed. Those 10 models were recaptured in all five conditions and animation. Other baseline model captures are retained with their original build hashes; all ability input scenarios use the final build.',
  scopeAudit:'93 ability source paths; shared rules plus targeted kit interaction regression tests',
  castBuildProvenance:'The input sweep and retry ran in the recorded final client/server session. Input rows use that shared session record; animation and food manifests include explicit build hashes.',
  limits:['Minecraft 26.3, supplied pack, no shaders, muted, 960x540.','Cast activation is confirmed by resources or success chat; not all downstream branches are asserted.',
    'Passive and special-input preconditions include server event fixtures.','All model conditions captured; manual visual review is selective.',
    'Other resource packs/shaders, every probability branch and all multiplayer combinations are not exhaustively verified.']};
writeFileSync(join(out,'verification.json'),JSON.stringify(result,null,2));
console.log(JSON.stringify(result,null,2));if(errors.length)process.exitCode=1;
