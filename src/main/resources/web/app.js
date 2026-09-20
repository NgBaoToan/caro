'use strict';
const $ = id => document.getElementById(id);
const canvas = $('board');
const ctx = canvas.getContext('2d');
const size = 50;
let config = {mode: 'ai', human: 'X', difficulty: 'MEDIUM'};
let game = emptyGame();
let busy = false, failure = '', requestVersion = 0, controller;
let hint = null, busyAction = '', retryAction = 'reply';
let width = 0, height = 0, cell = window.innerWidth < 650 ? 28 : 32;
let camera = {col: 25, row: 25}, hover = null, cursor = [25, 25], drag = null;
const levels = {EASY: 'DỄ', MEDIUM: 'VỪA', HARD: 'KHÓ'};
let theme = 'dark', stoneStyle = 'round';
try {
  theme = localStorage.getItem('caroai.theme') === 'light' ? 'light' : 'dark';
  const saved = localStorage.getItem('caroai.stoneStyle');
  if (['round', 'sharp', 'neon', 'badge'].includes(saved)) stoneStyle = saved;
} catch { /* Preferences are optional. */ }
function applyAppearance() {
  document.documentElement.dataset.theme = theme;
  $('theme').textContent = `Giao diện: ${theme === 'dark' ? 'Tối' : 'Sáng'}`;
  $('theme').setAttribute('aria-pressed', String(theme === 'dark'));
  $('stone-style').value = stoneStyle;
  try { localStorage.setItem('caroai.theme', theme); localStorage.setItem('caroai.stoneStyle', stoneStyle); } catch {}
  draw();
}
$('theme').addEventListener('click', () => { theme = theme === 'dark' ? 'light' : 'dark'; applyAppearance(); });
$('stone-style').addEventListener('change', event => { stoneStyle = event.target.value; applyAppearance(); });
function emptyGame() { return {moves: [], turn: 'X', winner: null, draw: false, winningLine: [], aiMillis: 0}; }
function turn() { return game.moves.length % 2 ? 'O' : 'X'; }
function ended() { return Boolean(game.winner || game.draw); }
function occupied(col, row) { return game.moves.some(c => c[0] === col && c[1] === row); }
function humanIndex() {
  if (config.mode === 'local') return game.moves.length - 1;
  for (let i = game.moves.length - 1; i >= 0; i--) if ((i % 2 ? 'O' : 'X') === config.human) return i;
  return -1;
}
function cancelRequest() { requestVersion++; controller?.abort(); busy = false; busyAction = ''; }
function clearHint() { hint = null; $('hint-status').textContent = ''; }
async function requestGame(settings, moves, action, signal) {
  const body = new URLSearchParams({...settings, action, moves: moves.map(c => c.join(',')).join(';')});
  const response = await fetch('/api/game', {method: 'POST', body, signal});
  const data = await response.json();
  if (!response.ok) throw new Error(data.error || 'Không kết nối được với máy chủ.');
  return data;
}
async function synchronize(action = 'reply') {
  cancelRequest();
  const version = requestVersion;
  controller = new AbortController();
  const signal = controller.signal;
  const timeout = setTimeout(() => controller?.signal === signal && controller.abort('timeout'), 15000);
  const previousCount = game.moves.length;
  busy = true; busyAction = action; retryAction = action; failure = ''; render();
  try {
    const data = await requestGame(config, game.moves, action, signal);
    if (version !== requestVersion) return;
    busy = false; busyAction = '';
    if (action === 'hint') {
      game.evaluation = data.evaluation;
      hint = data.hint;
      $('hint-status').textContent = hint ? `Gợi ý: cột ${hint[0] + 1}, hàng ${hint[1] + 1}. Bấm ô khoanh để đặt quân.` : 'Ván này đã kết thúc.';
      if (hint) { cursor = [...hint]; reveal(hint); }
    } else {
      game = data;
      if (action === 'reply') {
        if (game.winner) CaroSound.play(config.mode === 'ai' && game.winner !== config.human ? 'lose' : 'win');
        else if (game.draw) CaroSound.play('draw');
        else if (game.moves.length > previousCount) CaroSound.play('reply');
      }
      if (game.moves.length) reveal(game.moves.at(-1));
    }
    render();
  } catch (error) {
    if (version !== requestVersion) return;
    busy = false; busyAction = '';
    failure = signal.aborted ? 'Kết nối quá lâu. Hãy thử lại.' : error.message;
    render();
  } finally { clearTimeout(timeout); }
}
function play(col, row) {
  if (busy || failure || ended() || col < 0 || row < 0 || col >= size || row >= size || occupied(col, row)) return;
  if (config.mode === 'ai' && turn() !== config.human) return;
  CaroSound.unlock(); clearHint();
  game.moves.push([col, row]);
  hover = null; cursor = [col, row];
  synchronize();
  CaroSound.play('move');
}
function undo() {
  const index = humanIndex();
  if (index < 0) return;
  cancelRequest();
  CaroSound.stop(); clearHint();
  game = {...emptyGame(), moves: game.moves.slice(0, index)};
  synchronize('state');
}
function render() {
  renderEvaluation();
  const current = turn();
  const aiThinking = busy && config.mode === 'ai' && current !== config.human;
  const you = config.mode === 'ai' && current === config.human;
  $('status').textContent = failure ? 'Mất nhịp một chút' : game.winner
    ? (config.mode === 'ai' ? (game.winner === config.human ? 'Bạn thắng rồi!' : 'CaroAI thắng') : `Quân ${game.winner} thắng!`)
    : game.draw ? 'Ván cờ hòa' : busyAction === 'hint' ? 'Đang tìm gợi ý…' : aiThinking ? 'Máy đang nghĩ…' : busy ? 'Đang kiểm tra…' : you ? 'Đến lượt bạn' : `Lượt quân ${current}`;
  $('status-detail').textContent = ended() ? 'Thử một ván mới nhé?' : config.mode === 'ai'
    ? `Bạn cầm ${config.human} · ${config.human === 'X' ? 'Đi trước' : 'Đi sau'}` : 'Hai người · Cùng một bàn cờ';
  $('turn-symbol').textContent = (game.winner || current) === 'X' ? '×' : '○';
  for (const side of ['X', 'O']) {
    const prefix = side.toLowerCase();
    $(prefix + '-name').textContent = config.mode === 'ai' ? (side === config.human ? 'Bạn' : 'CaroAI') : `Người chơi ${side}`;
    $(prefix + '-status').textContent = ended() ? (game.winner === side ? 'Chiến thắng' : 'Kết thúc') : current === side ? 'Đang đi' : 'Chờ lượt';
  }
  $('mode-label').textContent = $('mode-detail').textContent = config.mode === 'ai' ? 'Đấu với máy' : 'Hai người';
  $('difficulty-tag').textContent = config.mode === 'ai' ? levels[config.difficulty] : 'LOCAL';
  $('move-count').textContent = String(game.moves.length).padStart(2, '0');
  $('ai-time').textContent = game.aiMillis ? `${(game.aiMillis / 1000).toFixed(2)} giây` : '—';
  $('undo').disabled = humanIndex() < 0;
  $('hint').disabled = busy || Boolean(failure) || ended() || (config.mode === 'ai' && current !== config.human);
  $('save-game').disabled = busy || Boolean(failure);
  $('open-game').disabled = busy;
  $('error').hidden = !failure;
  $('error-message').textContent = failure;
  $('history').replaceChildren();
  if (!game.moves.length) $('history').textContent = 'Bàn cờ đang chờ nước đi đầu tiên.';
  game.moves.slice(-6).forEach((c, i) => {
    const index = Math.max(0, game.moves.length - 6) + i;
    const chip = document.createElement('span');
    chip.className = 'history-chip' + (index % 2 ? ' o' : '');
    chip.textContent = `${index % 2 ? '○' : '×'} ${c[0] + 1} · ${c[1] + 1}`;
    $('history').append(chip);
  });
  draw();
}
function renderEvaluation() {
  const evaluation = game.evaluation;
  const pending = busy || Boolean(failure) || !evaluation;
  $('advantage').hidden = pending;
  $('eval-x').textContent = pending ? 'X · —' : `X · ${evaluation.xPercent}%`;
  $('eval-o').textContent = pending ? 'O · —' : `O · ${100 - evaluation.xPercent}%`;
  if (pending) {
    $('eval-status').textContent = failure ? 'Chưa đánh giá được' : 'Đang cập nhật…';
    $('eval-detail').textContent = '';
    return;
  }
  const x = evaluation.xPercent;
  $('advantage').value = x;
  const side = x > 50 ? 'X' : 'O';
  $('eval-status').textContent = game.winner ? `${game.winner} đã thắng` : game.draw ? 'Hòa cờ'
    : Math.abs(x - 50) < 5 ? 'Thế cờ cân bằng' : `${side} ${Math.abs(x - 50) >= 25 ? 'áp đảo' : 'nhỉnh hơn'}`;
  const reasons = {
    'x-can-win': 'X có nước thắng ngay trong lượt này.', 'o-can-win': 'O có nước thắng ngay trong lượt này.',
    'x-double-threat': 'O bất lợi: X có hai ô có thể hoàn thành năm quân.',
    'o-double-threat': 'X bất lợi: O có hai ô có thể hoàn thành năm quân.',
    'x-must-block': 'X cần chặn ngay đường thắng của O.', 'o-must-block': 'O cần chặn ngay đường thắng của X.'
  };
  $('eval-detail').textContent = reasons[evaluation.reason] || (ended() ? 'Ván cờ đã kết thúc.'
    : Math.abs(x - 50) < 5 ? 'Chưa bên nào có lợi thế rõ rệt.' : `${side === 'X' ? 'O' : 'X'} đang bất lợi hơn về thế cờ.`);
}
function point(col, row) { return {x: width / 2 + (col - camera.col) * cell, y: height / 2 + (row - camera.row) * cell}; }
function boardPoint(x, y) { return [Math.round(camera.col + (x - width / 2) / cell), Math.round(camera.row + (y - height / 2) / cell)]; }
function clampCamera() { camera.col = Math.max(0, Math.min(size - 1, camera.col)); camera.row = Math.max(0, Math.min(size - 1, camera.row)); }
function reveal(c) {
  const p = point(...c);
  if (p.x < cell || p.x > width - cell || p.y < cell || p.y > height - cell) camera = {col: c[0], row: c[1]};
}
function stone(c, symbol, alpha = 1) {
  const p = point(...c), radius = cell * .27;
  ctx.save();ctx.globalAlpha = alpha;
  const ink = symbol === 'X' ? (theme === 'dark' ? '#edf3fc' : '#315c4c') : (theme === 'dark' ? '#f4bb75' : '#a44c28');
  if (stoneStyle === 'badge') {
    ctx.fillStyle = theme === 'dark' ? '#202b3a' : '#fffaf0';
    ctx.shadowColor = '#0008'; ctx.shadowBlur = 4; ctx.shadowOffsetY = 2;
    ctx.beginPath(); ctx.arc(p.x, p.y, cell * .41, 0, Math.PI * 2); ctx.fill();
    ctx.shadowBlur = 0; ctx.shadowOffsetY = 0;
  }
  ctx.strokeStyle = ink; ctx.lineWidth = Math.max(2, cell * .085);
  ctx.lineCap = stoneStyle === 'sharp' ? 'square' : 'round';
  if (stoneStyle === 'neon') { ctx.shadowColor = ink; ctx.shadowBlur = cell * .3; }
  ctx.beginPath();
  if (symbol === 'X') { ctx.moveTo(p.x-radius,p.y-radius);ctx.lineTo(p.x+radius,p.y+radius);ctx.moveTo(p.x+radius,p.y-radius);ctx.lineTo(p.x-radius,p.y+radius); }
  else ctx.arc(p.x,p.y,radius,0,Math.PI*2);
  ctx.stroke();ctx.restore();
}
function draw() {
  ctx.clearRect(0,0,width,height);
  ctx.fillStyle = theme === 'dark' ? '#263140' : '#f0e9d9';ctx.fillRect(0,0,width,height);
  const first = point(0,0), last = point(size-1,size-1);
  ctx.fillStyle=theme === 'dark' ? '#354253' : '#f6f0e2';ctx.fillRect(first.x-cell/2,first.y-cell/2,size*cell,size*cell);
  ctx.strokeStyle=theme === 'dark' ? '#58677b' : '#d5cbbb';ctx.lineWidth=1;ctx.beginPath();
  for(let i=0;i<=size;i++) {
    const x=first.x+(i-.5)*cell,y=first.y+(i-.5)*cell;
    if(x>=0&&x<=width){ctx.moveTo(Math.round(x)+.5,Math.max(0,first.y-cell/2));ctx.lineTo(Math.round(x)+.5,Math.min(height,last.y+cell/2));}
    if(y>=0&&y<=height){ctx.moveTo(Math.max(0,first.x-cell/2),Math.round(y)+.5);ctx.lineTo(Math.min(width,last.x+cell/2),Math.round(y)+.5);}
  }ctx.stroke();
  ctx.fillStyle='#bcbaa2';
  for(let col=5;col<size;col+=10)for(let row=5;row<size;row+=10){const p=point(col,row);ctx.beginPath();ctx.arc(p.x,p.y,2,0,Math.PI*2);ctx.fill();}
  if(game.moves.length){const p=point(...game.moves.at(-1));ctx.fillStyle=theme === 'dark' ? '#566846' : '#d8dfca';ctx.fillRect(p.x-cell/2+1,p.y-cell/2+1,cell-2,cell-2);}
  for(const c of game.winningLine){const p=point(...c);ctx.fillStyle=theme === 'dark' ? '#637d3d' : '#d5dfb0';ctx.fillRect(p.x-cell/2+1,p.y-cell/2+1,cell-2,cell-2);}
  game.moves.forEach((c,i)=>stone(c,i%2?'O':'X'));
  if(hint){const p=point(...hint);ctx.save();ctx.strokeStyle='#b88628';ctx.lineWidth=2;ctx.setLineDash([4,3]);ctx.beginPath();ctx.arc(p.x,p.y,cell*.4,0,Math.PI*2);ctx.stroke();ctx.restore();}
  if(hover&&!busy&&!failure&&!ended()&&(config.mode==='local'||turn()===config.human)&&!occupied(...hover))stone(hover,turn(),.28);
  if(document.activeElement===canvas){const p=point(...cursor);ctx.strokeStyle='#8b926c';ctx.lineWidth=1.5;ctx.strokeRect(p.x-cell/2+3,p.y-cell/2+3,cell-6,cell-6);}
  $('position').textContent=`CỘT ${Math.round(camera.col)+1} · HÀNG ${Math.round(camera.row)+1}`;
  $('zoom-out').disabled=cell<=20;$('zoom-in').disabled=cell>=64;
}
function resize(){const box=canvas.getBoundingClientRect();width=box.width;height=box.height;const ratio=window.devicePixelRatio||1;canvas.width=Math.round(width*ratio);canvas.height=Math.round(height*ratio);ctx.setTransform(ratio,0,0,ratio,0,0);draw();}
function zoom(delta){cell=Math.max(20,Math.min(64,cell+delta));draw();}
canvas.addEventListener('pointerdown',event=>{if(!event.isPrimary||event.button!==0)return;canvas.focus({preventScroll:true});canvas.setPointerCapture(event.pointerId);drag={id:event.pointerId,x:event.clientX,y:event.clientY,col:camera.col,row:camera.row,moved:false};});
canvas.addEventListener('pointermove',event=>{
  const box=canvas.getBoundingClientRect();
  if(drag&&drag.id===event.pointerId){const dx=event.clientX-drag.x,dy=event.clientY-drag.y;if(Math.hypot(dx,dy)>6)drag.moved=true;if(drag.moved){camera={col:drag.col-dx/cell,row:drag.row-dy/cell};clampCamera();hover=null;draw();return;}}
  const c=boardPoint(event.clientX-box.left,event.clientY-box.top);hover=c.every(v=>v>=0&&v<size)?c:null;draw();
});
canvas.addEventListener('pointerup',event=>{if(!drag||drag.id!==event.pointerId)return;const moved=drag.moved;drag=null;if(canvas.hasPointerCapture(event.pointerId))canvas.releasePointerCapture(event.pointerId);if(!moved){const box=canvas.getBoundingClientRect();play(...boardPoint(event.clientX-box.left,event.clientY-box.top));}});
canvas.addEventListener('pointercancel',()=>{drag=null;hover=null;draw();});
canvas.addEventListener('lostpointercapture',()=>{drag=null;});
canvas.addEventListener('pointerleave',()=>{hover=null;draw();});
canvas.addEventListener('wheel',event=>{event.preventDefault();zoom(event.deltaY<0?2:-2);},{passive:false});
canvas.addEventListener('keydown',event=>{
  const key=event.key.toLowerCase();
  const arrows={arrowleft:[-1,0],arrowright:[1,0],arrowup:[0,-1],arrowdown:[0,1]};
  const pans={a:[-2,0],d:[2,0],w:[0,-2],s:[0,2]};
  if(arrows[key]){event.preventDefault();cursor=cursor.map((v,i)=>Math.max(0,Math.min(size-1,v+arrows[key][i])));hover=cursor;reveal(cursor);draw();}
  else if(pans[key]){event.preventDefault();camera.col+=pans[key][0];camera.row+=pans[key][1];clampCamera();draw();}
  else if(key==='enter'||key===' '){event.preventDefault();play(...cursor);}
});
canvas.addEventListener('focus',draw);canvas.addEventListener('blur',draw);
$('undo').addEventListener('click',undo);
$('retry').addEventListener('click',()=>synchronize(retryAction));
$('hint').addEventListener('click',()=>{clearHint();synchronize('hint');});
$('zoom-in').addEventListener('click',()=>zoom(4));$('zoom-out').addEventListener('click',()=>zoom(-4));
$('center').addEventListener('click',()=>{const c=game.moves.at(-1)||[25,25];camera={col:c[0],row:c[1]};cursor=c;draw();});
const settings=$('settings-dialog'), form=$('settings-form');
function updateOptions(){const ai=form.elements.mode.value==='ai';$('side-options').hidden=!ai;$('difficulty-options').hidden=!ai;}
$('new-game').addEventListener('click',()=>{for(const [key,value]of Object.entries(config))form.elements[key].value=value;updateOptions();settings.showModal();});
$('close-settings').addEventListener('click',()=>settings.close());form.addEventListener('change',updateOptions);
form.addEventListener('submit',event=>{event.preventDefault();cancelRequest();CaroSound.stop();CaroSound.unlock();clearHint();config={mode:form.elements.mode.value,human:form.elements.human.value,difficulty:form.elements.difficulty.value};game=emptyGame();failure='';camera={col:25,row:25};cursor=[25,25];hover=null;settings.close();synchronize();});
$('help').addEventListener('click',()=>$('help-dialog').showModal());$('close-help').addEventListener('click',()=>$('help-dialog').close());
function updateSoundButton() {
  $('sound').textContent = `Âm thanh: ${CaroSound.isEnabled() ? 'Bật' : 'Tắt'}`;
  $('sound').setAttribute('aria-pressed', String(CaroSound.isEnabled()));
}
$('sound').addEventListener('click', () => { CaroSound.toggle(); updateSoundButton(); });
updateSoundButton();
$('save-game').addEventListener('click', () => {
  if (busy || failure) return;
  const blob = new Blob([CaroFiles.serialize(config, game.moves)], {type: 'application/json'});
  const url = URL.createObjectURL(blob), link = document.createElement('a');
  link.href = url; link.download = `caroai-${new Date().toISOString().replace(/[:.]/g, '-')}.json`;
  document.body.append(link); link.click(); link.remove();
  setTimeout(() => URL.revokeObjectURL(url), 30000);
  $('file-status').textContent = 'Đã yêu cầu tải tệp JSON. Kiểm tra mục Tải xuống của trình duyệt.';
});
const loadDialog = $('load-dialog');
$('open-game').addEventListener('click', () => {
  if (busy) return;
  $('load-form').reset(); $('load-error').textContent = ''; loadDialog.showModal();
});
function closeLoad() {
  if (busyAction === 'load') { cancelRequest(); render(); }
  loadDialog.close();
}
$('close-load').addEventListener('click', closeLoad);
loadDialog.addEventListener('cancel', event => { event.preventDefault(); closeLoad(); });
$('load-form').addEventListener('submit', async event => {
  event.preventDefault();
  if (busy) return;
  const file = $('game-file').files[0];
  if (!file) return;
  cancelRequest();
  const version = requestVersion;
  controller = new AbortController();
  const signal = controller.signal;
  const timeout = setTimeout(() => controller?.signal === signal && controller.abort('timeout'), 15000);
  busy = true; busyAction = 'load'; $('load-submit').disabled = true; $('load-error').textContent = ''; render();
  try {
    if (file.size > CaroFiles.MAX_BYTES) throw new Error('Tệp vượt quá 64 KB.');
    const candidate = CaroFiles.parse(await file.text());
    if (version !== requestVersion) return;
    const validated = await requestGame(candidate.config, candidate.moves, 'state', signal);
    if (version !== requestVersion) return;
    // Commit only after the complete history has passed server validation.
    config = candidate.config; game = validated; failure = ''; busy = false; busyAction = '';
    clearHint(); CaroSound.stop(); hover = null;
    const last = game.moves.at(-1) || [25,25];
    camera = {col: last[0], row: last[1]}; cursor = [...last];
    loadDialog.close(); $('file-status').textContent = `Đã mở ván có ${game.moves.length} nước đi.`; render();
    if (config.mode === 'ai' && !ended() && turn() !== config.human) synchronize();
  } catch (error) {
    if (version !== requestVersion) return;
    busy = false; busyAction = '';
    $('load-error').textContent = (signal.aborted ? 'Kết nối quá lâu. Hãy thử lại.' : error.message) + ' Ván hiện tại vẫn được giữ nguyên.';
    render();
  } finally { clearTimeout(timeout); $('load-submit').disabled = false; }
});
new ResizeObserver(resize).observe(canvas);
applyAppearance();render();synchronize('state');
