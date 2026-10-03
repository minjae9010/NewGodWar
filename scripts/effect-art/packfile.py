"""Locate the single combined art pack built by `gradlew resourcePack` (named by its own SHA-1)."""
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
DIST = ROOT / 'build/effect-pack/dist'


def built_pack():
    packs = sorted(DIST.glob('NewGodWar-Art-*.zip'))
    assert len(packs) == 1, f'Expected exactly one built pack in {DIST}, found {[p.name for p in packs]}'
    return packs[0]
