// Real resource-pack food rendering in the isolated client; never sends OS input.
import {readFileSync,writeFileSync,mkdirSync,copyFileSync} from 'node:fs';
import {resolve,join} from 'node:path';
import {pathToFileURL} from 'node:url';
const root=resolve(import.meta.dirname,'../..'),runtime=join(root,'.build/minecraft-mcp');
const out=join(runtime,'evidence',process.env.NGW_EVIDENCE??'scope-effects','foods');mkdirSync(out,{recursive:true});
const sdk=join(runtime,'bridge/node_modules/@modelcontextprotocol/sdk/dist/esm');
const {Client}=await import(pathToFileURL(join(sdk,'client/index.js')));
const {StdioClientTransport}=await import(pathToFileURL(join(sdk,'client/stdio.js')));
const client=new Client({name:'ngw-food-render-check',version:'1'}),sleep=ms=>new Promise(r=>setTimeout(r,ms));
const resultFile=join(runtime,'server/plugins/VisualRegressionProbe/last-result.json');
async function tool(name,args={}){const r=await client.callTool({name,arguments:args});if(r.isError)throw new Error(JSON.stringify(r));return JSON.parse(r.content.find(c=>c.type==='text').text);}
function result(){try{return JSON.parse(readFileSync(resultFile,'utf8'));}catch{return null;}}
async function fixture(args){const before=result()?.sequence;await tool('execute_command',{command:'/ngwvisual '+args});for(let i=0;i<100;i++){await sleep(30);const r=result();if(r&&r.sequence!==before&&r.command.join(' ')===args){if(!r.ok)throw new Error(r.error);return r;}}throw new Error('No fixture acknowledgement: '+args);}
async function screenshot(name){await sleep(350);const r=await tool('screenshot');const path=join(out,name+'.png');copyFileSync(r.path,path);return path;}
try{
  await client.connect(new StdioClientTransport({command:process.execPath,args:[join(root,'scripts/minecraft-mcp/bridge.mjs')],cwd:root}));
  await tool('enter_control_mode');
  const before=await fixture('state');if(!before.artPackLoaded)throw new Error('Food pack not acknowledged');
  await fixture('prepare siksin ally');await tool('client_action',{action:'camera',value:'first'});
  await sleep(2500);
  await tool('client_action',{action:'hud',value:'true'});await tool('set_view_angle',{yaw:0,pitch:18});
  const catalogue=await fixture('foods gallery');const gallery=await screenshot('gallery');
  const tooltips=[];
  for(let i=0;i<6;i++){await tool('client_action',{action:'menu_hover',value:String(10+i)});tooltips.push(await screenshot('tooltip-'+i));}
  const items=[];
  for(let i=0;i<6;i++){await fixture('foods '+i);items.push({...catalogue.foods[i],path:await screenshot(String(i))});}
  const report={session:JSON.parse(readFileSync(join(runtime,'session.json'),'utf8')),gallery,items,tooltips,ok:true};
  writeFileSync(join(out,'foods.json'),JSON.stringify(report,null,2));console.log(JSON.stringify({gallery,foods:items.length,artPackLoaded:true}));
}finally{await client.close();}
