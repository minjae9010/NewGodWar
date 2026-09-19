"""Restart/crash regression against an already cached Paper distribution (no downloads).

Build first: gradlew :plugin:recoveryRegressionJar build
Run: python scripts/Test-GameRecovery.py [26.3] [plugin.jar] [probe.jar]
"""
from pathlib import Path
import os
import gzip
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
project_version = re.search(r'version\s*=\s*"([^"]+)"', (root / "build.gradle").read_text(encoding="utf-8")).group(1)
plugin_jar = Path(sys.argv[2]).resolve() if len(sys.argv) > 2 else root / "build/libs" / ("NewGodWar-" + project_version + ".jar")
probe_jar = Path(sys.argv[3]).resolve() if len(sys.argv) > 3 else root / "plugin/build/recovery-regression/RecoveryRegressionProbe.jar"
source = root / ".paper-smoke" / version
target = Path(tempfile.mkdtemp(prefix="recovery-" + version + "-", dir=root / ".paper-smoke"))
for name in ("cache", "libraries", "versions"):
    if (source / name).exists():
        shutil.copytree(source / name, target / name)
shutil.copy2(source / "eula.txt", target / "eula.txt")
data = target / "plugins/NewGodWar"
data.mkdir(parents=True)
shutil.copy2(plugin_jar, target / "plugins/NewGodWar.jar")
shutil.copy2(probe_jar, target / "plugins/RecoveryRegressionProbe.jar")
(data / "config.yml").write_text("updates:\n  enabled: false\ngame:\n  recovery-save-interval-seconds: 1\n  reveal-abilities-on-end: false\ntips:\n  enabled: false\n", encoding="utf-8")
with socket.socket() as sock:
    sock.bind(("127.0.0.1", 0))
    port = sock.getsockname()[1]
(target / "server.properties").write_text(f"server-ip=127.0.0.1\nserver-port={port}\nonline-mode=false\nview-distance=2\nsimulation-distance=2\nspawn-protection=0\ngenerate-structures=false\n", encoding="utf-8")
paper = next(source.glob("paper-*.jar"))
for phase in range(1, 7):
    if phase == 6:
        (data / "game-session.yml").write_text("state: [broken", encoding="utf-8")
    process = subprocess.Popen(["java", "-DPaper.IgnoreJavaVersion=true", "-Xmx1G", "-jar", str(paper), "nogui"], cwd=target,
        stdin=subprocess.PIPE, stdout=subprocess.PIPE, stderr=subprocess.STDOUT, text=True, encoding="utf-8", errors="replace",
        creationflags=subprocess.CREATE_NO_WINDOW if os.name == "nt" else 0)
    output = queue.Queue()
    def read_output(stream=process.stdout, sink=output):
        for line in stream:
            sink.put(line)
    reader = threading.Thread(target=read_output, daemon=True)
    reader.start()
    lines = []
    passed = False
    marker = "RECOVERY REGRESSION PASS" if phase == 6 else "RECOVERY PHASE " + str(phase)
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
            if any(word in line for word in ("RECOVERY", "Recovered game", "recovery failed", "ERROR", "Exception", "Done (")):
                print(line.rstrip(), flush=True)
            if marker in line:
                passed = True
                break
            if "RECOVERY REGRESSION FAILED" in line:
                break
    finally:
        if process.poll() is None:
            if passed and phase in (2, 4):
                process.kill()  # No onDisable or shutdown checkpoint.
            else:
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
        (target / ("phase-" + str(phase) + ".log")).write_text("".join(lines), encoding="utf-8")
    if any("Error loading saved data" in line or "Could not save data" in line for line in lines):
        passed = False
    if not passed:
        print("".join(lines[-90:]))
        raise SystemExit("Recovery regression failed; logs: " + str(target))
    if phase == 6:
        assert (data / "game-session.yml").read_text(encoding="utf-8") == "state: [broken", "Shutdown replaced corrupt session"
    # A PASS marker must not hide partially written compressed world metadata.
    for saved_file in target.rglob("*.dat"):
        if saved_file.name == "uid.dat":
            continue
        payload = saved_file.read_bytes()
        if len(payload) < 2:
            raise AssertionError("Empty/truncated world data: " + str(saved_file))
        if payload[:2] == b"\x1f\x8b":
            gzip.decompress(payload)
    time.sleep(2)
print("All recovery phases passed on Paper " + version + "; logs: " + str(target), flush=True)
