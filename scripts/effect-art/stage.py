"""Copy the explicitly rebuilt combined pack into this repository's resoucepack/ directory.

Run gradlew resourcePack first when artwork changes. This tool performs no network
operations, creates no repository/branch and never runs during a plugin build.
Commit the resulting ZIP and metadata with the normal source changes.

The pack is named by its own hash. Earlier packs are left in place on purpose: a released
plugin keeps downloading the exact file and SHA-1 it embeds. Remove one only once no server
runs the release that references it.
"""
from pathlib import Path
import shutil
import subprocess
import sys

sys.path.insert(0, str(Path(__file__).resolve().parent))
from packfile import ROOT, DIST, built_pack

subprocess.run([sys.executable, str(ROOT / 'scripts/effect-art/verify_versions.py'), '--skip-jar'], check=True)
target = ROOT / 'resoucepack'
target.mkdir(exist_ok=True)
pack = built_pack()
for name in (pack.name, pack.name + '.sha1'):
    shutil.copyfile(DIST / name, target / name)
rows = [line.split('\t') for line in (DIST / 'manifest.tsv').read_text().splitlines()[1:]]
assert {row[1] for row in rows} == {pack.name}
base = 'https://raw.githubusercontent.com/minjae9010/NewGodWar/master/resoucepack/'
manifest = 'Minecraft\tfile\tsha1\turl\n' + ''.join(
    f'{version}\t{filename}\t{sha}\t{base}{filename}\n' for version, filename, sha in rows)
(target / 'manifest.tsv').write_text(manifest, encoding='utf-8', newline='\n')
for name in ('art-packs.properties', 'art-models.properties'):
    shutil.copyfile(ROOT / 'build/generated/pack-resources' / name, ROOT / 'plugin/src/main/resources' / name)
print(f'Staged {pack.name} for every release. Earlier packs were kept for released plugin versions; '
      'no remote publication was performed.')
