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
  let triggerError;
  try{row.triggerState=await trigger();}catch(error){triggerError=error;}
  for(let i=0;i<160&&!existsSync(path);i++)await sleep(50);
  if(!existsSync(path))throw new Error('Recording did not finish');
  const clip=JSON.parse(readFileSync(path,'utf8'));if(clip.error)throw new Error(clip.error);
  row.frames=clip.frames.map(f=>({path:join(folder,f.file),ms:f.ms}));
  row.durationMs=clip.durationMs;row.fps=Math.round((row.frames.length-1)*100000/(row.frames.at(-1).ms-row.frames[0].ms))/100;
  if(triggerError)throw triggerError;
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
  if(mode==='models'||mode==='outlines'){
    await fixture('prepare invincibility enemy');
    await fixture('clear');await command('/gamemode creative');await command('/time set noon');await command('/weather clear');
    await command('/gamerule minecraft:advance_time false');
    await command('/tp @s 0.5 -60 0.5 0 0');await action('hud','false');await action('camera','front');await action('particles','ALL');
    await tool('set_view_angle',{yaw:0,pitch:0});await sleep(500);
    for(const model of catalog.models.filter(m=>!selected.length||selected.includes(m.id))){
      const overhead=['athena.PHALANX','design.ABYSS','design.CLOCK','design.EXPLOSION_CHARGE','runesmith.FIRE_RUNE','runesmith.FROST_RUNE'].includes(model.id);
      await action('camera',overhead?'back':'front');await tool('set_view_angle',{yaw:0,pitch:overhead?50:0});
      const row={kind:'model',id:model.id,label:model.description,view:mode==='outlines'?'fallback':overhead?'up':'front'};
      try{await record(row,()=>fixture(`model ${model.id} ${mode==='outlines'?'particles':'objects'}`));}
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
        if(!success&&row.after.stones>=before.stones&&row.after[(advanced?'advanced':'normal')+'Cooldown']<=before[(advanced?'advanced':'normal')+'Cooldown'])throw new Error('Cast was not confirmed');
        if(id==='megumin'&&!row.after.dead)throw new Error('Delayed explosion did not complete');
      }catch(error){row.ok=false;row.error=error.message;process.exitCode=1;}
      save(row);console.log(`${id}: ${row.ok?row.frames.length+' frames / '+row.fps+' fps':row.error}`);
    }
  }else if(mode==='motion'){
    const scenarios=selected.length?selected:['side','first','moving','elevated','near','still','fallback','cancel'];
    for(const scenario of scenarios){
      const row={kind:'cast',id:'thor',label:'토르 · 동작 회귀 · '+scenario,view:'motion-'+scenario};
      try{
        const prepared=await fixture('prepare thor enemy');
        const distance=scenario==='near'?3.5:8,dy=scenario==='elevated'?4:0;
        if(dy)await command('/setblock 0 -57 8 minecraft:stone');
        await command(`/tp ${prepared.target} 0.5 ${-60+dy} ${0.5+distance} 135 70`);
        await action('camera',scenario==='first'?'first':'back');await action('hud','false');
        await tool('set_view_angle',{yaw:0,pitch:-Math.atan2(dy,distance)*180/Math.PI});
        if(scenario==='still')await fixture('settings animations false');
        if(scenario==='fallback')await fixture('settings objects false');
        await sleep(250);row.before=await fixture('state');
        await record(row,async()=>{
          await fixture('trace');const cast=await action('attack');await sleep(150);
          row.during=await fixture('state');
          if(scenario!=='fallback'&&row.during.displays===0)throw new Error('Hammer was not rendered');
          if(scenario!=='first')await tool('set_view_angle',{yaw:-50,pitch:12});
          if(scenario==='moving'){
            await sleep(350);
            for(let i=0;i<5;i++){await command('/tp @s ~0.3 ~ ~');await sleep(80);}
          }
          if(scenario==='cancel'){await sleep(450);await fixture('prepare invincibility enemy');}
          return cast;
        },3100);
        if(row.during.stones>=row.before.stones)throw new Error('Throw was not activated');
        if(row.after.displays!==0)throw new Error('Hammer remained after catch/cancellation');
        if(!['fallback','cancel'].includes(scenario)){
          const main=row.after.motionTrace.flatMap(f=>f.entities.filter(e=>e.billboard==='FIXED').map(e=>({...e,tick:f.tick})));
          if(main.length<12)throw new Error('Insufficient trajectory samples');
          const direction=e=>{const yaw=e.yaw*Math.PI/180,pitch=e.pitch*Math.PI/180;return [-Math.sin(yaw)*Math.cos(pitch),-Math.sin(pitch),Math.cos(yaw)*Math.cos(pitch)];};
          const thrownDirection=direction(main[0]);let outboundSteps=0,recallSteps=0;
          for(let i=1;i<main.length;i++){
            const a=main[i-1],b=main[i];
            if(![b.x,b.y,b.z,b.yaw,b.pitch].every(Number.isFinite))throw new Error('Nonfinite trajectory');
            if(Math.hypot(b.x-a.x,b.y-a.y,b.z-a.z)>4)throw new Error('Hammer jumped outside its path');
            const pose=direction(b);
            if(pose.reduce((sum,v,index)=>sum+v*thrownDirection[index],0)<.99999)throw new Error('Hammer changed its thrown pose during recall');
            const dx=b.x-a.x,dy=b.y-a.y,dz=b.z-a.z,length=Math.hypot(dx,dy,dz);
            if(length>0.00001){
              const dot=(pose[0]*dx+pose[1]*dy+pose[2]*dz)/length;
              if(dot>0){
                if(dot<.9999||recallSteps)throw new Error('Outbound hammer did not lead with its head');
                outboundSteps++;
              }else{
                if(dot>-.8)throw new Error('Recall did not lead with the grip');
                recallSteps++;
              }
            }
          }
          if(!outboundSteps||!recallSteps)throw new Error('Throw and recall were not both observed');
          if(Math.max(...main.map(e=>e.tick))>26)throw new Error('Hammer persisted beyond its return');
          row.trajectory={samples:main.length,first:main[0],last:main.at(-1),outboundSteps,recallSteps,posePreservedOnRecall:true,gripFirstReturn:true,cleanup:true};
        }
      }catch(error){row.ok=false;row.error=error.message;process.exitCode=1;}
      save(row);console.log(`thor ${scenario}: ${row.ok?'PASS · '+row.frames.length+' frames':row.error}`);
    }
  }else if(mode==='directional'){
    for(const id of ['nike','athena','assasin'])for(const pitch of [-90,90]){
      const row={kind:'cast',id,label: id+' · 수직 시선 방향 검사',view:'vertical-'+pitch};
      try{
        const prepared=await fixture(`prepare ${id} enemy`);
        await action('camera','back');await action('hud','false');
        if(id==='assasin')await command(`/tp ${prepared.target} 0.5 -60 4.5 90 ${pitch}`);
        await tool('set_view_angle',{yaw:id==='assasin'?0:90,pitch:id==='assasin'?0:pitch});
        await sleep(200);row.before=await fixture('state');
        await record(row,async()=>{
          await fixture('trace');const cast=await action(id==='nike'?'attack':'use');await sleep(180);
          row.during=await fixture('state');
          await tool('set_view_angle',{yaw:-45,pitch:18});return cast;
        },2300);
        if(row.during.stones>=row.before.stones)throw new Error('Directional action was not activated');
        if(id==='nike'&&!(row.during.x<row.before.x-.3&&Math.abs(row.during.z-row.before.z)<.1))throw new Error('Vertical-camera dash lost its westward heading');
        if(id==='assasin'&&Math.hypot(row.during.x-(row.during.targetX+1),row.during.z-row.during.targetZ)>.05)throw new Error('Teleport was not behind the target\'s westward heading');
        if(id==='athena'){
          const shields=row.during.motionTrace.flatMap(f=>f.entities).filter(e=>e.art?.startsWith('newgodwar:art/athena/phalanx/glyph'));
          if(shields.length<5||shields.some(e=>Math.cos((e.yaw-90)*Math.PI/180)<.9999))throw new Error('Formation did not keep the westward heading');
        }
        await fixture('prepare invincibility enemy');
        if((await fixture('state')).displays!==0)throw new Error('Reassignment did not remove directional effects');
        row.headingPreserved=true;row.cleanup=true;
      }catch(error){row.ok=false;row.error=error.message;process.exitCode=1;}
      save(row);console.log(`${id} vertical ${pitch}: ${row.ok?'PASS':row.error}`);
    }
  }else if(mode==='push'){
    for(const id of selected.length?selected:['aeolus','poseidon','quetzalcoatl','yisunsin','wizard'])for(const pitch of [-90,90]){
      const row={kind:'cast',id,label:id+' · 수직 시선 밀쳐내기',view:'push-'+pitch};
      try{
        await fixture(`prepare ${id} enemy`);await action('camera','back');await action('hud','false');
        await tool('set_view_angle',{yaw:90,pitch});await sleep(200);row.before=await fixture('state');
        await record(row,async()=>{
          const cast=await action(['yisunsin','wizard'].includes(id)?'attack':'use');
          await sleep(180);row.during=await fixture('state');
          await tool('set_view_angle',{yaw:-45,pitch:18});return cast;
        },3100);
        if(row.during.stones>=row.before.stones)throw new Error('Push was not activated');
        const dx=row.after.targetX-row.before.targetX,dz=row.after.targetZ-row.before.targetZ;
        // Real water adds lateral drift after Poseidon's push; judge heading rather than exact final Z.
        row.displacement={x:dx,z:dz,westwardAlignment:-dx/Math.hypot(dx,dz)};
        if(!(dx<-.8&&row.displacement.westwardAlignment>.98))throw new Error('Push lost its westward heading');
        await fixture('prepare invincibility enemy');
        if((await fixture('state')).displays!==0)throw new Error('Push effects remained after reassignment');
        row.headingPreserved=true;row.cleanup=true;
      }catch(error){row.ok=false;row.error=error.message;process.exitCode=1;}
      save(row);console.log(`${id} push ${pitch}: ${row.ok?'PASS':row.error}`);
    }
  }else if(mode==='spear'){
    for(const scenario of selected.length?selected:['side','elevated','moving','near','blocked','cancel','fallback','still']){
      const row={kind:'cast',id:'odin',label:'오딘 · 궁니르 비행 · '+scenario,view:'spear-'+scenario};
      try{
        const prepared=await fixture('prepare odin enemy');
        const distance=scenario==='near'?.7:8,dy=scenario==='elevated'?3:0;
        if(dy)await command('/setblock 0 -58 8 minecraft:stone');
        await command(`/tp ${prepared.target} 0.5 ${-60+dy} ${.5+(scenario==='near'?8:distance)} 135 70`);
        await action('camera','back');await action('hud','false');
        await tool('set_view_angle',{yaw:0,pitch:-Math.atan2(dy,distance)*180/Math.PI});
        if(scenario==='still')await fixture('settings animations false');
        if(scenario==='fallback')await fixture('settings objects false');
        await sleep(250);await action('attack');await sleep(200);
        if(scenario==='near'){
          await command(`/tp ${prepared.target} 0.5 -60 ${.5+distance} 135 70`);
          await tool('set_view_angle',{yaw:-90,pitch:12});
        }
        if(scenario==='side'){
          await tool('set_view_angle',{yaw:-50,pitch:12});await sleep(200);
        }
        row.before=await fixture('state');
        if(row.before.normalCooldown<=0)throw new Error('Scout mark did not activate before the throw');
        await record(row,async()=>{
          await fixture('trace');const cast=await action('use');await sleep(150);row.during=await fixture('state');
          await tool('set_view_angle',{yaw:-50,pitch:12});
          if(scenario==='moving')for(let i=0;i<4;i++){await command(`/tp ${prepared.target} ~0.25 ~ ~`);await sleep(60);}
          if(scenario==='blocked')await command('/fill -1 -60 4 1 -56 4 minecraft:stone');
          if(scenario==='cancel')await fixture('prepare invincibility enemy');
          return cast;
        },3100);
        if(row.during.stones>=row.before.stones)throw new Error('Spear cast was not activated');
        if(row.after.displays!==0)throw new Error('Spear or tracking birds remained after expiry');
        if(scenario==='blocked'&&row.after.targetHealth<row.before.targetHealth)throw new Error('Blocked spear damaged the target');
        if(!['cancel','blocked'].includes(scenario)&&row.after.targetHealth>=row.before.targetHealth)throw new Error('Spear never hit its target');
        if(!['fallback','cancel'].includes(scenario)){
          const main=row.after.motionTrace.flatMap(f=>f.entities.filter(e=>e.art?.startsWith('newgodwar:art/shared/spear/glyph')).map(e=>({...e,tick:f.tick})));
          if(main.length<6)throw new Error('Insufficient moving spear samples');
          let steps=0;
          for(let i=1;i<main.length;i++){
            const a=main[i-1],b=main[i],dx=b.x-a.x,dy=b.y-a.y,dz=b.z-a.z,length=Math.hypot(dx,dy,dz);
            if(![b.x,b.y,b.z,b.yaw,b.pitch].every(Number.isFinite)||length>4)throw new Error('Invalid spear trajectory');
            if(length>0.00001){
              const yaw=b.yaw*Math.PI/180,pitch=b.pitch*Math.PI/180;
              const dot=(-Math.sin(yaw)*Math.cos(pitch)*dx-Math.sin(pitch)*dy+Math.cos(yaw)*Math.cos(pitch)*dz)/length;
              if(dot<.9999)throw new Error('Spear did not fly tip first');steps++;
            }
          }
          if(steps<4)throw new Error('Spear appeared at impact without travelling');
          row.trajectory={samples:main.length,steps,tipFirst:true,cleanup:true};
        }
      }catch(error){row.ok=false;row.error=error.message;process.exitCode=1;}
      save(row);console.log(`odin ${scenario}: ${row.ok?'PASS':row.error}`);
    }
  }else if(mode==='flight'){
    for(const id of ['jujak','hermes']){
      await fixture(`prepare ${id} enemy`);await action('camera','back');await action('hud','false');
      await tool('set_view_angle',{yaw:0,pitch:0});await sleep(500);
      const row={kind:'cast',id,label:catalog.abilities.find(a=>a.id===id).name+' · 실제 공중 비행',view:'airborne',before:await fixture('state')};
      try{await record(row,async()=>{
        await command('/tp @s 0.5 -56 0.5 0 0');return action('attack');
      },4000);}catch(error){row.ok=false;row.error=error.message;process.exitCode=1;}
      save(row);console.log(`${id} flight: ${row.ok?'PASS':row.error}`);
    }
  }else if(mode==='roles'){
    for(const id of ['asclepius','poseidon']){
      await fixture(`prepare ${id} ${id==='asclepius'?'ally':'enemy'}`);
      await action('camera','back');await action('hud','false');
      await tool('set_view_angle',{yaw:-25,pitch:10});await sleep(500);
      const ability=catalog.abilities.find(a=>a.id===id);
      const row={kind:'cast',id,label:ability.name+' · 실제 시전과 대상 반응',view:'recipient',before:await fixture('state')};
      try{await record(row,()=>action('use'),2600);}catch(error){row.ok=false;row.error=error.message;process.exitCode=1;}
      save(row);console.log(`${id} recipient: ${row.ok?'PASS':row.error}`);
    }
    await fixture('prepare sniper enemy');await action('hud','false');await action('camera','first');
    await tool('set_view_angle',{yaw:0,pitch:0});await sleep(500);
    for(const [view,label,duration,trigger] of [
      ['ready','저격수 · 준비 완료 조준경',5500,()=>fixture('condition normal')],
      ['shot','저격수 · 실제 강화 화살 발사',2500,()=>fixture('condition advanced')]
    ]){
      const row={kind:'cast',id:'sniper',label,view,before:await fixture('state')};
      try{await record(row,trigger,duration);}catch(error){row.ok=false;row.error=error.message;process.exitCode=1;}
      save(row);console.log(`sniper ${view}: ${row.ok?'PASS':row.error}`);
    }
    await fixture('prepare sniper enemy');await fixture('condition normal');await sleep(4300);
    await action('camera','first');await tool('set_view_angle',{yaw:90,pitch:0});
    const miss={kind:'cast',id:'sniper',label:'저격수 · 빗나간 강화 화살',view:'miss',before:await fixture('state')};
    try{await record(miss,()=>fixture('condition advanced'),2500);
      if(miss.after.targetHealth<miss.before.targetHealth)throw new Error('Miss unexpectedly damaged target');
    }catch(error){miss.ok=false;miss.error=error.message;process.exitCode=1;}
    save(miss);console.log(`sniper miss: ${miss.ok?'PASS':miss.error}`);
  }else throw new Error('Use models, outlines, casts, advanced, motion, directional, push, spear, flight or roles');
}finally{await client.close();}
