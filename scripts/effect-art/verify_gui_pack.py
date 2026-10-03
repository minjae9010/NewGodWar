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
    font = json.loads(archive.read('assets/minecraft/font/default.json'))
    assert font == json.loads(archive.read('assets/minecraft/font/uniform.json'))
    space, *panels = font['providers']
    assert space == {'type': 'space', 'advances': {'\ue100': -8, '\ue103': -168, '\ue10a': -176, '\ue10b': -1}}
    expected = [(0xe101, 5), (0xe102, 6), (0xe104, 6), (0xe105, 4), (0xe106, 4), (0xe107, 6), (0xe108, 6), (0xe109, 5)]
    assert len(panels) == len(expected) * 4
    for index, panel in enumerate(panels):
        group, part = divmod(index, 4)
        code, rows = expected[group]
        assert panel['chars'] == [chr(code if part == 0 else 0xe110 + group * 3 + part - 1)]
        assert panel['height'] == (rows * 18 + 114) // 2
        assert panel['ascent'] == 13 - (part // 2) * panel['height']
        namespace, texture = panel['file'].split(':')
        png = archive.read(f'assets/{namespace}/textures/{texture}')
        width, height = struct.unpack('>II', png[16:24])
        assert (width, height) == (176, panel['height'] * 2)
        assert max(width, height) <= 256, 'Bitmap glyph must fit Minecraft font atlas'
        assert -8 + 2 * (88 + 1 - 1) - 176 + 2 * (88 + 1 - 1) - 168 == 0
    import zlib
    for layer in ('back', 'front'):
        png = archive.read(f'assets/minecraft/textures/gui/sprites/container/slot_highlight_{layer}.png')
        assert struct.unpack('>II', png[16:24]) == (24, 24)
        assert png[24:26] == bytes([8, 6])
        pos, blocks = 8, []
        while pos < len(png):
            length = struct.unpack('>I', png[pos:pos+4])[0]
            if png[pos+4:pos+8] == b'IDAT': blocks.append(png[pos+8:pos+8+length])
            pos += length + 12
        # ImageIO writes a fully zero RGBA raster; no visible pixel may remain.
        assert not any(zlib.decompress(b''.join(blocks))), 'Hover sprites must be transparent'
    assert not any('/textures/gui/container/' in name for name in names), 'Never reskin unrelated chests'
digest = hashlib.sha1(pack.read_bytes()).hexdigest()
assert pack.with_suffix('.zip.sha1').read_text().strip() == digest
print(f'PASS {len(icons)} menu item/model/texture chains, formats 4-97, default item atlas, ZIP uniqueness and SHA-1 {digest}')
