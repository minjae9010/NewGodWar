"""Update the repository design table from the auditable handler trace."""
import importlib.util
import json
import re
from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]
spec=importlib.util.spec_from_file_location('presentation_audit', ROOT/'scripts/audit-ability-presentation.py')
audit=importlib.util.module_from_spec(spec)
spec.loader.exec_module(audit)
rows=audit.audit()
definitions=(audit.BUILTIN/'AbilityDesigns.java').read_text(encoding='utf-8')
descriptions=dict(re.findall(r'public static final DesignedEffect\s+(\w+)\s*=\s*\w+\("([^"]+)"',definitions))
descriptions.update({'AEGIS_PLATES':'몸 양옆에서 맞물리는 아이기스 방패','SCOPE':'손 옆에 고정된 조준경 · 준비 표시',
    'BLESSING':'축복의 빛과 작은 성표','DREAM':'대상 머리의 수면 표식','RECORD':'레코드와 음표',
    'FEAST':'요리 그릇과 음식','TRIDENT':'앞으로 찌르는 삼지창과 물결','LAUREL':'머리 위 월계관',
    'BOMB':'설치 위치의 작은 폭탄','BEES':'대상을 추적하는 벌떼','RAVENS':'대상을 추적하는 까마귀',
    'CLOCK':'시간 좌표와 시곗바늘','ABYSS':'실제 추락 지점의 나락 문','HUNT_MARK':'사냥 대상의 누적 표식',
    'PHALANX':'시전 위치·방향에 고정된 방패 진형','HAMMER':'투척·충돌·회수 경로의 묠니르',
    'SCALES':'심판 대상 머리 위 금빛 저울','SharedModels.FIRE_WINGS':'주작의 관절 날개와 긴 꼬리깃',
    'SharedModels.WINGS':'백금 날개','공유 날개':'백금 날개'})


def label(model):
    return descriptions.get(model,model)


def phase(row,kind):
    path=row['paths'][kind]
    if path['skill']=='없음':return '없음'
    models=list(dict.fromkeys(path['sourceModels']+path['nativeModels']))
    parts=[label(model) for model in models]
    if not parts:
        parts=['적용 대상 반응' if row['receivedEffects'] else '적용된 상태 반응·게임 동작']
    handlers=', '.join('`'+name+'`' for name in path['handlers'] if name.startswith('on'))
    return path['skill'].replace('|','/')+'<br>→ '+' / '.join(parts)+'<br>'+handlers


def received(row):
    overrides=[cue+' → '+label(model) for cue,model in row['receivedEffects'].items()]
    if not overrides:return '실제 적용된 상태·회복·타격에 공용 수신 반응'
    return '<br>'.join(overrides)+'<br>나머지는 공용 수신 반응'


header='''## 전체 93개 능력 실행 연결표

등록 목록 대신 실제 이벤트 함수에서 도달하는 연출을 추적했다. `scripts/audit-ability-presentation.py --check`로 93개 누락 여부와 연결되지 않은 시전 모델을 검사한다. 자세한 함수·행 번호는 `output/ability-redesign/ability-audit.json`에 남긴다. 이 정적 추적과 서버·클라이언트 실행 검증은 별개이며 검증 결과는 `output/ability-redesign/verification.json`에 기록한다.

준비 표시는 준비 사건, 공격 동작은 확인된 공격 사건, 수신 반응은 대상에게 실제 적용된 사건에서 실행한다. 강화·저주는 이미 유지 중인 효과보다 강하거나 새로 적용됐을 때 표시한다. 회복은 체력 증가, 수리는 장비 내구도 변경, 정화는 제거할 상태, 부양은 적용된 부양 효과, 봉인은 성공한 봉인 결과를 확인한다. 표시할 사건이 없는 정보 조회와 상시 면역에는 타격을 만들어 붙이지 않는다. 은신 시전은 본인에게만 표시한다.

모든 성공한 일반·고급 발동은 음량·음높이를 구분한 확인음을 1회 낸다. 수신 반응음은 실제 적용 결과와 함께 출력하며 광역의 같은 반응은 청취자별로 묶는다. 아래 음색은 `AbilitySounds`의 계열이며, 모루 3회 접촉·담금질·날갯짓·실제 효과 종료음은 해당 동작에서만 추가한다. 게임 엔진의 폭발·낙뢰 소리도 유지한다.

| 능력 | 일반 실행·연출 | 고급 실행·연출 | 패시브 실행·연출 | 수신 반응 | 음색 |
|---|---|---|---|---|---|
'''
table=header+'\n'.join('| '+row['name']+' (`'+row['id']+'`) | '+phase(row,'normal')+' | '+phase(row,'advanced')+' | '+phase(row,'passive')+' | '+received(row)+' | '+row['theme']+' |' for row in rows)+'\n'
path=ROOT/'docs/wiki/ability-presentation-design.md'
source=path.read_text(encoding='utf-8')
source=source[:source.index('## 전체 93개 능력')]+table
path.write_text(source,encoding='utf-8')
print('Updated 93 actual handler rows in '+str(path))
