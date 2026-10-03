"""Prepare an isolated real Minecraft client. Default action never launches it.

python scripts/Test-ClientVisuals.py prepare --minecraft-dir <installed .minecraft> --java <java.exe>
python scripts/Test-ClientVisuals.py preflight
python scripts/Test-ClientVisuals.py run --hidden
python scripts/Test-ClientVisuals.py run --allow-client-window
"""
import argparse
import hashlib
import json
import os
import platform
from pathlib import Path
import re
import shutil
import socket
import subprocess
import time
import urllib.request
import uuid
import zipfile
import threading
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer

ROOT = Path(__file__).resolve().parents[1]
RUNTIME = ROOT / '.build/minecraft-mcp'
VERSION = '26.3'
FABRIC = '0.19.5'
MOD_NAME = 'minecraft-mcp-26.3-fabric-v0.4.1.jar'
LOCAL_MOD_NAME = 'minecraft-mcp-26.3-fabric-v0.4.1-localhost.jar'
MOD_HASH = '3c04ab2b3b7b08dd0b261acfadbc6a05cfafe00ca2172ad367bed407cb36ac8d'
MC_PORT, MCP_PORT = 25576, 19876
PLAYER = 'NGWVisualTest'
PLAYER_UUID = str(uuid.UUID(bytes=hashlib.md5(('OfflinePlayer:' + PLAYER).encode()).digest(), version=3))


def write_json(path, value):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, indent=2, ensure_ascii=False), encoding='utf-8')


def download(url, path, sha1=None):
    path.parent.mkdir(parents=True, exist_ok=True)
    if not path.exists():
        with urllib.request.urlopen(url, timeout=60) as response:
            temporary = path.with_suffix(path.suffix + '.download')
            temporary.write_bytes(response.read())
        if sha1 and hashlib.sha1(temporary.read_bytes()).hexdigest() != sha1:
            raise RuntimeError('Checksum mismatch: ' + str(path))
        temporary.replace(path)
    if sha1 and hashlib.sha1(path.read_bytes()).hexdigest() != sha1:
        raise RuntimeError('Checksum mismatch: ' + str(path))


def allowed(rules):
    if not rules:
        return True
    result = False
    for rule in rules:
        system = rule.get('os', {})
        if system.get('name', 'windows') != 'windows':
            continue
        if system.get('arch', 'x86_64') not in ('x86_64', 'amd64'):
            continue
        if 'version' in system and not re.search(system['version'], platform.version()):
            continue
        if any(value != (key == 'has_custom_resolution') for key, value in rule.get('features', {}).items()):
            continue
        result = rule['action'] == 'allow'
    return result


def expand_arguments(items, replacements):
    result = []
    for item in items:
        if isinstance(item, dict):
            if not allowed(item.get('rules')):
                continue
            item = item['value']
        for value in item if isinstance(item, list) else [item]:
            for key, replacement in replacements.items():
                value = value.replace('${' + key + '}', str(replacement))
            if '${' in value:
                raise RuntimeError('Unresolved launch argument: ' + value)
            result.append(value)
    return result


def restrict_mod_to_loopback(original, output):
    """Patch only the verified upstream class's UTF-8 bind-address constant.

    The v0.4.1 README says loopback, but McpHttpServer binds 0.0.0.0.
    Keep all bytecode instructions and other archive entries unchanged.
    """
    if hashlib.sha256(original.read_bytes()).hexdigest() != MOD_HASH:
        raise RuntimeError('Minecraft MCP release checksum mismatch')
    target = 'xyz/langyo/minecraft/mcp/common/McpHttpServer.class'
    with zipfile.ZipFile(original) as archive:
        data = archive.read(target)
        if data[:4] != bytes.fromhex('cafebabe'):
            raise RuntimeError('Unexpected class format')
        position, index, matches = 10, 1, []
        count = int.from_bytes(data[8:10], 'big')
        sizes = {3: 4, 4: 4, 5: 8, 6: 8, 7: 2, 8: 2, 9: 4, 10: 4,
                 11: 4, 12: 4, 15: 3, 16: 2, 17: 4, 18: 4, 19: 2, 20: 2}
        while index < count:
            tag = data[position]
            start = position
            position += 1
            if tag == 1:
                length = int.from_bytes(data[position:position + 2], 'big')
                value = data[position + 2:position + 2 + length]
                position += 2 + length
                if value == b'0.0.0.0':
                    matches.append((start, position))
            else:
                position += sizes[tag]
                if tag in (5, 6):
                    index += 1
            index += 1
        if len(matches) != 1:
            raise RuntimeError('Upstream bind-address constant changed; review the new mod before patching.')
        start, end = matches[0]
        patched = data[:start] + b'\x01\x00\x09127.0.0.1' + data[end:]
        output.parent.mkdir(parents=True, exist_ok=True)
        with zipfile.ZipFile(output, 'w') as destination:
            for entry in archive.infolist():
                destination.writestr(entry, patched if entry.filename == target else archive.read(entry.filename))
    with zipfile.ZipFile(original) as before, zipfile.ZipFile(output) as after:
        assert before.namelist() == after.namelist()
        assert after.testzip() is None
        changed = [name for name in before.namelist() if before.read(name) != after.read(name)]
        if changed != [target]:
            raise RuntimeError('Unexpected patched archive differences')
    return hashlib.sha256(output.read_bytes()).hexdigest()


def prepare(args):
    if (RUNTIME / 'session.json').exists():
        raise RuntimeError('Stop the existing visual-test runner before preparing files.')
    if os.name != 'nt':
        raise RuntimeError('This prepared profile targets Windows x64.')
    source = Path(args.minecraft_dir).resolve()
    java = Path(args.java).resolve()
    if not java.is_file():
        raise RuntimeError('Java executable not found')
    base = json.loads((source / f'versions/{VERSION}/{VERSION}.json').read_text(encoding='utf-8'))
    client = RUNTIME / 'client'
    client.mkdir(parents=True, exist_ok=True)
    profile_path = RUNTIME / 'fabric-profile.json'
    download(f'https://meta.fabricmc.net/v2/versions/loader/{VERSION}/{FABRIC}/profile/json', profile_path)
    fabric = json.loads(profile_path.read_text(encoding='utf-8'))
    classpath, required = [], []
    for library in fabric['libraries'] + base['libraries']:
        if not allowed(library.get('rules')):
            continue
        artifact = library.get('downloads', {}).get('artifact')
        if artifact:
            relative = artifact['path']
            origin = source / 'libraries' / relative
            if origin.is_file() and (not artifact.get('sha1') or hashlib.sha1(origin.read_bytes()).hexdigest() == artifact['sha1']):
                path = origin  # Read existing game libraries; never modify the personal installation.
            else:
                path = RUNTIME / 'libraries' / relative
                download(artifact['url'], path, artifact.get('sha1'))
        else:
            group, name, version = library['name'].split(':')
            relative = f"{group.replace('.', '/')}/{name}/{version}/{name}-{version}.jar"
            path = RUNTIME / 'libraries' / relative
            base_url = library.get('url', 'https://maven.fabricmc.net/')
            url = base_url + relative
            hash_path = path.with_suffix('.jar.sha1')
            download(url + '.sha1', hash_path)
            download(url, path, hash_path.read_text().strip().split()[0])
        if str(path) not in classpath:
            classpath.append(str(path))
            required.append(str(path))
    jar = source / f'versions/{VERSION}/{VERSION}.jar'
    if hashlib.sha1(jar.read_bytes()).hexdigest() != base['downloads']['client']['sha1']:
        raise RuntimeError('Installed Minecraft client checksum mismatch')
    classpath.append(str(jar))
    required.append(str(jar))
    asset_index = source / f"assets/indexes/{base['assetIndex']['id']}.json"
    assets = json.loads(asset_index.read_text(encoding='utf-8'))
    missing_assets = [item['hash'] for item in assets['objects'].values()
                      if not (source / 'assets/objects' / item['hash'][:2] / item['hash']).is_file()]
    if missing_assets:
        raise RuntimeError(f'Installed game is missing {len(missing_assets)} assets; repair it before testing.')
    original_mod = RUNTIME / 'downloads' / MOD_NAME
    download(f'https://github.com/langyo/minecraft-mod-mcp/releases/download/v0.4.1/{MOD_NAME}', original_mod)
    mod = client / 'mods' / LOCAL_MOD_NAME
    local_hash = restrict_mod_to_loopback(original_mod, mod)
    # Remove only the upstream copy staged by the initial setup, never other mods.
    unpatched = client / 'mods' / MOD_NAME
    if unpatched.exists():
        if hashlib.sha256(unpatched.read_bytes()).hexdigest() != MOD_HASH:
            raise RuntimeError('Unexpected unpatched mod in test folder; preserve it for review.')
        unpatched.unlink()
    with zipfile.ZipFile(mod) as archive:
        metadata = json.loads(archive.read('fabric.mod.json'))
        if metadata['id'] != 'mcpmod' or metadata['version'] != '0.4.1':
            raise RuntimeError('Unexpected Minecraft MCP mod metadata')
    replacements = dict(
        natives_directory=client / 'natives', launcher_name='NewGodWarVisualTest', launcher_version='1',
        classpath=';'.join(classpath), auth_player_name=PLAYER, version_name=fabric['id'],
        game_directory=client, assets_root=source / 'assets', assets_index_name=base['assetIndex']['id'],
        auth_uuid=PLAYER_UUID, auth_access_token='0', clientid='0', auth_xuid='0', version_type='release',
        resolution_width=960, resolution_height=540,
    )
    jvm = expand_arguments(base['arguments']['jvm'] + fabric.get('arguments', {}).get('jvm', []), replacements)
    game = expand_arguments(base['arguments']['game'], replacements)
    command = [str(java), '-Xms512M', '-Xmx2G', '-Dmcp.port=' + str(MCP_PORT),
               '-Dmcp.mod.version=' + VERSION, '-Dmcp.mod.loader=fabric', *jvm,
               fabric['mainClass'], *game, '--quickPlayMultiplayer', f'127.0.0.1:{MC_PORT}']
    options = client / 'options.txt'
    if not options.exists():
        options.write_text('lang:ko_kr\nfullscreen:false\nmaxFps:30\nrenderDistance:6\n'
                           'simulationDistance:5\npauseOnLostFocus:false\nsoundCategory_master:0.0\n'
                           'particles:0\nentityDistanceScaling:1.0\n', encoding='utf-8')
    cached = ROOT / '.paper-smoke' / VERSION
    server = RUNTIME / 'server'
    server.mkdir(exist_ok=True)
    eula = cached / 'eula.txt'
    if not re.search(r'^eula=true\s*$', eula.read_text(), re.M):
        raise RuntimeError('Cached server must already have an accepted EULA.')
    paper = next(cached.glob('paper-*.jar'))
    for name in ('libraries', 'versions', 'cache'):
        if (cached / name).exists() and not (server / name).exists():
            shutil.copytree(cached / name, server / name)
    shutil.copy2(eula, server / 'eula.txt')
    plugins = server / 'plugins'
    plugins.mkdir(exist_ok=True)
    version = re.search(r'version\s*=\s*"([^"]+)"', (ROOT / 'build.gradle').read_text()).group(1)
    plugin = ROOT / 'build/libs' / f'NewGodWar-{version}.jar'
    shutil.copy2(plugin, plugins / 'NewGodWar.jar')
    (plugins / 'NewGodWar').mkdir(exist_ok=True)
    (plugins / 'NewGodWar/config.yml').write_text(
        'updates:\n  enabled: false\nworld:\n  reset-game-world-on-stop: false\n'
        'game:\n  killtime-seconds: 0\n', encoding='utf-8')
    (server / 'server.properties').write_text(
        f'server-ip=127.0.0.1\nserver-port={MC_PORT}\nonline-mode=false\nenforce-secure-profile=false\n'
        'view-distance=6\nsimulation-distance=5\nspawn-protection=0\ngenerate-structures=false\n'
        'level-type=minecraft:flat\ngamemode=creative\nmax-players=2\nwhite-list=true\n'
        'generator-settings={"biome":"minecraft:plains","layers":[{"block":"minecraft:bedrock","height":1},{"block":"minecraft:dirt","height":2},{"block":"minecraft:grass_block","height":1}]}\n', encoding='utf-8')
    write_json(server / 'ops.json', [dict(uuid=PLAYER_UUID, name=PLAYER, level=4, bypassesPlayerLimit=True)])
    write_json(server / 'whitelist.json', [dict(uuid=PLAYER_UUID, name=PLAYER)])
    write_json(RUNTIME / 'launch.json', dict(
        version=VERSION, fabric=FABRIC, mod=LOCAL_MOD_NAME, modSha256=local_hash,
        upstreamModSha256=MOD_HASH, client=command, clientDir=str(client),
        server=[str(java), '-Xms256M', '-Xmx1G', '-jar', str(paper), 'nogui'], serverDir=str(server),
        required=required + [str(java), str(mod), str(paper), str(plugins / 'NewGodWar.jar'), str(asset_index)],
        assetsChecked=len(assets['objects']), player=PLAYER, port=MCP_PORT,
    ))
    preflight()


def preflight():
    plan = json.loads((RUNTIME / 'launch.json').read_text(encoding='utf-8'))
    missing = [path for path in plan['required'] if not Path(path).is_file()]
    if missing:
        raise RuntimeError('Missing dependencies: ' + ', '.join(missing))
    if hashlib.sha256((RUNTIME / 'client/mods' / LOCAL_MOD_NAME).read_bytes()).hexdigest() != plan['modSha256']:
        raise RuntimeError('Minecraft MCP mod checksum mismatch')
    print(json.dumps(dict(status='prepared-not-launched', version=plan['version'], fabric=plan['fabric'],
                          filesChecked=len(plan['required']), assetsChecked=plan['assetsChecked'],
                          serverPort=MC_PORT, mcpPort=MCP_PORT, graphicsTested=False), indent=2))
    return plan


def hidden_command(plan):
    command = list(plan['client'])
    index = command.index('-cp') + 1
    paths = command[index].split(';')
    source = next(Path(path) for path in paths if re.fullmatch(r'lwjgl-sdl-[\d.]+\.jar', Path(path).name))
    helper = ROOT / 'scripts/minecraft-mcp/PrepareHiddenClient.java'
    classes = RUNTIME / 'hidden-classes'
    classes.mkdir(exist_ok=True)
    target = RUNTIME / 'libraries/lwjgl-sdl-hidden.jar'
    flags = subprocess.CREATE_NO_WINDOW if os.name == 'nt' else 0
    def checked(args):
        result = subprocess.run(args, capture_output=True, text=True, timeout=45, creationflags=flags)
        if result.returncode:
            raise RuntimeError(result.stdout + result.stderr)
        print(result.stdout.strip(), flush=True)
    checked([str(Path(command[0]).with_name('javac.exe')), '-cp', command[index], '-d', str(classes), str(helper)])
    checked([command[0], '-cp', str(classes) + ';' + command[index], 'PrepareHiddenClient', str(source), str(target)])
    command[index] = ';'.join(str(target) if Path(path) == source else path for path in paths)
    # The helper creates an SDL window only through the already-patched library.
    checked([command[0], '--enable-native-access=ALL-UNNAMED', '-cp', str(classes) + ';' + command[index],
             'PrepareHiddenClient', 'verify'])
    write_json(RUNTIME / 'hidden-check.json', dict(status='passed', source=str(source),
        hiddenLibrary=str(target), sha256=hashlib.sha256(target.read_bytes()).hexdigest(), graphicsTested=False))
    return command


def prepare_client_actions(plan):
    command = plan['client']
    classpath = command[command.index('-cp') + 1]
    classes = RUNTIME / 'action-classes'
    classes.mkdir(exist_ok=True)
    local = RUNTIME / 'client/mods' / LOCAL_MOD_NAME
    intermediate = RUNTIME / 'downloads/minecraft-mcp-loopback.jar'
    restrict_mod_to_loopback(RUNTIME / 'downloads' / MOD_NAME, intermediate)
    flags = subprocess.CREATE_NO_WINDOW if os.name == 'nt' else 0
    for args in ([str(Path(command[0]).with_name('javac.exe')), '-cp', classpath, '-d', str(classes),
                  str(ROOT / 'scripts/minecraft-mcp/PrepareClientActions.java'),
                  str(ROOT / 'scripts/minecraft-mcp/TestClientActions.java')],
                 [command[0], '-cp', str(classes) + ';' + classpath, 'PrepareClientActions',
                  str(intermediate), str(local), str(classes)]):
        result = subprocess.run(args, capture_output=True, text=True, timeout=45, creationflags=flags)
        if result.returncode:
            raise RuntimeError(result.stdout + result.stderr)
        if result.stdout.strip():
            print(result.stdout.strip(), flush=True)
    plan['modSha256'] = hashlib.sha256(local.read_bytes()).hexdigest()
    write_json(RUNTIME / 'launch.json', plan)


def run(args):
    if not args.allow_client_window and not args.hidden:
        raise RuntimeError('Client startup can open a window. Use --allow-client-window only when it will not interrupt the user.')
    if (RUNTIME / 'session.json').exists():
        raise RuntimeError('An existing test session is recorded; do not launch a second client.')
    plan = preflight()
    client_command = hidden_command(plan) if args.hidden else plan['client']
    prepare_client_actions(plan)
    plugin_version = re.search(r'version\s*=\s*"([^"]+)"', (ROOT / 'build.gradle').read_text()).group(1)
    built_plugin = ROOT / 'build/libs' / f'NewGodWar-{plugin_version}.jar'
    deployed_plugin = Path(plan['serverDir']) / 'plugins/NewGodWar.jar'
    shutil.copy2(built_plugin, deployed_plugin)
    visual_probe = Path(plan['serverDir']) / 'plugins/VisualRegressionProbe.jar'
    if args.visual_probe:
        shutil.copy2(ROOT / 'plugin/build/visual-regression/VisualRegressionProbe.jar', visual_probe)
    else:
        visual_probe.unlink(missing_ok=True)
    stop_request = RUNTIME / 'stop-request'
    stop_request.unlink(missing_ok=True)
    for port in (MC_PORT, MCP_PORT):
        with socket.socket() as probe:
            probe.bind(('127.0.0.1', port))
    logs = RUNTIME / 'logs'
    logs.mkdir(exist_ok=True)
    flags = subprocess.CREATE_NO_WINDOW if os.name == 'nt' else 0
    client, server = None, None
    pack_server = None
    packs = sorted((ROOT / 'build/effect-pack/dist').glob('NewGodWar-Art-*.zip')) if args.art_pack else []
    if args.art_pack and len(packs) != 1:
        raise SystemExit('Build the combined art pack first: gradlew resourcePack')
    pack_path = packs[0] if packs else None
    config_path = Path(plan['serverDir']) / 'plugins/NewGodWar/config.yml'
    old_config = config_path.read_bytes()
    if args.art_pack:
        pack_bytes = pack_path.read_bytes()
        class PackHandler(BaseHTTPRequestHandler):
            def do_GET(self):
                if self.path != '/' + pack_path.name:
                    self.send_error(404)
                    return
                self.send_response(200)
                self.send_header('Content-Type', 'application/zip')
                self.send_header('Content-Length', str(len(pack_bytes)))
                self.end_headers()
                self.wfile.write(pack_bytes)
            def log_message(self, *args):
                pass
        pack_server = ThreadingHTTPServer(('127.0.0.1', 19877), PackHandler)
        threading.Thread(target=pack_server.serve_forever, daemon=True).start()
        config_path.write_text('updates:\n  enabled: false\nworld:\n  reset-game-world-on-stop: false\n'
            'game:\n  killtime-seconds: 0\nabilities:\n  effects:\n    resource-pack:\n'
            f"      url: 'http://127.0.0.1:19877/{pack_path.name}'\n"
            f"      sha1: '{hashlib.sha1(pack_bytes).hexdigest()}'\n", encoding='utf-8')
    try:
        with (logs / 'server.log').open('w', encoding='utf-8') as output:
            server = subprocess.Popen(plan['server'], cwd=plan['serverDir'], stdin=subprocess.PIPE,
                                      stdout=output, stderr=subprocess.STDOUT, text=True, creationflags=flags)
        ready = False
        for _ in range(180):
            if server.poll() is not None:
                raise RuntimeError('Test server exited; inspect .build/minecraft-mcp/logs/server.log')
            if 'Done (' in (logs / 'server.log').read_text(encoding='utf-8', errors='replace'):
                ready = True
                break
            time.sleep(1)
        if not ready:
            raise RuntimeError('Test server readiness timed out')
        with (logs / 'client.log').open('w', encoding='utf-8') as output:
            client = subprocess.Popen(client_command, cwd=plan['clientDir'], stdout=output,
                                      stderr=subprocess.STDOUT, creationflags=flags)
        write_json(RUNTIME / 'session.json', dict(pid=client.pid, serverPid=server.pid,
            port=MCP_PORT, version=VERSION, hidden=args.hidden,
            pluginSha256=hashlib.sha256(deployed_plugin.read_bytes()).hexdigest(),
            artPackSha1=hashlib.sha1(pack_bytes).hexdigest() if args.art_pack else None))
        print('Test client started. MCP port: 19876. Stop this runner with Ctrl+C.', flush=True)
        while client.poll() is None and server.poll() is None and not stop_request.exists():
            time.sleep(1)
    finally:
        if pack_server:
            pack_server.shutdown()
            pack_server.server_close()
            config_path.write_bytes(old_config)
        (RUNTIME / 'session.json').unlink(missing_ok=True)
        stop_request.unlink(missing_ok=True)
        try:
            if client and client.poll() is None:
                client.terminate()
                try:
                    client.wait(timeout=20)
                except subprocess.TimeoutExpired:
                    client.kill()
                    client.wait(timeout=10)
        finally:
            if server and server.poll() is None:
                try:
                    server.stdin.write('stop\n')
                    server.stdin.flush()
                except (BrokenPipeError, OSError):
                    pass
                try:
                    server.wait(timeout=40)
                except subprocess.TimeoutExpired:
                    server.kill()
                    server.wait(timeout=10)


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('action', choices=['prepare', 'preflight', 'run', 'stop'], nargs='?', default='preflight')
    parser.add_argument('--minecraft-dir', default=str(Path(os.environ.get('APPDATA', '.')) / '.minecraft'))
    parser.add_argument('--java', default=shutil.which('java') or 'java')
    parser.add_argument('--allow-client-window', action='store_true')
    parser.add_argument('--hidden', action='store_true', help='Verify and use a hidden, non-focusable SDL test window.')
    parser.add_argument('--visual-probe', action='store_true', help='Install the separately built local visual regression fixture.')
    parser.add_argument('--art-pack', action='store_true', help='Offer the built effect pack from an isolated loopback-only HTTP server.')
    arguments = parser.parse_args()
    if arguments.action == 'prepare':
        prepare(arguments)
    elif arguments.action == 'stop':
        if (RUNTIME / 'session.json').exists():
            (RUNTIME / 'stop-request').touch()
            print('Requested shutdown of the isolated visual-test runner.')
        else:
            print('No visual-test session is running.')
    elif arguments.action == 'run':
        run(arguments)
    else:
        preflight()
