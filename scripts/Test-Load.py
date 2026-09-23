"""Real TCP load clients for cached Paper 26.3; no third-party Python dependencies.

Build: gradlew :plugin:loadRegressionJar build
Run: python scripts/Test-Load.py --players 16,32,64 --seconds 60
Only creates disposable localhost/offline servers. Packet IDs come from the server.
The minimal clients acknowledge login, configuration, chunks, teleports and keepalives;
they send movement, staff interactions and commands, without rendering the world.
"""
import argparse
import asyncio
import hashlib
import json
import math
import os
from pathlib import Path
import re
import shutil
import socket
import struct
import sys
import tempfile
import time
from types import SimpleNamespace
import zlib

ROOT = Path(__file__).resolve().parents[1]


def vi(value):
    out = bytearray()
    value &= 0xffffffff
    while value > 127:
        out.append((value & 127) | 128)
        value >>= 7
    out.append(value)
    return bytes(out)


def readvi(data, offset=0):
    value = 0
    for n in range(5):
        byte = data[offset + n]
        value |= (byte & 127) << (7 * n)
        if byte < 128:
            return value, offset + n + 1
    raise ValueError("Invalid VarInt")


async def streamvi(reader):
    value = 0
    for n in range(5):
        byte = (await reader.readexactly(1))[0]
        value |= (byte & 127) << (7 * n)
        if byte < 128:
            return value
    raise ValueError("Invalid frame length")


def string(text):
    data = text.encode()
    return vi(len(data)) + data


def props(path):
    return dict(line.split("=", 1) for line in path.read_text().splitlines() if line and not line.startswith("#"))


def percentile(values, fraction):
    values = sorted(values)
    return values[max(0, math.ceil(len(values) * fraction) - 1)] if values else None


class Client:
    def __init__(self, test, index):
        self.test, self.index = test, index
        self.name = f"Load{index:03}"
        self.state, self.threshold = "login", -1
        self.ready = asyncio.Event()
        self.intentional = False
        self.position = None
        self.entity_id = None
        self.origin = None
        self.frames = self.received = self.sent = 0
        self.sequence = 0
        self.pings = {}

    async def connect(self):
        self.reader, self.writer = await asyncio.open_connection("127.0.0.1", self.test.port)
        self.send_raw(0, vi(self.test.protocol_version) + string("127.0.0.1") + struct.pack(">H", self.test.port) + vi(2))
        uuid = bytearray(hashlib.md5(("OfflinePlayer:" + self.name).encode()).digest())
        uuid[6] = (uuid[6] & 15) | 48
        uuid[8] = (uuid[8] & 63) | 128
        self.send("hello", string(self.name) + uuid)
        self.read_task = asyncio.create_task(self.receive())
        await asyncio.wait_for(self.ready.wait(), 40)
        if self.test.errors:
            raise RuntimeError(self.test.errors[-1])
        self.move_task = asyncio.create_task(self.activity())

    def send_raw(self, packet, body=b""):
        data = vi(packet) + body
        if self.threshold >= 0:
            data = vi(len(data)) + zlib.compress(data) if len(data) >= self.threshold else b"\x00" + data
        wire = vi(len(data)) + data
        self.writer.write(wire)
        self.sent += len(wire)

    def send(self, name, body=b""):
        self.send_raw(self.test.ids[self.state + ".serverbound." + name], body)

    async def receive(self):
        try:
            while True:
                length = await streamvi(self.reader)
                if length > 8388608:
                    raise ValueError("Oversized packet")
                data = await self.reader.readexactly(length)
                self.received += length
                if self.threshold >= 0:
                    expanded, offset = readvi(data)
                    data = zlib.decompress(data[offset:]) if expanded else data[offset:]
                packet, offset = readvi(data)
                body = data[offset:]
                name = self.test.reverse.get((self.state, packet), "unknown")
                self.frames += 1
                if name in ("disconnect", "login_disconnect"):
                    raise RuntimeError("Server disconnect: " + repr(body[:600]))
                if self.state == "login":
                    if name == "login_compression":
                        self.threshold = readvi(body)[0]
                    elif name == "login_finished":
                        self.send("login_acknowledged")
                        self.state = "configuration"
                elif self.state == "configuration":
                    if name == "select_known_packs":
                        self.send("select_known_packs", vi(0))
                    elif name == "finish_configuration":
                        self.send("finish_configuration")
                        self.state = "game"
                    elif name == "keep_alive":
                        self.send("keep_alive", body)
                    elif name == "ping":
                        self.send("pong", body)
                elif self.state == "game":
                    if name == "login":
                        self.entity_id = struct.unpack_from(">i", body)[0]
                    elif name == "keep_alive":
                        self.send("keep_alive", body)
                    elif name == "ping":
                        self.send("pong", body)
                    elif name == "chunk_batch_finished":
                        self.send("chunk_batch_received", struct.pack(">f", 20))
                    elif name == "player_position":
                        teleport, pos = readvi(body)
                        x, y, z, dx, dy, dz, yaw, pitch, flags = struct.unpack_from(">ddddddffi", body, pos)
                        if self.position:
                            x += self.position[0] if flags & 1 else 0
                            y += self.position[1] if flags & 2 else 0
                            z += self.position[2] if flags & 4 else 0
                        self.position = (x, y, z, yaw, pitch)
                        self.origin = (x, y, z)
                        self.send("accept_teleportation", vi(teleport) + struct.pack(">dddff", x, y, z, yaw, pitch))
                        self.send("player_loaded")
                        self.ready.set()
                    elif name == "set_health" and struct.unpack_from(">f", body)[0] <= 0:
                        self.send("client_command", vi(0))
                    elif name == "system_chat":
                        for marker, server_time in re.findall(rb"LOADPONG:(\d+):(\d+)", body):
                            start = self.pings.pop(marker.decode(), None)
                            if start is not None and self.test.measuring:
                                self.test.rtts.append((time.perf_counter() - start[0]) * 1000)
                                self.test.upstream.append(int(server_time) - start[1] * 1000)
                                self.test.downstream.append(time.time() * 1000 - int(server_time))
                await self.writer.drain()
                if self.frames % 100 == 0:
                    await asyncio.sleep(0)
        except asyncio.CancelledError:
            pass
        except Exception as error:
            if not self.intentional:
                self.test.errors.append(f"{self.name}/{self.state}: {error}")
                print("CLIENT ERROR", self.test.errors[-1], file=sys.stderr, flush=True)
                self.ready.set()

    async def activity(self):
        tick = 0
        try:
            while True:
                if self.position and self.state == "game":
                    x, y, z, yaw, pitch = self.position
                    if self.test.active and tick % 2 == 0:
                        x = self.origin[0] + math.sin(tick * .07 + self.index) * .2
                        z = self.origin[2] + math.cos(tick * .07 + self.index) * .2
                        yaw = -90 if self.index % 2 == 0 else 90
                        self.position = (x, y, z, yaw, 0)
                        self.send("move_player_pos_rot", struct.pack(">dddffB", x, y, z, yaw, 0, 1))
                    if self.test.active and tick % 40 == self.index % 40:
                        # A swing directly at a nearby entity is a melee hit, not a
                        # Bukkit air interaction. Aim above it for staff activation.
                        self.send("move_player_rot", struct.pack(">ffB", yaw, -20, 1))
                        self.send("punch")
                    if self.test.active and tick % 80 == (self.index + 10) % 80:
                        target = self.test.clients.get(self.index ^ 1)
                        if target and target.entity_id is not None:
                            self.send("attack", vi(target.entity_id))
                    if self.test.active and tick % 40 == (self.index + 20) % 40:
                        self.sequence += 1
                        self.send("use_item", vi(0) + vi(self.sequence) + struct.pack(">ff", yaw, 0))
                    if tick % 100 == self.index % 100:
                        marker = str(time.perf_counter_ns())
                        self.pings[marker] = (time.perf_counter(), time.time())
                        self.send("chat_command", string("loadping " + marker))
                        if self.test.active:
                            self.send("chat_command", string("gw help"))
                    self.send("client_tick_end")
                    await self.writer.drain()
                tick += 1
                await asyncio.sleep(.05)
        except asyncio.CancelledError:
            pass
        except Exception as error:
            if not self.intentional:
                self.test.errors.append(f"{self.name}/activity: {error}")

    async def close(self):
        self.intentional = True
        for name in ("read_task", "move_task"):
            task = getattr(self, name, None)
            if task:
                task.cancel()
        if hasattr(self, "writer"):
            self.writer.close()
            try:
                await self.writer.wait_closed()
            except (OSError, ConnectionError):
                pass


async def worker_main(path):
    settings = json.loads(Path(path).read_text())
    state = SimpleNamespace(port=settings["port"], protocol_version=settings["protocol_version"], ids=settings["ids"],
        reverse={(key.split(".")[0], value): key.split(".")[-1] for key, value in settings["ids"].items() if ".clientbound." in key},
        clients={}, errors=[], rtts=[], upstream=[], downstream=[], active=False, measuring=False)
    while line := await asyncio.to_thread(sys.stdin.readline):
        request = json.loads(line)
        action = request["action"]
        response = {}
        try:
            if action == "connect":
                pending = []
                for index in request["indices"]:
                    client = Client(state, index)
                    state.clients[index] = client
                    pending.append(asyncio.create_task(client.connect()))
                    await asyncio.sleep(.1)
                await asyncio.gather(*pending)
            elif action == "begin":
                state.rtts = []
                state.upstream = []; state.downstream = []
                state.cpu_start = time.process_time(); state.wall_start = time.perf_counter()
                state.measuring = state.active = True
            elif action == "end":
                state.measuring = state.active = False
                response["rtts"] = state.rtts
                response["upstream"] = state.upstream; response["downstream"] = state.downstream
                response["cpu_percent_one_core"] = (time.process_time() - state.cpu_start) / (time.perf_counter() - state.wall_start) * 100
                response["received_bytes"] = sum(client.received for client in state.clients.values())
            elif action == "close":
                state.active = False
                await asyncio.gather(*(state.clients[index].close() for index in request["indices"]))
                for index in request["indices"]:
                    del state.clients[index]
            elif action == "quit":
                await asyncio.gather(*(client.close() for client in state.clients.values()))
            response["errors"] = state.errors
        except Exception as error:
            response["errors"] = state.errors + [repr(error)]
        print(json.dumps(response), flush=True)
        if action == "quit":
            break


class Worker:
    def __init__(self, process):
        self.process = process

    async def call(self, action, **fields):
        self.process.stdin.write((json.dumps(dict(action=action, **fields)) + "\n").encode())
        await self.process.stdin.drain()
        line = await asyncio.wait_for(self.process.stdout.readline(), 60)
        if not line:
            raise RuntimeError("Load client worker exited")
        return json.loads(line)


class Test:
    def __init__(self, args):
        self.args = args
        self.clients = {}
        self.workers = {}
        self.errors, self.lines, self.rtts, self.results = [], [], [], []
        self.server_errors = []
        self.active = self.measuring = False
        self.lifecycle_results = []
        self.log_event = asyncio.Event()

    async def command(self, command, marker=None, timeout=45):
        start = len(self.lines)
        self.process.stdin.write((command + "\n").encode())
        await self.process.stdin.drain()
        if marker:
            await self.wait_marker(marker, start, timeout)

    async def wait_marker(self, marker, start=0, timeout=120):
        deadline = time.monotonic() + timeout
        while time.monotonic() < deadline:
            if any("LOAD FAILED" in line for line in self.lines):
                raise RuntimeError("Server-side load check failed")
            if any(marker in line for line in self.lines[start:]):
                return
            if self.process.returncode is not None:
                raise RuntimeError("Server exited")
            self.log_event.clear()
            try:
                await asyncio.wait_for(self.log_event.wait(), 1)
            except asyncio.TimeoutError:
                pass
        raise TimeoutError(marker)

    async def read_logs(self):
        while line := await self.process.stdout.readline():
            text = line.decode("utf-8", "replace")
            self.lines.append(text)
            if any(marker in text for marker in ("Could not pass event", "generated an exception", "LOAD FAILED",
                    "Error occurred while enabling NewGodWar", "Error occurred while disabling NewGodWar",
                    "Could not save game session", "Game recovery failed", "Ability cleanup failed",
                    "Could not save game world/player checkpoint")):
                self.server_errors.append(text.strip())
            if any(word in text for word in ("LOAD ", "Can't keep up", "Done (", "lost connection")):
                print(text.rstrip(), flush=True)
            self.log_event.set()

    async def connect_to(self, count):
        batch = self.args.clients_per_worker
        for group in range(math.ceil(count / batch)):
            if group not in self.workers:
                process = await asyncio.create_subprocess_exec(sys.executable, str(Path(__file__).resolve()), "--worker", str(self.target / "load-clients.json"),
                    stdin=asyncio.subprocess.PIPE, stdout=asyncio.subprocess.PIPE,
                    creationflags=0x08000000 if os.name == "nt" else 0)
                self.workers[group] = Worker(process)
            indices = [index for index in range(group * batch, min(count, (group + 1) * batch)) if index not in self.clients]
            if indices:
                result = await self.workers[group].call("connect", indices=indices)
                if result["errors"]:
                    raise RuntimeError(result["errors"][0])
                self.clients.update({index: None for index in indices})
        await self.command(f"loadprobe online {count}", f"LOAD ONLINE {count}")

    async def workers_call(self, action):
        results = await asyncio.gather(*(worker.call(action) for worker in self.workers.values()))
        for result in results:
            self.errors.extend(error for error in result["errors"] if error not in self.errors)
        return results

    async def measure(self, phase, duration):
        self.rtts = []
        await self.command("loadprobe begin " + phase, "LOAD MEASURE " + phase)
        self.measuring = True
        self.active = phase != "idle"
        if self.active:
            await self.workers_call("begin")
        for offset in range(0, duration, 15):
            await asyncio.sleep(min(15, duration - offset))
            await self.workers_call("status")
            print(f"LOAD PROGRESS {phase} {min(offset + 15, duration)}/{duration}s clients={len(self.clients)} errors={len(self.errors)}", flush=True)
            if self.errors:
                raise RuntimeError(self.errors[0])
            if self.server_errors:
                raise RuntimeError(self.server_errors[0])
        self.measuring = False
        upstream, downstream, worker_cpu = [], [], []
        for result in await self.workers_call("end"):
            self.rtts.extend(result.get("rtts", []))
            upstream.extend(result.get("upstream", [])); downstream.extend(result.get("downstream", []))
            worker_cpu.append(result["cpu_percent_one_core"])
        await self.command("loadprobe end", "LOAD RESULT " + phase)
        result = props(self.target / ("load-result-" + phase + ".properties"))
        result.update(rtt_samples=len(self.rtts), rtt_p95_ms=percentile(self.rtts, .95), rtt_max_ms=percentile(self.rtts, 1))
        result.update(upstream_p95_ms=percentile(upstream, .95), downstream_p95_ms=percentile(downstream, .95), worker_cpu_percent=worker_cpu)
        self.results.append(result)

    async def lifecycle(self, count):
        started = time.perf_counter()
        self.active = False
        left = list(range(count // 2))
        await asyncio.gather(*(worker.call("close", indices=[index for index in left if index // self.args.clients_per_worker == group])
            for group, worker in self.workers.items()))
        for index in left:
            del self.clients[index]
        await asyncio.sleep(2)
        await self.command(f"loadprobe online {count - len(left)}", f"LOAD ONLINE {count - len(left)}")
        await self.command("loadprobe stop", "LOAD STOP CLEAN")
        await self.connect_to(count)
        await asyncio.sleep(3)
        await self.command(f"loadprobe verify {count}", f"LOAD CLEANUP PASS {count}")
        self.lifecycle_results.append({"players": count, "disconnected_at_stop": len(left), "passed": True,
            "seconds_including_waits_and_reconnect": time.perf_counter() - started})

    async def run(self):
        cache = ROOT / ".paper-smoke" / self.args.version
        paper = next(cache.glob("paper-*.jar"))
        version = re.search(r'version\s*=\s*"([^"]+)"', (ROOT / "build.gradle").read_text()).group(1)
        self.target = Path(tempfile.mkdtemp(prefix="load-" + self.args.version + "-", dir=ROOT / ".paper-smoke"))
        print("LOAD DIRECTORY " + str(self.target), flush=True)
        for name in ("cache", "libraries", "versions"):
            if (cache / name).exists():
                shutil.copytree(cache / name, self.target / name)
        shutil.copy2(cache / "eula.txt", self.target / "eula.txt")
        data = self.target / "plugins/NewGodWar"
        data.mkdir(parents=True)
        shutil.copy2(ROOT / f"build/libs/NewGodWar-{version}.jar", self.target / "plugins/NewGodWar.jar")
        shutil.copy2(ROOT / "plugin/build/load-regression/LoadRegressionProbe.jar", self.target / "plugins/LoadRegressionProbe.jar")
        (data / "config.yml").write_text("updates:\n  enabled: false\nworld:\n  reset-game-world-on-stop: false\ngame:\n  select-right: false\n  fast-ready-countdown-seconds: 2\n  killtime-seconds: 0\n  remove-entities: false\n  reveal-abilities-on-end: false\n", encoding="utf-8")
        with socket.socket() as sock:
            sock.bind(("127.0.0.1", 0))
            self.port = sock.getsockname()[1]
        (self.target / "server.properties").write_text(
            f"server-ip=127.0.0.1\nserver-port={self.port}\nonline-mode=false\nmax-players=128\nview-distance=4\nsimulation-distance=4\n"
            'spawn-protection=0\nlevel-type=minecraft:flat\ngenerator-settings={"layers":[{"block":"minecraft:bedrock","height":1},{"block":"minecraft:dirt","height":2},{"block":"minecraft:grass_block","height":1}],"biome":"minecraft:plains"}\n'
            "generate-structures=false\nallow-flight=true\nnetwork-compression-threshold=256\nwhite-list=false\nenforce-whitelist=false\nenforce-secure-profile=false\n", encoding="utf-8")
        (self.target / "bukkit.yml").write_text("settings:\n  connection-throttle: -1\n", encoding="utf-8")
        self.process = await asyncio.create_subprocess_exec("java", "-Xms1G", "-Xmx3G", "-XX:+UseG1GC", "-jar", str(paper), "nogui",
            cwd=self.target, stdin=asyncio.subprocess.PIPE, stdout=asyncio.subprocess.PIPE, stderr=asyncio.subprocess.STDOUT,
            creationflags=0x08000000 if os.name == "nt" else 0)
        reader = asyncio.create_task(self.read_logs())
        passed = False
        try:
            await self.wait_marker("LOAD READY")
            self.ids = {key: int(value) for key, value in props(self.target / "load-protocol.properties").items()}
            self.protocol_version = self.ids.pop("version")
            if self.protocol_version != 777:
                raise RuntimeError("This client currently implements protocol 777 (Paper 26.3)")
            self.reverse = {(key.split(".")[0], value): key.split(".")[-1] for key, value in self.ids.items() if ".clientbound." in key}
            (self.target / "load-clients.json").write_text(json.dumps({"port": self.port, "protocol_version": self.protocol_version, "ids": self.ids}))
            await self.measure("idle", 5 if self.args.pilot else 15)
            stages = [(count, False) for count in self.args.players]
            if not self.args.pilot:
                stages.append((max(self.args.players), True))
            for count, urf in stages:
                await self.connect_to(count)
                await self.command("loadprobe start" + (" urf" if urf else ""), "LOAD MATCH READY")
                self.active = False
                await asyncio.sleep(3 if self.args.pilot else 10)
                await self.measure(str(count) + ("-urf" if urf else "-normal"), self.args.seconds)
                await self.lifecycle(count)
            if self.errors:
                raise RuntimeError(self.errors[0])
            for result in self.results[1:]:
                if int(result["moves"]) == 0 or int(result["interactions"]) == 0 or int(result["resource_spent"]) == 0 or not result["rtt_samples"]:
                    raise AssertionError("No actual load recorded: " + str(result))
            passed = True
            print("LOAD TEST COMPLETE", flush=True)
        finally:
            await asyncio.gather(*(worker.call("quit") for worker in self.workers.values()), return_exceptions=True)
            for worker in self.workers.values():
                try:
                    await asyncio.wait_for(worker.process.wait(), 10)
                except asyncio.TimeoutError:
                    worker.process.kill()
                    await worker.process.wait()
            if self.process.returncode is None:
                self.process.stdin.write(b"stop\n")
                await self.process.stdin.drain()
                try:
                    await asyncio.wait_for(self.process.wait(), 45)
                except asyncio.TimeoutError:
                    self.process.kill()
                    await self.process.wait()
            await reader
            (self.target / "load.log").write_text("".join(self.lines), encoding="utf-8")
            performance_ok = all(float(result["tps"]) >= 19 and float(result["tick_ms_p95"]) < 50
                and result["rtt_p95_ms"] is not None and result["rtt_p95_ms"] < 200 for result in self.results[1:])
            (self.target / "results.json").write_text(json.dumps({"completed": passed, "performance_thresholds_passed": passed and performance_ok,
                "thresholds": {"min_tps": 19, "max_p95_tick_ms": 50, "max_p95_rtt_ms": 200},
                "server": self.args.version, "protocol": getattr(self, "protocol_version", None),
                "clients_per_worker": self.args.clients_per_worker,
                "plugin_sha256": hashlib.sha256((self.target / "plugins/NewGodWar.jar").read_bytes()).hexdigest(),
                "phases": self.results, "lifecycle_checks": self.lifecycle_results, "client_errors": self.errors,
                "server_errors": self.server_errors}, indent=2), encoding="utf-8")
            print("LOAD RESULTS " + str(self.target / "results.json"), flush=True)


if __name__ == "__main__":
    if len(sys.argv) == 3 and sys.argv[1] == "--worker":
        asyncio.run(worker_main(sys.argv[2]))
        raise SystemExit(0)
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--version", default="26.3", choices=["26.3"])
    parser.add_argument("--players", default="16,32,64", type=lambda value: [int(n) for n in value.split(",")])
    parser.add_argument("--seconds", type=int, default=60)
    parser.add_argument("--clients-per-worker", type=int, default=4, choices=[2, 4, 8, 16])
    parser.add_argument("--pilot", action="store_true")
    options = parser.parse_args()
    if not options.players or min(options.players) < 2 or max(options.players) > 100 or options.seconds < 5:
        parser.error("Use 2-100 players and at least 5 seconds per phase")
    asyncio.run(Test(options).run())
