"""Validate native cuboid geometry, shared atlases, and unchanged item model IDs."""
import json
import math
import struct
import zipfile
from pathlib import Path
from packfile import ROOT, built_pack


def properties(path):
    return dict(line.split('=', 1) for line in path.read_text().splitlines()
                if line and not line.startswith('#'))


resources = ROOT / 'plugin/src/main/resources'
solid = properties(resources / 'solid-art.properties')
recorded = properties(resources / 'art-models.properties')
generated = properties(ROOT / 'build/generated/pack-resources/art-models.properties')
assert generated == recorded, 'Native geometry must not add, remove, or renumber item model IDs'
assert set(solid.values()) == {'hammer', 'spear', 'trident', 'dagger', 'shield'}
assert len(solid) == 10
meshes = set()
with zipfile.ZipFile(built_pack()) as archive:
    for key, kind in solid.items():
        reference = None
        atlases = set()
        for fade in range(4):
            path = f'art/{key}/glyph{fade}'
            assert f'assets/newgodwar/textures/item/{path}.png' not in archive.namelist(), 'Unused flat glyphs must not consume atlas space'
            model = json.loads(archive.read(f'assets/newgodwar/models/{path}.json'))
            elements = model['elements']
            assert 8 <= len(elements) <= 40, (key, 'cuboid budget')
            assert model['ambientocclusion'] is False
            assert any(element['shade'] for element in elements), 'Metal must have directional shading'
            assert any(not element['shade'] for element in elements), 'Rune inlays remain legible'
            if kind == 'shield':
                # The client appends Ry(PI) when rendering every ItemDisplay. Model -Z must carry
                # the front decoration so local display +Z faces outward / toward a billboard viewer.
                runes = [element for element in elements if not element['shade']]
                grips = [element for element in elements
                         if 6 < element['faces']['north']['uv'][0] < 8]
                assert runes and all(element['to'][2] < 8 for element in runes), 'Shield front must face model -Z'
                assert grips and all(element['from'][2] > 8 for element in grips), 'Shield grips must stay on model +Z'
            for element in elements:
                low, high = element['from'], element['to']
                assert len(low) == len(high) == 3
                for a, b in zip(low, high):
                    assert math.isfinite(a) and math.isfinite(b) and 0 <= a < b <= 16, (key, element)
                assert set(element['faces']) == {'north', 'south', 'east', 'west', 'up', 'down'}
                for face in element['faces'].values():
                    assert face['texture'] == '#surface'
                    u0, v0, u1, v1 = face['uv']
                    assert 0 <= u0 < u1 <= 16 and 0 <= v0 < v1 <= 16
            if reference is None:
                reference = elements
            assert elements == reference, 'Fade changes textures, never mesh topology'
            texture = model['textures']['surface']
            assert texture == f'newgodwar:item/art/{key}/material{fade}'
            png = archive.read('assets/newgodwar/textures/' + texture.split(':')[1] + '.png')
            assert png[:8] == b'\x89PNG\r\n\x1a\n'
            assert struct.unpack('>II', png[16:24]) == (128, 16)
            assert png[25] == 6, 'Atlas must preserve alpha for the existing dissolve animation'
            atlases.add(png)
            node = json.loads(archive.read(f'assets/newgodwar/items/{path}.json'))['model']
            assert node == {'type': 'minecraft:model', 'model': 'newgodwar:' + path}
        assert len(atlases) == 4
        meshes.add(json.dumps(reference, sort_keys=True))
assert len(meshes) == 5, 'Ten scenes reuse five distinct native silhouettes'
print('PASS 10 native props / 5 meshes / 40 fade models: six-sided cuboids, bounded geometry, atlases and stable model IDs')
