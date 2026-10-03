"""Validate the recorded pack catalogue without generating or downloading any ZIP."""
from pathlib import Path
import re
import hashlib
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
for system, versions in rows:
    for version in versions.split(','):
        expected[version] = system
assert set(packs) == set(expected)
assert len({value.split('|')[0] for value in packs.values()}) == 1, 'Every release is served the same combined pack'
for version, value in packs.items():
    filename, sha, system = value.split('|')
    assert system == expected[version]
    assert re.fullmatch('[0-9a-f]{40}', sha), version
    assert filename == f'NewGodWar-Art-{sha[:8]}.zip', 'The pack is named by its own hash'
static = ROOT / 'resoucepack'
files = {value.split('|')[0]: value.split('|')[1] for value in packs.values()}
# Older packs may remain for released plugin versions; the current one must be present.
assert set(files) <= {p.name for p in static.glob('*.zip')}, 'Static directory is missing the catalogued pack'
for filename, sha in files.items():
    assert hashlib.sha1((static / filename).read_bytes()).hexdigest() == sha, filename
    assert (static / (filename + '.sha1')).read_text().strip() == sha
manifest = [line.split('\t') for line in (static / 'manifest.tsv').read_text().splitlines()[1:]]
assert len(manifest) == len(packs)
for version, filename, sha, url in manifest:
    assert packs[version].split('|')[:2] == [filename, sha]
    assert url == 'https://raw.githubusercontent.com/minjae9010/NewGodWar/master/resoucepack/' + filename
assert len(set(models.values())) == len(models)
assert all(int(value) >= 74000 for value in models.values())
source = (ROOT / 'plugin/src/main/java/kr/newgodwar/gui/GuiIcon.java').read_text(encoding='utf-8')
icons = re.findall(r'\b[A-Z][A-Z_]+\b', source.split('enum GuiIcon {')[1].split(';')[0])
assert {key[4:] for key in models if key.startswith('gui/')} == {icon.lower() for icon in icons}
art_keys = set()
for line in (resources / 'effect-art.tsv').read_text(encoding='utf-8').splitlines():
    if line and not line.startswith('#'):
        fields = line.split('|')
        key = fields[0].lower().replace('.', '/')
        art_keys.update(f'art/{key}/glyph{fade}' for fade in range(4))
        art_keys.update(f'art/{key}/draw{frame}' for frame in range(2))
        for layer in ('flash', 'wave', 'sigil', 'ring', 'beam', 'spark', 'shard'):
            art_keys.update(f'art/fx/{fields[4]}/{layer}{fade}' for fade in range(4))
        for layer in ('burst', 'slash', 'craft', 'chart', 'crash'):
            art_keys.update(f'art/fx/{fields[4]}/{layer}{frame}' for frame in range(6))
        for prop in (fields[5].split(',') if len(fields) > 5 else []):
            art_keys.update(f'art/{key}/{prop.split(":")[0]}{fade}' for fade in range(4))
assert art_keys == {key for key in models if key.startswith('art/')}, 'Publish updated artwork before releasing the plugin'
version = re.search(r'version = "([^"]+)"', (ROOT / 'build.gradle').read_text()).group(1)
with zipfile.ZipFile(ROOT / f'build/libs/NewGodWar-{version}.jar') as jar:
    assert not any(name.endswith('.zip') or name.startswith(('resoucepack/', 'assets/newgodwar/')) for name in jar.namelist()), 'Resource packs must not be bundled in the plugin JAR'
    for resource in ('art-models.properties', 'art-packs.properties'):
        # Git may check text out with CRLF on Windows; compare logical catalogues.
        assert jar.read(resource).decode().splitlines() == (resources / resource).read_text().splitlines()
print(f'PASS recorded catalogue: {len(packs)} versions, {len(models)} models and static ZIPs; JAR contains metadata only, no pack generation')
