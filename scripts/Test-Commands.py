"""Run command and dummy regression on a cached Paper server without downloading dependencies.

Build first: gradlew build
Run: python scripts/Test-Commands.py [26.3]
The cached .paper-smoke/<version> must include Paper and an accepted eula.txt.
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
source = root / ".paper-smoke" / version
project_version = re.search(r'version\s*=\s*"([^"]+)"', (root / "build.gradle").read_text(encoding="utf-8")).group(1)
plugin = root / "build/libs" / ("NewGodWar-" + project_version + ".jar")
probe = root / "plugin/build/command-regression/CommandRegressionProbe.jar"
paper = next(source.glob("paper-*.jar"), None)
if not paper or not plugin.is_file() or not probe.is_file() or not (source / "eula.txt").is_file():
    raise SystemExit("Build the plugin/probe and cache the Paper distribution first.")
target = Path(tempfile.mkdtemp(prefix="command-dummy-" + version + "-", dir=root / ".paper-smoke"))
for name in ("cache", "libraries", "versions"):
    if (source / name).exists():
        shutil.copytree(source / name, target / name)
shutil.copy2(source / "eula.txt", target / "eula.txt")
data = target / "plugins/NewGodWar"
data.mkdir(parents=True)
shutil.copy2(plugin, target / "plugins/NewGodWar.jar")
shutil.copy2(probe, target / "plugins/CommandRegressionProbe.jar")
(data / "config.yml").write_text("updates:\n  enabled: false\nworld:\n  reset-game-world-on-stop: false\n", encoding="utf-8")
with socket.socket() as sock:
    sock.bind(("127.0.0.1", 0))
    port = sock.getsockname()[1]
(target / "server.properties").write_text(
    f"server-ip=127.0.0.1\nserver-port={port}\nonline-mode=false\nview-distance=2\nsimulation-distance=2\n"
    "spawn-protection=0\nlevel-type=minecraft:flat\ngenerate-structures=false\n", encoding="utf-8")
process = subprocess.Popen(["java", "-DPaper.IgnoreJavaVersion=true", "-Xmx1G", "-jar", str(paper), "nogui"], cwd=target,
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
        if any(word in line for word in ("COMMAND REGRESSION", "PASS ", "ERROR", "Exception", "Done (")):
            print(line.rstrip(), flush=True)
        if "COMMAND REGRESSION PASS" in line:
            passed = True
            break
        if "COMMAND REGRESSION FAILED" in line:
            break
finally:
    if process.poll() is None:
        process.stdin.write("stop\n")
        process.stdin.flush()
        try:
            process.wait(timeout=40)
        except subprocess.TimeoutExpired:
            process.kill()
            process.wait(timeout=10)
    reader.join(timeout=5)
    while not output.empty():
        lines.append(output.get())
    (target / "command-regression.log").write_text("".join(lines), encoding="utf-8")
if not passed:
    failure = next((i for i, line in enumerate(lines) if "COMMAND REGRESSION FAILED" in line), None)
    print("".join(lines[failure:failure + 60] if failure is not None else lines[-90:]))
    raise SystemExit("Command and dummy regression failed; logs: " + str(target))
print("Command and dummy regression passed on Paper " + version + "; logs: " + str(target), flush=True)
