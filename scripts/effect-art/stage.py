"""Copy explicitly rebuilt packs into this repository's resoucepack/ directory.

Run gradlew resourcePack first when artwork changes. This tool performs no network
operations, creates no repository/branch and never runs during a plugin build.
Commit the resulting ZIPs and metadata with the normal source changes.
"""
from pathlib import Path
import shutil
import subprocess
import sys

ROOT = Path(__file__).resolve().parents[2]
subprocess.run([sys.executable, str(ROOT / 'scripts/effect-art/verify_versions.py'), '--skip-jar'], check=True)
target = ROOT / 'resoucepack'
target.mkdir(exist_ok=True)
rows = [line.split('\t') for line in (ROOT / 'build/libs/NewGodWar-Art-manifest.tsv').read_text().splitlines()[1:]]
for filename in sorted({row[1] for row in rows}):
    for name in (filename, filename + '.sha1'):
        shutil.copyfile(ROOT / 'build/libs' / name, target / name)
base = 'https://raw.githubusercontent.com/minjae9010/NewGodWar/master/resoucepack/'
manifest = 'Minecraft\tfile\tsha1\turl\n' + ''.join(
    f'{version}\t{filename}\t{sha}\t{base}{filename}\n' for version, filename, sha in rows)
(target / 'manifest.tsv').write_text(manifest, encoding='utf-8', newline='\n')
for name in ('art-packs.properties', 'art-models.properties'):
    shutil.copyfile(ROOT / 'build/generated/pack-resources' / name, ROOT / 'plugin/src/main/resources' / name)
print('Updated resoucepack/ and plugin metadata locally. Commit them together; no remote publication was performed.')
