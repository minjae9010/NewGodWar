"""Validate the menu pack's actual asset references against the server icon catalogue."""
import hashlib
import json
import re
import struct
import zipfile
from pathlib import Path

root = Path(__file__).resolve().parents[2]
source = (root / 'plugin/src/main/java/kr/newgodwar/gui/GuiIcon.java').read_text(encoding='utf-8')
catalogue = source.split('enum GuiIcon {', 1)[1].split(';', 1)[0]
icons = re.findall(r'\b[A-Z][A-Z_]+\b', catalogue)
pack = root / 'build/libs/NewGodWar-Art-26.3.zip'
with zipfile.ZipFile(pack) as archive:
    names = archive.namelist()
    assert len(names) == len(set(names)), 'Duplicate ZIP entry'
    meta = json.loads(archive.read('pack.mcmeta'))['pack']
    assert meta['min_format'] == [97, 1] and meta['max_format'] == [97, 1]
    atlas = json.loads(archive.read('assets/minecraft/atlases/items.json'))['sources']
    assert sum(entry.get('source') == 'gui' for entry in atlas) == 1, 'GUI atlas must be registered once'
    for icon in icons:
        key = icon.lower()
        definition = json.loads(archive.read(f'assets/newgodwar/items/gui/{key}.json'))['model']
        assert definition['type'] == 'minecraft:model'
        assert definition['model'] == f'newgodwar:gui/{key}'
        model = json.loads(archive.read(f'assets/newgodwar/models/gui/{key}.json'))
        assert model['parent'] == 'minecraft:item/generated', key
        texture = model['textures']['layer0']
        assert texture == f'newgodwar:gui/{key}', key
        png = archive.read(f'assets/newgodwar/textures/gui/{key}.png')
        assert png[:8] == b'\x89PNG\r\n\x1a\n'
        assert struct.unpack('>II', png[16:24]) == (128, 128), key
        assert png[25] == 6, 'Menu sprite must have an alpha channel'
    actual = [name for name in names if name.startswith('assets/newgodwar/items/gui/')]
    assert len(actual) == len(icons), 'Server and resource pack icon catalogues differ'
digest = hashlib.sha1(pack.read_bytes()).hexdigest()
assert pack.with_suffix('.zip.sha1').read_text().strip() == digest
print(f'PASS {len(icons)} menu item/model/texture chains, format 97.1, atlas, ZIP uniqueness and SHA-1 {digest}')
