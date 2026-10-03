"""Publish prebuilt packs independently of plugin releases (explicit --publish required).

Run gradlew resourcePack only when artwork changes. Upload immutable ZIP paths,
then record the published catalogues in source. Existing paths remain available.
"""
import argparse
import hashlib
from pathlib import Path
import shutil
import subprocess
import sys
import tempfile

ROOT = Path(__file__).resolve().parents[2]
REMOTE = 'https://github.com/minjae9010/NewGodWar.git'
BRANCH = 'codex/resource-packs'
BASE = 'https://raw.githubusercontent.com/minjae9010/NewGodWar/refs/heads/' + BRANCH + '/'

def git(directory, *args):
    result = subprocess.run(['git', '-c', 'safe.directory=' + str(directory), '-C', str(directory), *args],
                            check=True, capture_output=True, text=True, encoding='utf-8')
    return result.stdout.strip()

def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--publish', action='store_true', required=True, help='Upload validated packs and update source catalogues')
    parser.parse_args()
    subprocess.run([sys.executable, str(ROOT / 'scripts/effect-art/verify_versions.py'), '--skip-jar'], check=True)
    generated = ROOT / 'build/generated/pack-resources'
    rows = [line.split('\t') for line in (ROOT / 'build/libs/NewGodWar-Art-manifest.tsv').read_text().splitlines()[1:]]
    files = {filename: sha for _, filename, sha in rows}
    for filename, sha in files.items():
        if hashlib.sha1((ROOT / 'build/libs' / filename).read_bytes()).hexdigest() != sha:
            raise ValueError('Pack checksum mismatch: ' + filename)
    staging_parent = ROOT / '.build'
    staging_parent.mkdir(exist_ok=True)
    staging = Path(tempfile.mkdtemp(prefix='resource-pack-publish-', dir=staging_parent))
    existing = git(ROOT, 'ls-remote', '--heads', REMOTE, 'refs/heads/' + BRANCH)
    if existing:
        subprocess.run(['git', 'clone', '--depth', '1', '--single-branch', '--branch', BRANCH, REMOTE, str(staging)], check=True)
    else:
        git(staging, 'init', '--initial-branch=' + BRANCH)
        git(staging, 'remote', 'add', 'origin', REMOTE)
    git(staging, 'config', 'user.name', git(ROOT, 'config', 'user.name'))
    git(staging, 'config', 'user.email', git(ROOT, 'config', 'user.email'))
    for filename, sha in files.items():
        target = staging / 'packs' / sha / filename
        target.parent.mkdir(parents=True, exist_ok=True)
        if target.exists() and hashlib.sha1(target.read_bytes()).hexdigest() != sha:
            raise ValueError('Existing immutable path was changed: ' + str(target))
        if not target.exists():
            shutil.copyfile(ROOT / 'build/libs' / filename, target)
        target.with_suffix('.zip.sha1').write_text(sha + '\n', encoding='ascii', newline='\n')
    manifest = 'Minecraft\tfile\tsha1\turl\n' + ''.join(
        f'{version}\t{filename}\t{sha}\t{BASE}packs/{sha}/{filename}\n' for version, filename, sha in rows)
    (staging / 'NewGodWar-Art-manifest.tsv').write_text(manifest, encoding='utf-8', newline='\n')
    for resource in ('art-models.properties', 'art-packs.properties'):
        shutil.copyfile(generated / resource, staging / resource)
    readme = '''# NewGodWar resource packs

This branch stores resource packs independently of plugin releases.
ZIPs are published once at `packs/<SHA-1>/<filename>.zip` and reused by later plugin versions.
Existing paths must not be overwritten or removed. Only changed packs need a new path.

See [the version catalogue](NewGodWar-Art-manifest.tsv) for direct GitHub Raw URLs.
The plugin's `url: auto` selects its recorded filename and SHA-1 from this space.
Ordinary plugin builds and releases do not generate or upload these files.

To update artwork, use `gradlew resourcePack`, then `python scripts/effect-art/publish.py --publish`
from the source branch. Commit the updated source catalogues after publishing.

## Downloads

| Minecraft versions | ZIP | SHA-1 |
| --- | --- | --- |
'''
    for filename, sha in files.items():
        versions = ', '.join(version for version, name, _ in rows if name == filename)
        url = BASE + 'packs/' + sha + '/' + filename
        readme += f'| {versions} | [{filename}]({url}) | [SHA-1]({url}.sha1) |\n'
    (staging / 'README.md').write_text(readme, encoding='utf-8', newline='\n')
    (staging / '.gitattributes').write_text('*.zip binary\n*.properties text eol=lf\n*.tsv text eol=lf\n', encoding='ascii', newline='\n')
    git(staging, 'add', '.')
    if git(staging, 'status', '--porcelain'):
        git(staging, 'commit', '-m', 'Publish versioned resource packs in independent static storage')
        print(git(staging, 'push', 'origin', 'HEAD:refs/heads/' + BRANCH))
    # Do not update source metadata until remote publication succeeds.
    for resource in ('art-models.properties', 'art-packs.properties'):
        shutil.copyfile(generated / resource, ROOT / 'plugin/src/main/resources' / resource)
    print(f'Published {len(files)} packs for {len(rows)} versions: {BASE}NewGodWar-Art-manifest.tsv')
    print('Commit plugin/src/main/resources/art-*.properties with the source changes.')

if __name__ == '__main__':
    main()
