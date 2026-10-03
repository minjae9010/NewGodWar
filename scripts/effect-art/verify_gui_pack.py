"""Validate the menu pack's actual asset references against the server icon catalogue."""
import hashlib
import json
import re
import struct
import zipfile
from pathlib import Path

import sys
sys.path.insert(0, str(Path(__file__).resolve().parent))
from packfile import ROOT as root, built_pack
source = (root / 'plugin/src/main/java/kr/newgodwar/gui/GuiIcon.java').read_text(encoding='utf-8')
catalogue = source.split('enum GuiIcon {', 1)[1].split(';', 1)[0]
icons = re.findall(r'\b[A-Z][A-Z_]+\b', catalogue)
pack = built_pack()
with zipfile.ZipFile(pack) as archive:
    names = archive.namelist()
    assert len(names) == len(set(names)), 'Duplicate ZIP entry'
    meta = json.loads(archive.read('pack.mcmeta'))['pack']
    assert meta['min_format'] == 4 and meta['max_format'] == 97, 'One pack serves 1.14 through 26.3'
    assert not any('/atlases/' in name for name in names), 'Menu sprites live in the default item atlas'
    for icon in icons:
        key = icon.lower()
        definition = json.loads(archive.read(f'assets/newgodwar/items/gui/{key}.json'))['model']
        assert definition['type'] == 'minecraft:model'
        assert definition['model'] == f'newgodwar:gui/{key}'
        model = json.loads(archive.read(f'assets/newgodwar/models/gui/{key}.json'))
        assert model['parent'] == 'minecraft:item/generated', key
        texture = model['textures']['layer0']
        assert texture == f'newgodwar:item/gui/{key}', key
        png = archive.read(f'assets/newgodwar/textures/item/gui/{key}.png')
        assert png[:8] == b'\x89PNG\r\n\x1a\n'
        assert struct.unpack('>II', png[16:24]) == (128, 128), key
        assert png[25] == 6, 'Menu sprite must have an alpha channel'
    actual = [name for name in names if name.startswith('assets/newgodwar/items/gui/')]
    assert len(actual) == len(icons), 'Server and resource pack icon catalogues differ'
digest = hashlib.sha1(pack.read_bytes()).hexdigest()
assert pack.with_suffix('.zip.sha1').read_text().strip() == digest
print(f'PASS {len(icons)} menu item/model/texture chains, formats 4-97, default item atlas, ZIP uniqueness and SHA-1 {digest}')
