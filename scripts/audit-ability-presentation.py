"""Trace authored ability presentation from event handlers, rather than model registration.

This is a conservative source audit, not a substitute for server/client execution.
The exported evidence retains handler names and source lines for manual review.
"""
import argparse
import hashlib
import json
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
BUILTIN = ROOT / 'plugin/src/main/java/kr/newgodwar/ability/builtin'


def masked(source):
    return re.sub(r'"(?:\\.|[^"\\])*"|\'(?:\\.|[^\'\\])*\'|//[^\n]*|/\*.*?\*/',
                  lambda m: ''.join('\n' if c == '\n' else ' ' for c in m[0]), source, flags=re.S)


def methods(source):
    safe = masked(source)
    result = {}
    pattern = r'\b(?:public|protected|private)\s+(?:(?:static|final|synchronized)\s+)*(?:[\w.<>\[\],?]+\s+)+(?P<name>\w+)\s*\([^)]*\)\s*(?:throws [\w., ]+)?\s*\{'
    for match in re.finditer(pattern, safe):
        depth, end = 1, match.end()
        while depth and end < len(safe):
            depth += (safe[end] == '{') - (safe[end] == '}')
            end += 1
        result[match['name']] = {'body': source[match.end():end-1],
                                 'line': source.count('\n', 0, match.start())+1}
    return result


def reachable(names, bodies):
    found = set(names)
    pending = list(names)
    while pending:
        body = bodies[pending.pop()]['body']
        for name in re.findall(r'(?<![.\w])([a-zA-Z]\w*)\s*\(', masked(body)):
            if name in bodies and name not in found:
                found.add(name)
                pending.append(name)
    return sorted(found)


def field(style, key):
    match = re.search(r'\.'+key+r'\(EffectCue\.(\w+)\)', style)
    return match[1] if match else 'NONE'


def audit():
    rows = []
    for path in sorted(BUILTIN.glob('*Ability.java')):
        source = path.read_text(encoding='utf-8')
        annotation = re.search(r'@AbilityInfo\((.*?)\)\s*(?:public )?(?:final )?class', source, re.S)
        if not annotation:
            continue
        metadata = dict(re.findall(r'(id|name|normalSkill|advancedSkill|passiveSkill)\s*=\s*"((?:\\.|[^"\\])*)"', annotation[1]))
        style = re.search(r'AbilityStyle\.builder\(AbilityTheme\.(\w+)\)(.*?)\.build\(\)', source, re.S)
        assert style, path
        declarations = dict(re.findall(r'\.effect\(EffectCue\.(\w+),\s*AbilityDesigns\.(\w+)\)', style[2]))
        recipients = dict(re.findall(r'\.received\(EffectCue\.(\w+),\s*AbilityDesigns\.(\w+)\)', style[2]))
        bodies = methods(source)
        assert bodies, path
        used, paths = set(), {}
        for phase, skill, entry, activation in [('normal', 'normalSkill', 'onStaffLeft', 'useNormal'),
                                               ('advanced', 'advancedSkill', 'onStaffRight', 'useAdvanced'),
                                               ('passive', 'passiveSkill', '', '')]:
            roots = []
            if phase != 'passive' and metadata.get(skill) != '없음':
                if entry in bodies:
                    roots.append(entry)
                # Chat spells, signs, projectile release and delayed readiness use their real handlers.
                roots += [name for name, method in bodies.items() if re.search(r'\b'+activation+r'\(', method['body'])]
                roots += [name for name, method in bodies.items() if re.search(
                    r'feedback\.activated\(context,\s*player,\s*'+('false' if phase=='normal' else 'true')+r'\)', method['body'])]
                if phase == 'normal':
                    roots += [name for name, method in bodies.items() if 'readyNormal(' in method['body']]
            elif phase == 'passive' and metadata.get(skill) != '없음':
                roots = [name for name in bodies if name.startswith('on') and name not in ('onStaffLeft', 'onStaffRight', 'onChatMessage')]
                if 'onChatMessage' in bodies:
                    roots.append('onChatMessage')
            names = reachable(roots, bodies)
            text = '\n'.join(bodies[name]['body'] for name in names)
            cues = set()
            cast = field(style[2], phase)
            if cast != 'NONE' and roots:
                if phase != 'passive' or 'feedback.passive(' in text:
                    cues.add(cast)
            if 'feedback.passive(' in text or 'feedback.progress(' in text:
                cues.add(field(style[2], 'passive'))
            for call in re.finditer(r'feedback\.(?:castCue|drawCue)\((.*?);', text, re.S):
                cues.update(re.findall(r'EffectCue\.(\w+)', call[1]))
            if 'setWorldTime(' in text:
                # Both explicit helpers can choose day/night without a STYLE cast cue.
                cues.update(re.findall(r'\.effect\(EffectCue\.(SUN|MOON),', style[2]))
            if 'teleportNormalToSight(' in text:
                cues.add('PORTAL')
            if 'confirmedAttack(' in text:
                cues.add(field(style[2], 'hit'))
                for call in re.finditer(r'confirmedAttack\((.*?);', text, re.S):
                    cues.update(re.findall(r'EffectCue\.(\w+)', call[1]))
            # These impacts have a Location anchor, unlike received Player impacts.
            if re.search(r'feedback\.impact\(context,\s*event\.getBlock\(\)\.getLocation\(', text):
                cues.add(field(style[2], 'hit'))
            cues.discard('NONE')
            used.update(cues)
            native = []
            for call in re.finditer(r'feedback\.(?:object|followObject|flock)\((.*?)(?:\);|return;)', text, re.S):
                native += re.findall(r'AbilityDesigns\.(\w+)', call[1])
                native += re.findall(r',\s*([A-Z][A-Z_]+)\s*,', call[1])
            if 'feedback.spear(' in text:
                native.append('공유 창 궤적')
            if 'createExplosion(' in text:
                native.append('실제 폭발')
            if 'strikeLightning(' in text:
                native.append('실제 낙뢰')
            if re.search(r'\bflight\(|\.setFlying\(true\)', text) or 'WINGS' in cues:
                flight = re.search(r'\.flight\(([^)]+)\)', style[2])
                native.append(flight[1] if flight else '공유 날개')
            if 'feedback.particle(' in text or 'feedback.segment(' in text:
                native.append('전용 입자: '+', '.join(name for name in names if 'feedback.particle(' in bodies[name]['body'] or 'feedback.segment(' in bodies[name]['body']))
            paths[phase] = {'skill': metadata.get(skill, '없음'), 'handlers': names,
                            'sourceCues': sorted(cues), 'sourceModels': sorted({declarations[c] for c in cues if c in declarations}),
                            'nativeModels': sorted(set(native))}
        routes = []
        for name, method in bodies.items():
            for match in re.finditer(r'(?:feedback\.(?:\w+)|confirmedAttack|confirmedDamageImpact|appliedEffect|effect|ignite|restoreHealthApplied|restoreHealth|heal|damage|suppressAbility)\([^;]*;', method['body'], re.S):
                routes.append({'handler': name, 'line': method['line']+method['body'].count('\n', 0, match.start()),
                               'call': ' '.join(match[0].split())})
        rows.append({**metadata, 'file': path.relative_to(ROOT).as_posix(),
                     'sha256': hashlib.sha256(source.encode()).hexdigest(), 'theme': style[1],
                     'private': '.privateCast()' in style[2], 'dedicated': '.dedicated()' in style[2],
                     'paths': paths, 'sourceEffects': declarations, 'receivedEffects': recipients,
                     'unroutedSourceEffects': {c: m for c, m in declarations.items() if c not in used},
                     'calls': routes})
    return sorted(rows, key=lambda row: row['id'])


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--output', default='output/ability-redesign/ability-audit.json')
    parser.add_argument('--check', action='store_true')
    args = parser.parse_args()
    rows = audit()
    assert len(rows) == 93, f'Expected all 93 abilities, found {len(rows)}'
    orphans = [(r['id'], r['unroutedSourceEffects']) for r in rows if r['unroutedSourceEffects']]
    destination = ROOT / args.output
    destination.parent.mkdir(parents=True, exist_ok=True)
    destination.write_text(json.dumps({'scope': 'source handler trace; runtime evidence is separate',
                                       'abilities': rows, 'unrouted': orphans}, ensure_ascii=False, indent=2), encoding='utf-8')
    print(f'{len(rows)} abilities traced, {len(orphans)} source-route candidates need review')
    for ability, effects in orphans:
        print(ability, effects)
    if args.check and orphans:
        raise SystemExit(1)


if __name__ == '__main__':
    main()
