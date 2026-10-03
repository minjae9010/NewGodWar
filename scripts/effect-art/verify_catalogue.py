"""Validate the recorded pack catalogue without generating or downloading any ZIP."""
from pathlib import Path
import re
import sys
import zipfile

ROOT = Path(__file__).resolve().parents[2]
resources = ROOT / 'plugin/src/main/resources'
if '--plugin-only' in sys.argv:
    assert not (ROOT / 'build/pack-compiler').exists(), 'Plugin build invoked pack compilation'
    assert not (ROOT / 'build/generated/pack-resources').exists(), 'Plugin build regenerated the pack catalogue'
    assert not list((ROOT / 'build/libs').glob('*.zip')), 'Plugin build generated resource-pack ZIPs'

def properties(name):
    return dict(line.split('=', 1) for line in (resources / name).read_text(encoding='utf-8').splitlines()
                if line and not line.startswith('#'))

packs = properties('art-packs.properties')
models = properties('art-models.properties')
rows = [line.split('|') for line in (ROOT / 'scripts/effect-art/pack-versions.tsv').read_text().splitlines()
        if line and not line.startswith('#')]
expected = {}
for suffix, fmt, system, atlas, versions in rows:
    for version in versions.split(','):
        expected[version] = (f'NewGodWar-Art-{suffix}.zip', system)
assert set(packs) == set(expected)
for version, value in packs.items():
    filename, sha, system = value.split('|')
    assert (filename, system) == expected[version]
    assert re.fullmatch('[0-9a-f]{40}', sha), version
assert len(set(models.values())) == len(models)
assert all(int(value) >= 74000 for value in models.values())
source = (ROOT / 'plugin/src/main/java/kr/newgodwar/gui/GuiIcon.java').read_text(encoding='utf-8')
icons = re.findall(r'\b[A-Z][A-Z_]+\b', source.split('enum GuiIcon {')[1].split(';')[0])
assert {key[4:] for key in models if key.startswith('gui/')} == {icon.lower() for icon in icons}
art_keys = set()
for line in (resources / 'effect-art.tsv').read_text(encoding='utf-8').splitlines():
    if line and not line.startswith('#'):
        key = line.split('|')[0].lower().replace('.', '/')
        for layer in ('glyph', 'arc', 'ring', 'mote'):
            art_keys.update(f'art/{key}/{layer}{fade}' for fade in range(4))
assert art_keys == {key for key in models if key.startswith('art/')}, 'Publish updated artwork before releasing the plugin'
version = re.search(r'version = "([^"]+)"', (ROOT / 'build.gradle').read_text()).group(1)
with zipfile.ZipFile(ROOT / f'build/libs/NewGodWar-{version}.jar') as jar:
    for resource in ('art-models.properties', 'art-packs.properties'):
        # Git may check text out with CRLF on Windows; compare logical catalogues.
        assert jar.read(resource).decode().splitlines() == (resources / resource).read_text().splitlines()
print(f'PASS recorded catalogue: {len(packs)} versions, {len(models)} models and JAR metadata; no pack generation')
