// Full catalogue/render/cast checks against the hidden real client and isolated server.
import { readFileSync, writeFileSync, mkdirSync, copyFileSync, existsSync } from 'node:fs';
import { dirname, resolve, join } from 'node:path';
import { fileURLToPath, pathToFileURL } from 'node:url';
const root=resolve(dirname(fileURLToPath(import.meta.url)),'../..');
const runtime=join(root,'.build/minecraft-mcp');
const output=join(runtime,'evidence',process.env.NGW_EVIDENCE??'all-abilities');
mkdirSync(output,{recursive:true});
const sdk=join(runtime,'bridge/node_modules/@modelcontextprotocol/sdk/dist/esm');
const {Client}=await import(pathToFileURL(join(sdk,'client/index.js')));
const {StdioClientTransport}=await import(pathToFileURL(join(sdk,'client/stdio.js')));
const client=new Client({name:'newgodwar-complete-visual-suite',version:'1.0.0'});
const sleep=ms=>new Promise(r=>setTimeout(r,ms));
const resultPath=join(runtime,'server/plugins/VisualRegressionProbe/last-result.json');
const logPath=join(runtime,'logs/client.log');
async function tool(name,args={}) {
  const result=await client.callTool({name,arguments:args});
  const texts=result.content.filter(c=>c.type==='text').map(c=>{try{return JSON.parse(c.text);}catch{return c.text;}});
  if(result.isError) throw new Error(JSON.stringify(texts));
  return texts[0];
}
async function command(command){return tool('execute_command',{command});}
function readResult() {
  try{return JSON.parse(readFileSync(resultPath,'utf8'));}
  catch(error){if(['ENOENT','EACCES','EPERM'].includes(error.code)||error instanceof SyntaxError)return null;throw error;}
}
async function fixture(args) {
  const before=readResult()?.sequence;
  await command('/ngwvisual '+args);
  for(let i=0;i<100;i++) {
    await sleep(30);
    const value=readResult();if(!value)continue;
    if(value.sequence===before || value.command.join(' ')!==args)continue;
    if(!value.ok)throw new Error(JSON.stringify(value));
    return value;
  }
  throw new Error('Server fixture did not acknowledge '+args);
}
async function capture(folder,name) {
  mkdirSync(folder,{recursive:true});
  const info=await tool('screenshot');
  const path=join(folder,name+'.png');copyFileSync(info.path,path);return path;
}
async function action(action,value){return tool('client_action',value===undefined?{action}:{action,value});}
function save(name,value){writeFileSync(join(output,name+'.json'),JSON.stringify(value,null,2));}

try {
  await client.connect(new StdioClientTransport({command:process.execPath,args:[join(root,'scripts/minecraft-mcp/bridge.mjs')],cwd:root}));
  await tool('enter_control_mode');
  if(process.env.NGW_REQUIRE_ART==='1' && !(await fixture('state')).artPackLoaded)throw new Error('The game has not acknowledged loading the NewGodWar art pack');
  const catalog=(await fixture('catalog')).catalog;save('catalog',catalog);
  save('verification-session',JSON.parse(readFileSync(join(runtime,'session.json'),'utf8')));
  const selected=process.argv.slice(3);
  if(process.argv[2]==='models') {
    await fixture('clear');await command('/gamemode creative');await command('/time set noon');await command('/weather clear');
    await command('/tp @s 0.5 -60 0.5 0 0');await action('hud','false');
    const results=[];const scenarios=[['front','front',0,'ALL','objects'],['first','first',0,'ALL','objects'],
      ['up','back',50,'ALL','objects'],['minimal','front',0,'MINIMAL','objects'],['fallback','front',0,'ALL','particles']];
    for(const model of catalog.models.filter(m=>!selected.length||selected.includes(m.id))) {
      const row={...model,captures:[],started:new Date().toISOString()};
      try {
        for(const [name,camera,pitch,particles,mode] of scenarios) {
          await action('camera',camera);await action('particles',particles);await tool('set_view_angle',{yaw:0,pitch});
          const state=await fixture(`model ${model.id} ${mode}`);
          const expected=state.artPackLoaded?model.artParts:model.parts;
          if(mode==='objects' && state.displays!==expected)throw new Error(`Expected ${expected} displays, got ${state.displays}`);
          await sleep(220);
          row.captures.push({name,path:await capture(join(output,'models',model.id),name),mode,displays:state.displays});
        }
        await fixture(`model ${model.id} objects`);await sleep(1150);
        row.cleanup=(await fixture('state')).displays===0;
        if(!row.cleanup)throw new Error('Model did not expire');
        row.ok=true;
      } catch(error){row.ok=false;row.error=error.message;process.exitCode=1;}
      results.push(row);save('models'+(selected.length?'-retry':''),results);
      console.log(`${results.length}/${catalog.models.length} ${model.id}: ${row.ok?'PASS':row.error}`);
    }
  } else if(process.argv[2]==='passives') {
    if(!selected.length)throw new Error('Select abilities for a standalone passive fixture');
    const results=[];
    for(const ability of catalog.abilities.filter(a=>selected.includes(a.id))) {
      const row={id:ability.id,name:ability.name};
      try {
        await fixture(`prepare ${ability.id} enemy`);await action('hud','false');await action('camera','back');
        await tool('set_view_angle',{yaw:0,pitch:0});await sleep(250);
        const offset=readFileSync(logPath,'utf8').length;
        const state=await fixture('passive');await sleep(200);
        const log=readFileSync(logPath,'utf8').slice(offset);
        if(ability.id==='creeper'&&!state.observations.lightningCharged)throw new Error('Creeper lightning charge missing');
        row.passive={status:'callbacks-exercised',state,log,picture:await capture(join(output,'casts',ability.id),'passive')};
      }catch(error){row.error=error.message;process.exitCode=1;}
      results.push(row);save('passives-retry',results);console.log(`${ability.id}: ${row.error??'PASS'}`);
    }
  } else if(process.argv[2]==='casts') {
    const results=[];
    for(const ability of catalog.abilities.filter(a=>!selected.length||selected.includes(a.id))) {
      const row={id:ability.id,name:ability.name,started:new Date().toISOString(),attempts:[]};
      try {
        const role=['hera','teleporter','asclepius'].includes(ability.id)?'ally':'enemy';
        if((await tool('get_player_info')).health===0){await action('respawn');await sleep(350);}
        await fixture(`prepare ${ability.id} ${role}`);await action('hud','false');await action('camera','back');
        for(const [kind,description] of [['normal',ability.normal],['advanced',ability.advanced]]) {
          if(description==='없음'){row.attempts.push({kind,status:'not-applicable'});continue;}
          if(kind==='advanced'&&!['thor','hera','odin','anubis','echo','hephaestus','bomber','runesmith','nike','sniper'].includes(ability.id))
            await fixture(`prepare ${ability.id} ${role}`);
          await command('/gw ability cooldown reset self');
          if(['zeus','bomber','iris','poseidon','megumin','graviton'].includes(ability.id)||(['frost','teleporter'].includes(ability.id)&&kind==='normal')) await tool('set_view_angle',{yaw:0,pitch:30});
          else await tool('set_view_angle',{yaw:0,pitch:0});
          await sleep(700);
          const before=await fixture('state');const offset=readFileSync(logPath,'utf8').length;
          await fixture('condition '+kind);
          if(ability.id==='harry')await fixture('chat '+(kind==='normal'?'Lumos':'Stupefy'));
          else if(ability.id==='hermione')await fixture('chat '+(kind==='normal'?'윙가르디움 레비오사':'프로테고'));
          else if(ability.id!=='sniper'&&!(ability.id==='voodoo'&&kind==='normal')) await action(kind==='normal'?'attack':'use');
          await sleep(180);
          const picture=await capture(join(output,'casts',ability.id),kind);
          const after=await fixture('state');
          if(ability.id==='hades' && (kind==='normal'?after.y:after.targetY)>=-64)
            throw new Error('Abyss destination was not below the modern world floor');
          const log=readFileSync(logPath,'utf8').slice(offset);
          const confirmed=log.includes('✦ '+ability.name+' · '+(kind==='normal'?'일반':'고급')+' 사용!')
            || log.includes('✦ ['+ability.name+' ·') || after.stones<before.stones
            || after[kind+'Cooldown']>before[kind+'Cooldown']
            || (ability.id==='bomber'&&kind==='normal'&&after.displays>0);
          const infoOnly=ability.id==='snow'&&kind==='advanced';
          const prepared=['midoriya','sniper'].includes(ability.id)&&kind==='normal'&&log.includes('준비');
          row.attempts.push({kind,status:confirmed?'activated':prepared?'prepared':infoOnly?'informational':'needs-review',before,after,log,picture});
          if(ability.id==='sniper'&&kind==='normal')await sleep(4500);
          if(kind==='normal'&&['thor','pan'].includes(ability.id))await sleep(1500);
          if(kind==='normal'&&ability.id==='megumin') {
            await sleep(3200);const clientState=await tool('get_player_info');
            row.delayed={...clientState,dead:clientState.health===0,source:'client state after death; chat commands are disabled'};
            row.delayed.picture=await capture(join(output,'casts',ability.id),'detonation');
            if(!row.delayed.dead)throw new Error('Megumin did not complete its delayed explosion/death');
          }
        }
        if(!row.attempts.some(a=>a.after?.dead)&&!row.delayed?.dead) {
          const state=await fixture('passive');await sleep(200);
          row.passive={status:'callbacks-exercised',state,picture:await capture(join(output,'casts',ability.id),'passive')};
        }
      }catch(error){row.error=error.message;process.exitCode=1;}
      if(row.attempts.some(a=>a.status==='needs-review'))process.exitCode=1;
      results.push(row);save('casts'+(selected.length?'-retry':''),results);
      console.log(`${results.length}/${catalog.abilities.length} ${ability.id}: ${row.error??row.attempts.map(a=>a.kind+'='+a.status).join(', ')}`);
    }
  } else console.log(`Catalogue: ${catalog.abilities.length} abilities, ${catalog.models.length} models`);
} finally {await client.close();}
