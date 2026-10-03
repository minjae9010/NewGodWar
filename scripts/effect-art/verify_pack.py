"""Check built model references and food fallback dispatch without a graphics runtime."""
import json
import hashlib
import struct
import zipfile
from pathlib import Path

import sys
sys.path.insert(0, str(Path(__file__).resolve().parent))
from packfile import ROOT as root, built_pack
pack = built_pack()
materials = ['bread', 'cooked_chicken', 'cooked_beef', 'baked_potato', 'cooked_porkchop', 'cooked_cod']
with zipfile.ZipFile(pack) as archive:
    names = set(archive.namelist())
    textures = []
    for name in names:
        if '/models/art/' in name and name.endswith('.json'):
            model = json.loads(archive.read(name))
            for texture in model.get('textures', {}).values():
                if texture.startswith('#'):
                    continue
                namespace, path = texture.split(':')
                assert f'assets/{namespace}/textures/{path}.png' in names, (name, texture)
        if '/items/art/' in name and name.endswith('.json'):
            model = json.loads(archive.read(name))['model']['model']
            namespace, path = model.split(':')
            assert f'assets/{namespace}/models/{path}.json' in names, name
    for index, material in enumerate(materials):
        dispatch = json.loads(archive.read(f'assets/minecraft/items/{material}.json'))['model']
        assert dispatch['property'] == 'minecraft:custom_model_data' and dispatch['index'] == 0
        entries = sorted(dispatch['entries'], key=lambda e: e['threshold'])
        def selected(value):
            result = dispatch['fallback']
            for entry in entries:
                if value >= entry['threshold']:
                    result = entry['model']
            return result['model']
        code = 73101 + index
        base = f'minecraft:item/{material}'
        assert all(selected(value) == base for value in [0, code-1, code+1, 999999]), material
        model = selected(code)
        assert model.startswith('newgodwar:art/food/'), material
        path = model.split(':', 1)[1]
        png = archive.read(f'assets/newgodwar/textures/item/{path}.png')
        assert png[:8] == b'\x89PNG\r\n\x1a\n' and struct.unpack('>II', png[16:24]) == (128, 128)
        assert png[25] == 6, 'Food must have an alpha channel'
        textures.append(hashlib.sha256(png).hexdigest())
    assert len(set(textures)) == 6, 'Food textures must differ'
digest = hashlib.sha1(pack.read_bytes()).hexdigest()
assert pack.with_suffix('.zip.sha1').read_text().strip() == digest
print(f'PASS model/texture references, 6 distinct transparent foods, 30 fallback/dispatch cases; SHA1 {digest}')
