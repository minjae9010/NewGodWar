"""Run inventory, menu and core regressions on a cached Paper server (no downloads).

Build first: gradlew pluginJar :plugin:coreRegressionJar
Run: python scripts/Test-Inventory.py [26.3]
The cached .paper-smoke/<version> must include Paper and an accepted eula.txt.
Uses Java on PATH; Paper 26.3 requires Java 25. Logs remain in the isolated test folder.
"""
from pathlib import Path
import os
import queue
import re
import shutil
import socket
import subprocess
import sys
import tempfile
import threading
import time

root = Path(__file__).resolve().parents[1]
version = sys.argv[1] if len(sys.argv) > 1 else "26.3"
if not re.fullmatch(r"\d+(?:\.\d+)+", version):
    raise SystemExit("Specify a cached release version, such as 26.3 or 1.12.2.")
source = root / ".paper-smoke" / version
project_version = re.search(r'version\s*=\s*"([^"]+)"', (root / "build.gradle").read_text(encoding="utf-8")).group(1)
plugin = root / "build/libs" / ("NewGodWar-" + project_version + ".jar")
probe = root / "plugin/build/core-regression/CoreRegressionProbe.jar"
paper = next(source.glob("paper-*.jar"), None)
eula = source / "eula.txt"
java = shutil.which("java")
if not paper or not plugin.is_file() or not probe.is_file() or not eula.is_file() or not java:
    raise SystemExit("Build the plugin/probe, cache Paper, and place the appropriate Java on PATH first.")
if not re.search(r"^\s*eula\s*=\s*true\s*$", eula.read_text(encoding="utf-8-sig"), re.MULTILINE | re.IGNORECASE):
    raise SystemExit("The cached Paper distribution must already have an accepted eula.txt.")
if not (source / "cache" / ("mojang_" + version + ".jar")).is_file():
    raise SystemExit("The cached Minecraft server JAR is missing; this runner does not download it.")
if not ((source / "cache" / ("patched_" + version + ".jar")).is_file()
        or (source / "versions" / version / ("paper-" + version + ".jar")).is_file()):
    raise SystemExit("The cached patched Paper server JAR is missing; complete the cache first.")

target = Path(tempfile.mkdtemp(prefix="inventory-menu-" + version + "-", dir=root / ".paper-smoke"))
for name in ("cache", "libraries", "versions"):
    if (source / name).exists():
        shutil.copytree(source / name, target / name)
shutil.copy2(eula, target / "eula.txt")
data = target / "plugins/NewGodWar"
data.mkdir(parents=True)
shutil.copy2(plugin, target / "plugins/NewGodWar.jar")
shutil.copy2(probe, target / "plugins/CoreRegressionProbe.jar")
(data / "config.yml").write_text("updates:\n  enabled: false\nworld:\n  reset-game-world-on-stop: false\n", encoding="utf-8")
with socket.socket() as sock:
    sock.bind(("127.0.0.1", 0))
    port = sock.getsockname()[1]
(target / "server.properties").write_text(
    f"server-ip=127.0.0.1\nserver-port={port}\nonline-mode=false\nview-distance=2\nsimulation-distance=2\n"
    "spawn-protection=0\nlevel-type=minecraft:flat\ngenerate-structures=false\n", encoding="utf-8")
process = subprocess.Popen([java, "-DPaper.IgnoreJavaVersion=true", "-Xmx1G", "-jar", str(paper), "nogui"], cwd=target,
    stdin=subprocess.PIPE, stdout=subprocess.PIPE, stderr=subprocess.STDOUT, text=True, encoding="utf-8", errors="replace",
    creationflags=subprocess.CREATE_NO_WINDOW if os.name == "nt" else 0)
output = queue.Queue()


def read_output():
    for line in process.stdout:
        output.put(line)


reader = threading.Thread(target=read_output, daemon=True)
reader.start()
lines = []
passed = False
try:
    deadline = time.monotonic() + 150
    while time.monotonic() < deadline:
        try:
            line = output.get(timeout=0.5)
        except queue.Empty:
            if process.poll() is not None:
                break
            continue
        lines.append(line)
        if any(word in line for word in ("CORE REGRESSION", "PASS ", "ERROR", "Exception", "Done (")):
            print(line.rstrip(), flush=True)
        if "CORE REGRESSION PASS" in line:
            passed = True
            break
        if "CORE REGRESSION FAILED" in line:
            break
finally:
    if process.poll() is None:
        try:
            process.stdin.write("stop\n")
            process.stdin.flush()
        except (BrokenPipeError, OSError):
            pass
        try:
            process.wait(timeout=40)
        except subprocess.TimeoutExpired:
            process.kill()
            process.wait(timeout=10)
    reader.join(timeout=5)
    while not output.empty():
        lines.append(output.get())
    (target / "inventory-regression.log").write_text("".join(lines), encoding="utf-8")
if not passed:
    failure = next((i for i, line in enumerate(lines) if "CORE REGRESSION FAILED" in line), None)
    print("".join(lines[failure:failure + 60] if failure is not None else lines[-90:]))
    raise SystemExit("Inventory, menu and core regression failed; logs: " + str(target))
print("Inventory, menu and core regression passed on Paper " + version + "; logs: " + str(target), flush=True)
