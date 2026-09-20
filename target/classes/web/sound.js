'use strict';
const CaroSound = (() => {
  let context, enabled = true;
  const tap = typeof window.Audio === 'function' ? new window.Audio('/move.wav') : null;
  if (tap) tap.preload = 'auto';
  const active = new Set();
  try { enabled = localStorage.getItem('caroai.sound') !== 'off'; } catch { /* Storage is optional. */ }
  function unlock() {
    if (!enabled) return;
    try {
      context ??= new (window.AudioContext || window.webkitAudioContext)();
      if (context.state === 'suspended') context.resume().catch(() => {});
    } catch { /* Game remains usable without audio support. */ }
  }
  function stop() {
    if (tap) { tap.pause(); tap.currentTime = 0; }
    for (const oscillator of active) { try { oscillator.stop(); } catch {} }
    active.clear();
  }
  function play(kind) {
    if (!enabled) return;
    if ((kind === 'move' || kind === 'reply') && tap) {
      tap.pause(); tap.currentTime = 0;
      tap.play().catch(() => {});
      return;
    }
    if (!enabled || context?.state !== 'running') return;
    const notes = kind === 'win' ? [523, 659, 784] : kind === 'lose' ? [392, 330, 262]
      : kind === 'draw' ? [440, 440] : [kind === 'move' ? 620 : 470];
    const duration = notes.length === 1 ? .075 : .18;
    notes.forEach((frequency, i) => {
      const oscillator = context.createOscillator(), gain = context.createGain();
      const start = context.currentTime + i * .16;
      oscillator.type = 'sine'; oscillator.frequency.value = frequency;
      gain.gain.setValueAtTime(0, start);
      gain.gain.linearRampToValueAtTime(.09, start + .008);
      gain.gain.exponentialRampToValueAtTime(.001, start + duration);
      oscillator.connect(gain); gain.connect(context.destination);
      active.add(oscillator);
      oscillator.onended = () => { active.delete(oscillator); oscillator.disconnect(); gain.disconnect(); };
      oscillator.start(start); oscillator.stop(start + duration + .02);
    });
  }
  function toggle() {
    enabled = !enabled;
    if (!enabled) stop(); else unlock();
    try { localStorage.setItem('caroai.sound', enabled ? 'on' : 'off'); } catch {}
    return enabled;
  }
  return {unlock, play, stop, toggle, isEnabled: () => enabled};
})();
