// Transport and target-selection checks only; does not launch Minecraft.
import assert from 'node:assert/strict';
import { existsSync, writeFileSync } from 'node:fs';
import { fileURLToPath, pathToFileURL } from 'node:url';
import { dirname, resolve, join } from 'node:path';
import { validateSession } from './bridge.mjs';

const root = resolve(dirname(fileURLToPath(import.meta.url)), '../..');
const runtime = join(root, '.build/minecraft-mcp');
if (existsSync(join(runtime, 'session.json'))) throw new Error('Stop the visual-test runner before this offline check.');
const sdk = join(runtime, 'bridge/node_modules/@modelcontextprotocol/sdk/dist/esm');
const { Client } = await import(pathToFileURL(join(sdk, 'client/index.js')));
const { StdioClientTransport } = await import(pathToFileURL(join(sdk, 'client/stdio.js')));
const session = { pid: 12345, port: 19876, version: '26.3' };
const status = { ...session, type: 'minecraft-mod', loader: 'fabric' };
validateSession(session, status);
for (const change of [{ pid: 12346 }, { port: 9876 }, { version: '1.21.11' }, { loader: 'forge' }, { type: 'other' }]) {
  assert.throws(() => validateSession(session, { ...status, ...change }));
}

const client = new Client({ name: 'newgodwar-mcp-preflight', version: '1.0.0' });
const transport = new StdioClientTransport({
  command: process.execPath, args: [join(root, 'scripts/minecraft-mcp/bridge.mjs')], cwd: root,
});
try {
  await client.connect(transport);
  const { tools } = await client.listTools();
  const names = tools.map(tool => tool.name);
  assert.equal(names.length, 14);
  for (const name of ['test_status', 'screenshot', 'execute_command', 'set_view_angle', 'use_item', 'client_action']) assert(names.includes(name));
  for (const name of ['launch_minecraft', 'serve', 'kill_minecraft', 'paste_text', 'mouse_drag', 'scroll_at']) assert(!names.includes(name));
  for (const name of ['test_status', 'screenshot', 'execute_command']) {
    const result = await client.callTool({ name, arguments: name === 'execute_command' ? { command: 'gw status' } : {} });
    assert.equal(result.isError, true);
    assert.match(result.content[0].text, /Test client is stopped/);
  }
  const invalid = await client.callTool({ name: 'client_action', arguments: { action: 'camera', value: 'front; exit' } });
  assert.equal(invalid.isError, true);
  assert.match(invalid.content[0].text, /Invalid value/);
  const invalidClip = await client.callTool({name:'record_clip',arguments:{name:'../outside',durationMs:2000}});
  assert.equal(invalidClip.isError,true);
  const result = { status: 'passed', checks: ['MCP initialize', '14 tool schemas', 'internal action arguments validated', 'recording paths bounded', 'no launch/clipboard/cursor-drag tools',
    '5 wrong-target identities rejected', 'stopped-client calls rejected'], graphicsTested: false, tools: names };
  writeFileSync(join(runtime, 'bridge-check.json'), JSON.stringify(result, null, 2));
  console.log(JSON.stringify(result, null, 2));
} finally {
  await client.close();
}
