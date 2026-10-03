// Original, timed Minecraft framebuffers. Models and actual casts are recorded separately.
import {readFileSync,writeFileSync,existsSync,mkdirSync} from 'node:fs';
import {join,resolve,dirname} from 'node:path';
import {fileURLToPath,pathToFileURL} from 'node:url';
const root=resolve(dirname(fileURLToPath(import.meta.url)),'../..'),runtime=join(root,'.build/minecraft-mcp');
const out=join(runtime,'evidence',process.env.NGW_EVIDENCE??'all-abilities'),sdk=join(runtime,'bridge/node_modules/@modelcontextprotocol/sdk/dist/esm');
const {Client}=await import(pathToFileURL(join(sdk,'client/index.js')));
const {StdioClientTransport}=await import(pathToFileURL(join(sdk,'client/stdio.js')));
const client=new Client({name:'ngw-animation-recorder',version:'1'}),sleep=ms=>new Promise(r=>setTimeout(r,ms));
const resultFile=join(runtime,'server/plugins/VisualRegressionProbe/last-result.json');
async function tool(name,args={}){const r=await client.callTool({name,arguments:args});const v=r.content.find(c=>c.type==='text')?.text;if(r.isError)throw new Error(v);return JSON.parse(v);}
const command=command=>tool('execute_command',{command});
function result(){try{return JSON.parse(readFileSync(resultFile,'utf8'));}catch{return null;}}
async function fixture(args){const before=result()?.sequence;await command('/ngwvisual '+args);for(let i=0;i<100;i++){await sleep(30);const r=result();if(r&&r.sequence!==before&&r.command.join(' ')===args){if(!r.ok)throw new Error(JSON.stringify(r));return r;}}throw new Error('Fixture timeout: '+args);}
async function action(action,value){return tool('client_action',{action,...(value===undefined?{}:{value})});}
const manifest=join(out,'animations.json');mkdirSync(out,{recursive:true});
const clips=existsSync(manifest)?JSON.parse(readFileSync(manifest,'utf8')):[];
const session=JSON.parse(readFileSync(join(runtime,'session.json'),'utf8'));
function save(row){const index=clips.findIndex(c=>c.kind===row.kind&&c.id===row.id&&(row.kind==='model'||c.view===row.view));if(index>=0)clips[index]=row;else clips.push(row);writeFileSync(manifest,JSON.stringify(clips,null,2));}
async function record(row,trigger,duration=1800){
  const name=Date.now()+'-'+row.kind+'-'+row.id+'-'+row.view;
  const folder=join(runtime,'client/recordings',name),path=join(folder,'clip.json');
  const started=performance.now();
  await tool('record_clip',{name,durationMs:duration});
  await sleep(300);row.triggerMs=performance.now()-started;
  const state=await trigger();row.triggerState=state;
  for(let i=0;i<160&&!existsSync(path);i++)await sleep(50);
  if(!existsSync(path))throw new Error('Recording did not finish');
  const clip=JSON.parse(readFileSync(path,'utf8'));if(clip.error)throw new Error(clip.error);
  row.frames=clip.frames.map(f=>({path:join(folder,f.file),ms:f.ms}));
  row.durationMs=clip.durationMs;row.fps=Math.round((row.frames.length-1)*100000/(row.frames.at(-1).ms-row.frames[0].ms))/100;
  if(row.frames.length<20||row.fps<10)throw new Error('Insufficient temporal capture: '+row.frames.length+' frames / '+row.fps+' fps');
  if(row.kind==='cast'&&row.id==='megumin') {
    const info=await tool('get_player_info');
    row.after={...info,dead:info.health===0,source:'client player state; player commands are disabled after death'};
  }else row.after=await fixture('state');
  if(row.kind==='model'&&row.after.displays!==0)throw new Error('Model did not expire');
  row.pluginSha256=session.pluginSha256;row.artPackSha1=session.artPackSha1??null;row.ok=true;row.recorded=new Date().toISOString();
}
try{
  await client.connect(new StdioClientTransport({command:process.execPath,args:[join(root,'scripts/minecraft-mcp/bridge.mjs')],cwd:root}));
  await tool('enter_control_mode');
  if((await tool('get_player_info')).health===0){await action('respawn');await sleep(500);}
  const catalog=(await fixture('catalog')).catalog;
  if(process.env.NGW_REQUIRE_ART==='1' && !(await fixture('state')).artPackLoaded)throw new Error('The game has not acknowledged loading the NewGodWar art pack');
  const selected=process.argv.slice(3),mode=process.argv[2]??'models';
  if(mode==='models'){
    await fixture('clear');await command('/gamemode creative');await command('/time set noon');await command('/weather clear');
    await command('/gamerule minecraft:advance_time false');
    await command('/tp @s 0.5 -60 0.5 0 0');await action('hud','false');await action('camera','front');await action('particles','ALL');
    await tool('set_view_angle',{yaw:0,pitch:0});await sleep(500);
    for(const model of catalog.models.filter(m=>!selected.length||selected.includes(m.id))){
      const overhead=['athena.PHALANX','design.ABYSS','design.CLOCK','design.EXPLOSION_CHARGE','runesmith.FIRE_RUNE','runesmith.FROST_RUNE'].includes(model.id);
      await action('camera',overhead?'back':'front');await tool('set_view_angle',{yaw:0,pitch:overhead?50:0});
      const row={kind:'model',id:model.id,label:model.description,view:overhead?'up':'front'};
      try{await record(row,()=>fixture(`model ${model.id} objects`));}
      catch(error){row.ok=false;row.error=error.message;process.exitCode=1;}
      save(row);console.log(`${model.id}: ${row.ok?row.frames.length+' frames / '+row.fps+' fps':row.error}`);
    }
  }else if(mode==='casts'||mode==='advanced'){
    const advanced=mode==='advanced';
    const ids=selected.length?selected:advanced?['chronos','graviton']:['invincibility','chronos','thor','nike','persephone','megumin'];
    for(const id of ids){
      const camera=!advanced&&['blacksmith','siksin'].includes(id)?'first':'back';
      const ability=catalog.abilities.find(a=>a.id===id),row={kind:'cast',id,label:ability.name+(advanced?' · 고급 발동':camera==='first'?' · 일반 발동 · 1인칭':' · 일반 발동'),view:advanced?'back-advanced':camera};
      try{
        if((await tool('get_player_info')).health===0){await action('respawn');await sleep(500);}
        await fixture(`prepare ${id} enemy`);await action('hud','false');await action('camera',camera);
        await tool('set_view_angle',{yaw:0,pitch:advanced?40:id==='megumin'?30:0});await sleep(750);
        const before=await fixture('state');row.before=before;const logFile=join(runtime,'logs/client.log'),offset=readFileSync(logFile,'utf8').length;
        await record(row,async()=>{
          const result=await action(advanced?'use':'attack');
          if(id==='persephone'||id==='nike') {await sleep(100);await tool('set_view_angle',{yaw:0,pitch:30});}
          return result;
        },advanced||id==='megumin'?4300:2600);
        row.log=readFileSync(logFile,'utf8').slice(offset);row.before=before;
        const success=row.log.includes('✦ '+ability.name+' · '+(advanced?'고급':'일반')+' 사용!')||row.log.includes('✦ ['+ability.name+' ·');
        if(!success&&row.after.stones>=before.stones)throw new Error('Cast was not confirmed');
        if(id==='megumin'&&!row.after.dead)throw new Error('Delayed explosion did not complete');
      }catch(error){row.ok=false;row.error=error.message;process.exitCode=1;}
      save(row);console.log(`${id}: ${row.ok?row.frames.length+' frames / '+row.fps+' fps':row.error}`);
    }
  }else throw new Error('Use models, casts or advanced');
}finally{await client.close();}
