// Execute a reviewable JSON scenario through MCP and retain original framebuffer PNGs.
import { readFileSync, mkdirSync, writeFileSync, copyFileSync } from 'node:fs';
import { dirname, resolve, join } from 'node:path';
import { fileURLToPath, pathToFileURL } from 'node:url';
const root = resolve(dirname(fileURLToPath(import.meta.url)), '../..');
const sdk = join(root, '.build/minecraft-mcp/bridge/node_modules/@modelcontextprotocol/sdk/dist/esm');
const { Client } = await import(pathToFileURL(join(sdk, 'client/index.js')));
const { StdioClientTransport } = await import(pathToFileURL(join(sdk, 'client/stdio.js')));
const scenario = JSON.parse(readFileSync(process.argv[2], 'utf8'));
if (!/^[a-z0-9_-]+$/.test(scenario.name)) throw new Error('Invalid scenario name');
const folder = join(root, '.build/minecraft-mcp/evidence', scenario.name);
mkdirSync(folder, { recursive: true });
writeFileSync(join(folder, 'scenario.json'), JSON.stringify(scenario, null, 2));
const client = new Client({ name: 'newgodwar-visual-scenario', version: '1.0.0' });
const evidence = { name: scenario.name, started: new Date().toISOString(), steps: [] };
try {
  await client.connect(new StdioClientTransport({ command: process.execPath,
    args: [join(root, 'scripts/minecraft-mcp/bridge.mjs')], cwd: root }));
  for (const step of scenario.steps) {
    if (step.delay) {
      if (step.delay < 0 || step.delay > 10000) throw new Error('Delay must be 0–10000 ms');
      await new Promise(resolve => setTimeout(resolve, step.delay));
    }
    if (!step.tool && !step.capture) continue;
    const tool = step.capture ? 'screenshot' : step.tool;
    const logPath = join(root, '.build/minecraft-mcp/logs/client.log');
    const logOffset = step.expectChat ? readFileSync(logPath, 'utf8').length : 0;
    const result = await client.callTool({ name: tool, arguments: step.args ?? {} });
    const texts = result.content.filter(item => item.type === 'text').map(item => {
      try { return JSON.parse(item.text); } catch { return item.text; }
    });
    const entry = { time: new Date().toISOString(), step, result: texts, isError: !!result.isError };
    evidence.steps.push(entry);
    writeFileSync(join(folder, 'results.json'), JSON.stringify(evidence, null, 2));
    if (result.isError) throw new Error(JSON.stringify(texts));
    if (step.expectChat) {
      let observed = false;
      for (let attempt = 0; attempt < 15; attempt++) {
        observed = readFileSync(logPath, 'utf8').slice(logOffset).includes(step.expectChat);
        if (observed) break;
        await new Promise(resolve => setTimeout(resolve, 100));
      }
      entry.activationConfirmed = observed;
      writeFileSync(join(folder, 'results.json'), JSON.stringify(evidence, null, 2));
      if (!observed) throw new Error('Ability activation was not confirmed: ' + step.expectChat);
    }
    if (step.capture) {
      if (!/^[a-z0-9_-]+$/.test(step.capture)) throw new Error('Invalid capture name');
      const capture = texts.find(item => item.path);
      copyFileSync(capture.path, join(folder, step.capture + '.png'));
      console.log(`${step.capture}: ${capture.width}x${capture.height}`);
    } else console.log(tool + ': ' + JSON.stringify(texts));
  }
  evidence.completed = true;
} finally {
  evidence.finished = new Date().toISOString();
  writeFileSync(join(folder, 'results.json'), JSON.stringify(evidence, null, 2));
  await client.close();
}
