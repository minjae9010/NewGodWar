// Playback of original recorded frames using their capture timestamps. No generated in-between frames.
(() => {
  const source=document.querySelector('#animation-data');if(!source)return;
  const clips=JSON.parse(source.textContent),panel=document.querySelector('#animation');if(!clips.length)return;
  const select=panel.querySelector('select.clip'),speed=panel.querySelector('select.speed');
  const picture=panel.querySelector('img'),play=panel.querySelector('.play'),slider=panel.querySelector('input[type=range]');
  const phase=panel.querySelector('.phase'),clock=panel.querySelector('output'),note=panel.querySelector('.clip-note');
  phase.title='시전 요청 시각을 기준으로 표시하는 대략적인 구간';
  const loop=panel.querySelector('input[type=checkbox]');
  let clip,index=0,elapsed=0,last=0,playing=false,loaded=false,generation=0;
  clips.forEach((c,i)=>{const o=document.createElement('option');o.value=i;o.textContent=(c.kind==='cast'?'실제 발동 · ':'모델 동작 · ')+c.label;select.append(o);});
  function render(){
    const frame=clip.frames[index];picture.src=frame.src;picture.dataset.frame=String(index);
    slider.value=String(index);clock.textContent=(frame.ms/1000).toFixed(2)+'초 / '+(clip.durationMs/1000).toFixed(2)+'초';
    const age=frame.ms-clip.triggerMs;
    phase.textContent=age<0?'발동 전':clip.kind==='cast'?'실제 능력 발동 후':age<220?'등장':age<680?'동작':age<980?'소멸':'소멸 후';
    play.textContent=playing?'❚❚ 일시정지':'▶ 재생';
  }
  function pause(){playing=false;play.textContent='▶ 재생';}
  async function choose(i,autoplay=true){
    pause();loaded=false;const current=++generation;clip=clips[i];select.value=String(i);index=0;elapsed=0;
    slider.max=String(clip.frames.length-1);picture.alt=clip.label+' 실제 게임 연속 촬영';
    note.textContent='원본 프레임을 불러오는 중…';render();
    try{
      await Promise.all(clip.frames.map(f=>new Promise((resolve,reject)=>{const image=new Image();image.onload=resolve;image.onerror=()=>reject(new Error('원본 프레임 로딩 실패'));image.src=f.src;})));
      if(current!==generation)return;loaded=true;
      note.textContent=(clip.kind==='cast'?'실제 능력 발동 장면':'모델만 단독 재생한 장면')+' · '+clip.frames.length+'장 · 촬영 '+clip.fps+' fps · 프레임 보간 없음';
      playing=autoplay;last=performance.now();render();
    }catch(error){if(current===generation)note.textContent=error.message;}
  }
  function tick(now){
    if(playing&&loaded){
      elapsed+=(now-last)*Number(speed.value);
      if(elapsed>clip.durationMs){if(loop.checked)elapsed=0;else{elapsed=clip.durationMs;pause();}}
      let next=0;while(next+1<clip.frames.length&&clip.frames[next+1].ms<=elapsed)next++;
      if(next!==index){index=next;render();}
    }
    last=now;requestAnimationFrame(tick);
  }
  play.addEventListener('click',()=>{if(!loaded)return;if(elapsed>=clip.durationMs)elapsed=0;playing=!playing;last=performance.now();render();});
  slider.addEventListener('input',()=>{pause();index=Number(slider.value);elapsed=clip.frames[index].ms;render();});
  panel.querySelector('.previous').addEventListener('click',()=>{pause();index=Math.max(0,index-1);elapsed=clip.frames[index].ms;render();});
  panel.querySelector('.next').addEventListener('click',()=>{pause();index=Math.min(clip.frames.length-1,index+1);elapsed=clip.frames[index].ms;render();});
  select.addEventListener('change',()=>choose(Number(select.value)));
  document.querySelectorAll('[data-clip]').forEach(button=>button.addEventListener('click',()=>{choose(Number(button.dataset.clip));panel.scrollIntoView({behavior:'smooth',block:'start'});}));
  document.addEventListener('visibilitychange',()=>{if(document.hidden)pause();});
  const initial=clips.findIndex(c=>c.kind==='cast'&&c.id==='chronos'&&c.view==='back-advanced');
  choose(initial>=0?initial:Math.max(0,clips.findIndex(c=>c.kind==='model'&&c.id==='design.BLESSING')));
  requestAnimationFrame(tick);
})();
