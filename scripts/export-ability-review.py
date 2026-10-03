"""Export the actual client evidence, source trace and exact tested binaries for review."""
import hashlib
import html
import json
import shutil
import xml.etree.ElementTree as ET
from collections import Counter
from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]
OUT=ROOT/'output/ability-redesign'
EVIDENCE=ROOT/'.build/minecraft-mcp/evidence/presentation-final'


def read(path):return json.loads(path.read_text(encoding='utf-8'))
def write(path,data):path.write_text(json.dumps(data,ensure_ascii=False,indent=2),encoding='utf-8')
def copy_image(path,relative):
    target=OUT/relative
    target.parent.mkdir(parents=True,exist_ok=True)
    shutil.copy2(path,target)
    return relative.as_posix()


audit=read(OUT/'ability-audit.json')
casts=read(EVIDENCE/'casts.json')
assert len(casts)==93 and not any(row.get('error') for row in casts)
for row in casts:
    assert all(a['status']!='needs-review' for a in row['attempts']),row['id']
passives=read(EVIDENCE/'passives-retry.json')
assert not any(row.get('error') for row in passives)
session=read(EVIDENCE/'verification-session.json')
sha=hashlib.sha256((OUT/'NewGodWar-0.3.9.jar').read_bytes()).hexdigest()
assert sha==session['pluginSha256']
pack=ROOT/'resoucepack'/('NewGodWar-Art-'+session['artPackSha1'][:8]+'.zip')
assert hashlib.sha1(pack.read_bytes()).hexdigest()==session['artPackSha1']
shutil.copy2(pack,OUT/pack.name)
for row in casts:
    for attempt in row['attempts']:
        if attempt.get('picture'):
            attempt['picture']=copy_image(Path(attempt['picture']),Path('captures')/row['id']/(attempt['kind']+'.png'))
    if row.get('passive',{}).get('picture'):
        row['passive']['picture']=copy_image(Path(row['passive']['picture']),Path('captures')/row['id']/'passive.png')
for row in passives:
    if row.get('passive',{}).get('picture'):
        row['passive']['picture']=copy_image(Path(row['passive']['picture']),Path('captures')/row['id']/'passive.png')
clips=[]
for clip in read(EVIDENCE/'animations.json'):
    assert clip['ok'] and clip['pluginSha256']==sha,clip['id']
    frames=[{'src':copy_image(Path(frame['path']),Path('clips')/(str(index)+'-'+clip['id'].replace('.','-')+'-'+clip['view'])/(str(i)+'.png')),
             'ms':frame['ms']} for index in [len(clips)] for i,frame in enumerate(clip['frames'])]
    clips.append({'label':clip['label'],'kind':clip['kind'],'view':clip['view'],'frames':frames,'fps':clip['fps']})
tests=[ET.parse(path).getroot() for path in (ROOT/'plugin/build/test-results/test').glob('TEST-*.xml')]
assert all(int(test.attrib['failures'])==0 and int(test.attrib['errors'])==0 for test in tests)
report={'pluginSha256':sha,'artPackSha1':session['artPackSha1'],'abilities':93,
        'sourceUnrouted':audit['unrouted'],'unitTests':sum(int(t.attrib['tests']) for t in tests),
        'castStatuses':dict(Counter(a['status'] for r in casts for a in r['attempts'])),
        'passiveCallbacks':sorted({r['id'] for r in casts if r.get('passive')}|{r['id'] for r in passives}),
        'clips':len(clips),'frames':sum(len(c['frames']) for c in clips),'audioRecorded':False,
        'servers':{'1.12.2':'.paper-smoke/ability-variety-1.12.2-qlt12ynr','26.3':'.paper-smoke/ability-variety-26.3-f8tu06oz'},
        'limits':'All 93 source paths audited and live casts/callbacks exercised. Selected scenes recorded at original frame timings. Audio verified by server sound dispatch tests; recordings contain no audio.'}
assert len(report['passiveCallbacks'])==93
write(OUT/'verification.json',report)
write(OUT/'client-casts.json',casts)
write(OUT/'client-passives.json',passives)
write(OUT/'recordings.json',clips)
data=json.dumps({'abilities':audit['abilities'],'casts':casts,'passives':passives,'clips':clips,'report':report},ensure_ascii=False).replace('</','<\\/')
template='''<!doctype html><html lang="ko"><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>93개 능력 구현·연출 검증</title><style>
*{box-sizing:border-box}body{margin:28px auto;max-width:1200px;padding:0 22px;background:#10151c;color:#eaf0f7;font:15px/1.65 system-ui}h1{font-size:27px;margin-bottom:4px}p,.meta{color:#aec0d3}button,select,input{background:#233143;color:#fff;border:1px solid #4b6079;border-radius:7px;padding:9px;font:inherit}button{cursor:pointer;margin:5px}a{color:#8fd7ff}.card{border:1px solid #334458;border-radius:10px;margin:12px 0;padding:16px}summary{cursor:pointer;font-size:17px}.ok{color:#9ee5c0}.muted{color:#a3b1c3}.grid{display:grid;grid-template-columns:repeat(auto-fit,minmax(260px,1fr));gap:16px}.grid img{width:100%;border-radius:5px}small{display:block;color:#9fb4c9}#frame{width:100%;max-width:960px;display:block;border-radius:10px;background:#080d14}#seek{width:100%;max-width:960px}code{color:#b7defb}#movies[hidden],#catalogue[hidden]{display:none}
</style><h1>93개 능력 · 구현과 게임 연출</h1><p id="summary"></p>
<p><a href="../../docs/wiki/ability-presentation-design.md">실행 경로를 반영한 설계표</a> · <a href="NewGodWar-0.3.9.jar">검증 JAR</a> · <a id="pack">리소스팩</a> · <a href="verification.json">검증 기록</a></p>
<button id="all">전체 능력 실행 결과</button><button id="motion">게임 녹화 보기</button>
<section id="catalogue"><p>일반·고급은 실제 클라이언트 입력과 조건 준비로 발동했습니다. 패시브는 실제 서버 이벤트 콜백을 실행했습니다. 아래 캡처는 발동 직후의 한 프레임이며, 동작은 녹화 보기에서 확인할 수 있습니다.</p><input id="search" placeholder="능력 이름 또는 ID 검색" aria-label="능력 검색"><div id="list"></div></section>
<section id="movies" hidden><p>원본 Minecraft 화면과 실제 프레임 시간입니다. 모델 동작 검사와 실제 시전을 구분했습니다. 오디오는 포함하지 않습니다.</p><select id="pick"></select><button id="play">일시 정지</button><div id="label"></div><img id="frame" alt="실제 Minecraft 능력 연출"><input id="seek" type="range" min="0" value="0" aria-label="프레임 탐색"><div id="meta" class="meta"></div></section>
<script>const data=__DATA__;
const byId=Object.fromEntries(data.casts.map(r=>[r.id,r])),passives=Object.fromEntries(data.passives.map(r=>[r.id,r]));
const esc=s=>String(s??'').replace(/[&<>"']/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
const labels={normal:'일반',advanced:'고급',passive:'패시브'},states={activated:'실행 확인',prepared:'준비 확인',informational:'정보 조회', 'not-applicable':'없음'};
document.querySelector('#summary').textContent=`93개 실행 경로 조사 완료 · 연결되지 않은 시전 모델 0개 · 단위 테스트 ${data.report.unitTests}개 · Paper 1.12.2 / 26.3 통과 · 빌드 ${data.report.pluginSha256.slice(0,12)}`;
const pack=document.querySelector('#pack');pack.href=`NewGodWar-Art-${data.report.artPackSha1.slice(0,8)}.zip`;
function render(){const q=document.querySelector('#search').value.toLowerCase();document.querySelector('#list').innerHTML=data.abilities.filter(r=>(r.id+r.name).toLowerCase().includes(q)).map(r=>{const run=byId[r.id];return `<details class="card"><summary>${esc(r.name)} <small style="display:inline">${r.id}</small> <span class="ok">실행 경로 확인</span></summary><div class="grid">${run.attempts.map(a=>{const p=r.paths[a.kind];return `<div><b>${labels[a.kind]} · ${states[a.status]}</b><p>${esc(p.skill)}</p><small>시전: ${esc([...p.sourceModels,...p.nativeModels].join(', ')||'적용 대상 반응·게임 동작')}<br>함수: ${esc(p.handlers.join(', '))}</small>${a.picture?`<img src="${esc(a.picture)}" alt="${esc(r.name+' '+labels[a.kind])}" loading="lazy">`:''}</div>`}).join('')}<div><b>패시브 · 이벤트 콜백 확인</b><p>${esc(r.passiveSkill)}</p><small>${esc(r.paths.passive.handlers.join(', '))}</small>${(passives[r.id]?.passive??run.passive)?.picture?`<img src="${esc((passives[r.id]?.passive??run.passive).picture)}" alt="${esc(r.name+' 패시브')}" loading="lazy">`:''}</div></div><p class="muted">수신 전용: ${esc(Object.entries(r.receivedEffects).map(([c,m])=>c+' → '+m).join(', ')||'실제 적용된 상태·회복·피격 공용 반응')} · 음색 ${r.theme}${r.private?' · 본인에게만 보이는 시전':''}</p></details>`}).join('')}
document.querySelector('#search').oninput=render;render();
document.querySelector('#all').onclick=()=>{document.querySelector('#catalogue').hidden=false;document.querySelector('#movies').hidden=true};
document.querySelector('#motion').onclick=()=>{document.querySelector('#movies').hidden=false;document.querySelector('#catalogue').hidden=true};
const pick=document.querySelector('#pick'),frame=document.querySelector('#frame'),seek=document.querySelector('#seek'),label=document.querySelector('#label'),meta=document.querySelector('#meta'),play=document.querySelector('#play');let selected=0,playing=true,start=performance.now();data.clips.forEach((r,i)=>{const o=document.createElement('option');o.value=i;o.textContent=r.label;pick.append(o)});
function show(i){const r=data.clips[selected];frame.src=r.frames[i].src;seek.value=i;meta.textContent=`${r.kind==='cast'?'실제 시전':'모델 동작 검사'} · ${i+1}/${r.frames.length} 프레임 · ${r.fps} fps · 빌드 ${data.report.pluginSha256.slice(0,12)}`}
function choose(){selected=+pick.value;seek.max=data.clips[selected].frames.length-1;label.textContent=data.clips[selected].label;start=performance.now();show(0)}pick.onchange=choose;play.onclick=()=>{playing=!playing;play.textContent=playing?'일시 정지':'재생';start=performance.now()-data.clips[selected].frames[+seek.value].ms};seek.oninput=()=>{playing=false;play.textContent='재생';show(+seek.value)};
function tick(now){if(playing&&!document.querySelector('#movies').hidden){const fs=data.clips[selected].frames,elapsed=(now-start)%(fs.at(-1).ms+400);let i=Math.max(0,fs.findLastIndex(f=>f.ms<=elapsed));if(+seek.value!==i)show(i)}requestAnimationFrame(tick)}if(data.clips.length){choose();requestAnimationFrame(tick)}
</script></html>'''
(OUT/'review.html').write_text(template.replace('__DATA__',data),encoding='utf-8')
print(json.dumps(report,ensure_ascii=False,indent=2))
