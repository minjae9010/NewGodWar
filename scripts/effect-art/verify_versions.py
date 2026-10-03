"""Check the combined pack against the runtime catalogue, both item-model pipelines and every release."""
import hashlib
import json
import re
import sys
import zipfile
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from packfile import ROOT, built_pack

rows = [line.split('|') for line in (ROOT / 'scripts/effect-art/pack-versions.tsv').read_text().splitlines()
        if line and not line.startswith('#')]

def properties(path):
    return dict(line.split('=', 1) for line in path.read_text().splitlines() if line and not line.startswith('#'))

catalogue = properties(ROOT / 'build/generated/pack-resources/art-packs.properties')
model_ids = {key: int(value) for key, value in properties(ROOT / 'build/generated/pack-resources/art-models.properties').items()}
assert len(set(model_ids.values())) == len(model_ids)
assert len([key for key in model_ids if key.startswith('gui/')]) == 31
source = (ROOT / 'plugin/src/main/java/kr/newgodwar/gui/GuiIcon.java').read_text()
icons = re.findall(r'\b[A-Z][A-Z_]+\b', source.split('enum GuiIcon {')[1].split(';')[0])
assert {key[4:] for key in model_ids if key.startswith('gui/')} == {icon.lower() for icon in icons}

def select(overrides, value, base):
    result = base
    for entry in overrides:
        if value >= entry['predicate']['custom_model_data']:
            result = entry['model']
    return result

path = built_pack()
sha = hashlib.sha1(path.read_bytes()).hexdigest()
assert path.name == f'NewGodWar-Art-{sha[:8]}.zip', 'The pack must be named by its own hash'
assert path.with_suffix('.zip.sha1').read_text().strip() == sha

covered = set()
for system, versions in rows:
    for version in versions.split(','):
        assert version not in covered, version
        covered.add(version)
        assert catalogue[version] == f'{path.name}|{sha}|{system}', version

with zipfile.ZipFile(path) as archive:
    names = archive.namelist()
    assert len(names) == len(set(names))
    assert all(not name.startswith('/') and '..' not in name.split('/') for name in names)
    objects = {name: json.loads(archive.read(name)) for name in names if name.endswith(('.json', '.mcmeta'))}
    meta = objects['pack.mcmeta']['pack']
    # Every syntax at once: pack_format (<1.20.2), supported_formats (1.20.2-1.21.8), min/max_format (1.21.9+).
    assert meta['supported_formats'] == [4, 97] and meta['min_format'] == 4 and meta['max_format'] == 97
    assert 4 <= meta['pack_format'] <= 97
    assert not any('/atlases/' in name for name in names), 'Textures under item/ need no atlas file'
    assert not any(name.startswith('assets/newgodwar/textures/') and '/textures/item/' not in name for name in names)
    for name, obj in objects.items():
        if '/models/' in name:
            for texture in obj.get('textures', {}).values():
                if texture.startswith('newgodwar:'):
                    assert texture.startswith('newgodwar:item/'), (name, texture)
                    assert 'assets/newgodwar/textures/' + texture.split(':')[1] + '.png' in names, (name, texture)
            for entry in obj.get('overrides', []):
                if entry['model'].startswith('newgodwar:'):
                    assert 'assets/newgodwar/models/' + entry['model'].split(':')[1] + '.json' in names
    # Modern pipeline (1.21.4+): item model definitions.
    for key in model_ids:
        node = objects[f'assets/newgodwar/items/{key}.json']['model']
        assert node['model'] == 'newgodwar:' + key
        assert f'assets/newgodwar/models/{key}.json' in names
    foods = ['bread', 'cooked_chicken', 'cooked_beef', 'baked_potato', 'cooked_porkchop', 'cooked_cod']
    for index, material in enumerate(foods):
        node = objects[f'assets/minecraft/items/{material}.json']['model']
        assert node['type'] == 'minecraft:range_dispatch'
        assert node['entries'][0]['threshold'] == 73101 + index
        assert node['entries'][1]['threshold'] == 73102 + index
        assert node['entries'][1]['model'] == node['fallback']
    # Legacy pipeline (1.14 - 1.21.3): custom_model_data overrides.
    overrides = objects['assets/minecraft/models/item/paper.json']['overrides']
    for key, code in model_ids.items():
        assert select(overrides, code, 'minecraft:item/paper') == 'newgodwar:' + key
        assert select(overrides, code+1, 'minecraft:item/paper') == 'minecraft:item/paper'
    assert select(overrides, 0, 'minecraft:item/paper') == 'minecraft:item/paper'
    for index, material in enumerate(foods):
        base = 'minecraft:item/' + material
        overrides = objects[f'assets/minecraft/models/item/{material}.json']['overrides']
        code = 73101 + index
        assert select(overrides, code, base).startswith('newgodwar:art/food/')
        assert all(select(overrides, value, base) == base for value in (0, code-1, code+1, 999999))
    animated = [name for name in names if name.endswith('.png.mcmeta')]
    assert animated and all(name[:-len('.mcmeta')] in names for name in animated)

supported_source = (ROOT / 'scripts/Test-PaperMatrix.ps1').read_text()
supported = set(re.findall(r'"([0-9.]+)"', supported_source.split('function Get-SupportedPaperVersions')[1].split('function Get-LatestSupportedPaperVersion')[0]))
assert supported <= covered, supported - covered
assert covered == set(catalogue)
version = re.search(r'version = "([^"]+)"', (ROOT / 'build.gradle').read_text()).group(1)
if '--skip-jar' not in sys.argv:
    with zipfile.ZipFile(ROOT / f'build/libs/NewGodWar-{version}.jar') as jar:
        for resource in ('art-models.properties', 'art-packs.properties'):
            assert jar.read(resource) == (ROOT / 'build/generated/pack-resources' / resource).read_bytes()
print(f'PASS {path.name}: one pack for {len(covered)} releases, both model pipelines, {len(model_ids)} model IDs, {len(animated)} animations')
