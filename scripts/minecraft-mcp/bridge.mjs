// Project-scoped adapter for minecraft-mod-mcp 0.4.1. Never starts a game.
import { readFileSync, mkdirSync, writeFileSync } from 'node:fs';
import { fileURLToPath, pathToFileURL } from 'node:url';
import { dirname, resolve, join } from 'node:path';

const root = resolve(dirname(fileURLToPath(import.meta.url)), '../..');
const runtime = join(root, '.build/minecraft-mcp');
const modules = join(runtime, 'bridge/node_modules');
const { McpServer } = await import(pathToFileURL(join(modules, '@modelcontextprotocol/sdk/dist/esm/server/mcp.js')));
const { StdioServerTransport } = await import(pathToFileURL(join(modules, '@modelcontextprotocol/sdk/dist/esm/server/stdio.js')));
const { TOOLS } = await import(pathToFileURL(join(modules, 'minecraft-mod-mcp/dist/index.js')));
const { z } = await import(pathToFileURL(join(modules, 'zod/index.js')));
const port = 19876;
const selected = new Set([
  'ping', 'get_player_info', 'get_world_info', 'get_screen_buttons',
  'enumerate_widgets', 'enter_control_mode', 'execute_command',
  'set_view_angle', 'look_delta', 'use_item',
]);

export function validateSession(session, status) {
  if (session.port !== port || session.version !== '26.3' || !Number.isInteger(session.pid) || session.pid <= 0) {
    throw new Error('Invalid test session. Start the project visual-test runner.');
  }
  if (status.type !== 'minecraft-mod' || status.pid !== session.pid || status.port !== port
      || status.version !== session.version || status.loader !== 'fabric') {
    throw new Error('Minecraft process identity does not match the isolated test client.');
  }
}

async function request(path, body) {
  const response = await fetch(`http://127.0.0.1:${port}${path}`, {
    method: body ? 'POST' : 'GET',
    headers: body ? { 'Content-Type': 'application/json' } : undefined,
    body: body ? JSON.stringify(body) : undefined,
    signal: AbortSignal.timeout(15000),
    redirect: 'error',
  });
  if (!response.ok) throw new Error(`Minecraft returned HTTP ${response.status}`);
  return response.json();
}

async function connected() {
  let session;
  try { session = JSON.parse(readFileSync(join(runtime, 'session.json'), 'utf8')); }
  catch { throw new Error('Test client is stopped. This MCP never launches a game automatically.'); }
  const status = await request('/api/status');
  validateSession(session, status);
  return status;
}

function textResult(value) {
  return { content: [{ type: 'text', text: JSON.stringify(value) }] };
}

function guarded(handler) {
  return async (params) => {
    try { return await handler(params); }
    catch (error) { return { content: [{ type: 'text', text: error.message }], isError: true }; }
  };
}

const server = new McpServer({ name: 'newgodwar-visual-test', version: '1.0.0' });
server.registerTool('record_clip', {
  description: 'Record original PNG frames from the hidden test client at up to 20 fps. No OS capture. Writes client/recordings/<name>/clip.json when finished.',
  inputSchema: {name:z.string().regex(/^[A-Za-z0-9_.-]{1,100}$/).refine(v=>v!=='.'&&v!=='..'),durationMs:z.number().int().min(1000).max(6000)},
}, guarded(async ({name,durationMs}) => {
  await connected();
  const result=await request('/api/cmd',{cmd:'execute_command',command:`/__ngw_visual record ${name} ${durationMs}`});
  return {...textResult(result),...(result?.sent===true?{}:{isError:true})};
}));
server.registerTool('test_status', {
  description: 'Check only the project test client. Never launches apps or scans other ports.', inputSchema: {},
}, guarded(async () => textResult(await connected())));

server.registerTool('client_action', {
  description: 'Minecraft 26.3 internal controls only: attack/use, first/back/front camera, HUD visibility, or ALL/MINIMAL particles. Never sends OS input.',
  inputSchema: { action: z.enum(['attack', 'use', 'camera', 'hud', 'particles', 'pack', 'respawn', 'menu_hover', 'menu_click']), value: z.string().optional() },
}, guarded(async ({ action, value }) => {
  const choices = { camera: ['first', 'back', 'front'], hud: ['true', 'false'], particles: ['ALL', 'MINIMAL'], pack: ['accept', 'reject'] };
  if (action === 'menu_hover' || action === 'menu_click') choices[action] = Array.from({length: 54}, (_, i) => String(i));
  if (choices[action] && !choices[action].includes(value)) throw new Error('Invalid value for ' + action);
  if (!choices[action] && value !== undefined) throw new Error('This action has no value.');
  await connected();
  const result = await request('/api/cmd', { cmd: 'execute_command',
    command: '/__ngw_visual ' + action + (value === undefined ? '' : ' ' + value) });
  return { ...textResult(result), ...(result?.sent === true ? {} : { isError: true }) };
}));

for (const tool of TOOLS.filter(tool => selected.has(tool.name))) {
  server.registerTool(tool.name, { description: tool.description, inputSchema: tool.inputSchema }, guarded(async (params) => {
    await connected();
    const result = await request('/api/cmd', tool.name === 'use_item'
      ? { cmd: 'execute_command', command: '/__ngw_visual use' } : { ...params, cmd: tool.name });
    if (result?.error || result?.ok === false || result?.success === false) {
      return { ...textResult(result), isError: true };
    }
    return textResult(result);
  }));
}

server.registerTool('screenshot', {
  description: 'Capture actual Minecraft pixels from the test client, save PNG evidence, and return an MCP image. Requires a running test session.',
  inputSchema: {},
}, guarded(async () => {
  await connected();
  const fresh = await request('/api/cmd', { cmd: 'execute_command', command: '/__ngw_visual fresh_frame' });
  if (fresh?.error || fresh?.sent !== true) throw new Error('Could not invalidate the upstream two-second screenshot cache.');
  const result = await request('/api/screenshot');
  const match = /^data:image\/png;base64,([A-Za-z0-9+/=]+)$/.exec(result.original ?? '');
  if (!match) throw new Error('Minecraft did not return an original PNG framebuffer capture.');
  const data = Buffer.from(match[1], 'base64');
  if (data.length < 24 || data.subarray(0, 8).toString('hex') !== '89504e470d0a1a0a') throw new Error('Invalid PNG capture.');
  const folder = join(runtime, 'captures');
  mkdirSync(folder, { recursive: true });
  const path = join(folder, `${new Date().toISOString().replace(/[:.]/g, '-')}.png`);
  writeFileSync(path, data);
  return { content: [
    { type: 'image', mimeType: 'image/png', data: match[1] },
    { type: 'text', text: JSON.stringify({ path, width: data.readUInt32BE(16), height: data.readUInt32BE(20) }) },
  ] };
}));

if (process.argv[1] && resolve(process.argv[1]) === fileURLToPath(import.meta.url)) {
  await server.connect(new StdioServerTransport());
}
