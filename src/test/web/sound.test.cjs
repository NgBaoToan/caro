const {test} = require('node:test');
const assert = require('node:assert/strict');
const {readFileSync} = require('node:fs');
const {join} = require('node:path');
const vm = require('node:vm');
const source = readFileSync(join(__dirname, '../../main/resources/web/sound.js'),'utf8');
function setup(saved = null, storageThrows = false, withTap = false) {
  const oscillators = [], contexts = [], store = new Map([['caroai.sound', saved]]);
  class Context {
    constructor() { this.state = 'running'; this.currentTime = 0; contexts.push(this); }
    resume() { this.state = 'running'; return Promise.resolve(); }
    createGain() { return {gain:{setValueAtTime(){},linearRampToValueAtTime(){},exponentialRampToValueAtTime(){}},connect(){},disconnect(){}}; }
    createOscillator() {
      const oscillator = {frequency:{},connect(){},disconnect(){},start(){},stop(){this.stops=(this.stops||0)+1;}};
      oscillators.push(oscillator); return oscillator;
    }
  }
  const taps = [];
  class Audio {
    constructor(src) { this.src=src; this.currentTime=0; this.plays=0; this.pauses=0; taps.push(this); }
    play() { this.plays++; return Promise.resolve(); }
    pause() { this.pauses++; }
  }
  const sandbox = vm.createContext({window:{AudioContext:Context,Audio:withTap ? Audio : undefined},localStorage:{
    getItem(key){if(storageThrows)throw new Error();return store.get(key);},
    setItem(key,value){if(storageThrows)throw new Error();store.set(key,value);}
  }});
  const sound = vm.runInContext(source+'\nCaroSound;',sandbox);
  return {sound,oscillators,contexts,store,taps};
}
test('does not create audio until a gesture unlocks it', () => {
  const {sound,oscillators,contexts} = setup();
  sound.play('move');assert.equal(contexts.length,0);
  sound.unlock();sound.play('move');assert.equal(oscillators.length,1);
});
test('win and loss use different melodies; mute stops scheduled notes and persists', () => {
  const {sound,oscillators,store} = setup();sound.unlock();
  sound.play('win');const win=oscillators.map(o=>o.frequency.value);
  sound.play('lose');const loss=oscillators.slice(3).map(o=>o.frequency.value);
  assert.equal(win.length,3);assert.notDeepEqual(win,loss);
  sound.toggle();assert.equal(store.get('caroai.sound'),'off');
  assert.ok(oscillators.every(o=>o.stops===2));
  sound.play('move');assert.equal(oscillators.length,6);
});
test('saved mute is respected and unavailable storage does not break audio', () => {
  const muted = setup('off');muted.sound.unlock();assert.equal(muted.contexts.length,0);
  muted.sound.toggle();muted.sound.play('move');assert.equal(muted.oscillators.length,1);
  const blocked=setup(null,true);assert.doesNotThrow(()=>{blocked.sound.toggle();blocked.sound.toggle();blocked.sound.play('win');});
});
test('human and AI moves restart the supplied recording; mute stops playback', () => {
  const {sound,taps,oscillators} = setup(null,false,true);
  assert.equal(taps[0].src,'/move.wav');
  sound.unlock(); sound.play('move');
  taps[0].currentTime=.1; sound.play('reply');
  assert.equal(taps[0].plays,2); assert.equal(taps[0].currentTime,0);
  assert.equal(oscillators.length,0);
  sound.toggle(); sound.play('move');
  assert.equal(taps[0].plays,2); assert.ok(taps[0].pauses >= 3);
});
