// One MCP call over the same configured stdio transport; never starts a game.
import { dirname, resolve, join } from 'node:path';
import { fileURLToPath, pathToFileURL } from 'node:url';
const root = resolve(dirname(fileURLToPath(import.meta.url)), '../..');
const sdk = join(root, '.build/minecraft-mcp/bridge/node_modules/@modelcontextprotocol/sdk/dist/esm');
const { Client } = await import(pathToFileURL(join(sdk, 'client/index.js')));
const { StdioClientTransport } = await import(pathToFileURL(join(sdk, 'client/stdio.js')));
const client = new Client({ name: 'newgodwar-visual-test-call', version: '1.0.0' });
try {
  await client.connect(new StdioClientTransport({ command: process.execPath,
    args: [join(root, 'scripts/minecraft-mcp/bridge.mjs')], cwd: root }));
  const result = await client.callTool({ name: process.argv[2] ?? 'test_status', arguments: JSON.parse(process.argv[3] ?? '{}') });
  console.log(JSON.stringify({ ...result, content: result.content.map(item => item.type === 'image'
    ? { type: 'image', mimeType: item.mimeType, bytes: Buffer.from(item.data, 'base64').length } : item) }, null, 2));
  if (result.isError) process.exitCode = 1;
} finally { await client.close(); }
