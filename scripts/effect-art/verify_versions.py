"""Check every release pack against its runtime catalogue and both item-model pipelines."""
import hashlib
import json
import re
import zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
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

covered = set()
for suffix, fmt, system, atlas, versions in rows:
    filename = f'NewGodWar-Art-{suffix}.zip'
    path = ROOT / 'build/libs' / filename
    sha = hashlib.sha1(path.read_bytes()).hexdigest()
    assert path.with_suffix('.zip.sha1').read_text().strip() == sha
    for version in versions.split(','):
        assert version not in covered, version
        covered.add(version)
        assert catalogue[version] == f'{filename}|{sha}|{system}'
    with zipfile.ZipFile(path) as archive:
        names = archive.namelist()
        assert len(names) == len(set(names)), filename
        assert all(not name.startswith('/') and '..' not in name.split('/') for name in names)
        objects = {name: json.loads(archive.read(name)) for name in names if name.endswith(('.json', '.mcmeta'))}
        meta = objects['pack.mcmeta']['pack']
        if '.' in fmt:
            assert meta['min_format'] == meta['max_format'] == [int(n) for n in fmt.split('.')]
        else:
            assert meta['pack_format'] == int(fmt)
        atlases = [name for name in names if '/atlases/' in name]
        assert atlases == ([] if atlas == 'none' else [f'assets/minecraft/atlases/{atlas}.json'])
        for name, obj in objects.items():
            if '/models/' in name:
                for texture in obj.get('textures', {}).values():
                    if texture.startswith('newgodwar:'):
                        assert 'assets/newgodwar/textures/' + texture.split(':')[1] + '.png' in names, (filename, name, texture)
                for entry in obj.get('overrides', []):
                    if entry['model'].startswith('newgodwar:'):
                        assert 'assets/newgodwar/models/' + entry['model'].split(':')[1] + '.json' in names
        if system == 'modern':
            for key in model_ids:
                node = objects[f'assets/newgodwar/items/{key}.json']['model']
                assert node['model'] == 'newgodwar:' + key
                assert f'assets/newgodwar/models/{key}.json' in names
            for index, material in enumerate(['bread', 'cooked_chicken', 'cooked_beef', 'baked_potato', 'cooked_porkchop', 'cooked_cod']):
                node = objects[f'assets/minecraft/items/{material}.json']['model']
                assert node['type'] == 'minecraft:range_dispatch'
                assert node['entries'][0]['threshold'] == 73101 + index
                assert node['entries'][1]['threshold'] == 73102 + index
                assert node['entries'][1]['model'] == node['fallback']
        else:
            assert not any('/items/' in name for name in names)
            if system == 'legacy':
                overrides = objects['assets/minecraft/models/item/paper.json']['overrides']
                for key, code in model_ids.items():
                    assert select(overrides, code, 'minecraft:item/paper') == 'newgodwar:' + key
                    assert select(overrides, code+1, 'minecraft:item/paper') == 'minecraft:item/paper'
                assert select(overrides, 0, 'minecraft:item/paper') == 'minecraft:item/paper'
                for index, material in enumerate(['bread', 'cooked_chicken', 'cooked_beef', 'baked_potato', 'cooked_porkchop', 'cooked_cod']):
                    base = 'minecraft:item/' + material
                    overrides = objects[f'assets/minecraft/models/item/{material}.json']['overrides']
                    code = 73101 + index
                    assert select(overrides, code, base).startswith('newgodwar:art/food/')
                    assert all(select(overrides, value, base) == base for value in (0, code-1, code+1, 999999))
    print(f'PASS {suffix}: format {fmt}, {system}, {sha}')

supported_source = (ROOT / 'scripts/Test-PaperMatrix.ps1').read_text()
supported = set(re.findall(r'"([0-9.]+)"', supported_source.split('function Get-SupportedPaperVersions')[1].split('function Get-LatestSupportedPaperVersion')[0]))
assert supported <= covered, supported - covered
assert covered == set(catalogue)
version = re.search(r'version = "([^"]+)"', (ROOT / 'build.gradle').read_text()).group(1)
with zipfile.ZipFile(ROOT / f'build/libs/NewGodWar-{version}.jar') as jar:
    for resource in ('art-models.properties', 'art-packs.properties'):
        assert jar.read(resource) == (ROOT / 'build/generated/pack-resources' / resource).read_bytes()
print(f'PASS {len(rows)} ZIPs, {len(covered)} releases, {len(model_ids)} model IDs; shipped JAR catalogues match')
