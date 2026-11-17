const {test} = require('node:test');
const assert = require('node:assert/strict');
const vm = require('node:vm');
const fs = require('node:fs');
const files = require('../../main/resources/web/game-file.js');
const source = fs.readFileSync('src/main/resources/web/app.js', 'utf8');
function boot(saved, {blocked = false, reject = false, pending = false} = {}) {
  const store = new Map(saved ? [['caroai.game.v1', saved]] : []);
  const nodes = new Map();
  const node = id => {
    if (!nodes.has(id)) nodes.set(id, new Proxy({textContent: '', style: {}, dataset: {}, elements: {},
      getBoundingClientRect: () => ({width: 800, height: 600}),
      getContext: () => new Proxy({}, {get: () => () => {}}),
    }, {get: (o, k) => k in o ? o[k] : () => {}}));
    return nodes.get(id);
  };
  const calls = [];
  const context = vm.createContext({document: {getElementById: node, documentElement: {dataset: {}}, createElement: node},
    window: {innerWidth: 800}, localStorage: {
      getItem: k => {if (blocked) throw Error('blocked'); return store.get(k) || null;},
      setItem: (k,v) => {if (blocked) throw Error('quota'); store.set(k,v);}},
    CaroFiles: files, CaroSound: new Proxy({}, {get: () => () => {}}),
    ResizeObserver: class {observe() {}}, AbortController, URLSearchParams, setTimeout: (...args) => setTimeout(...args).unref(), clearTimeout,
    fetch: async (url, options) => {
      const body = options.body; calls.push(body);
      if (pending) return new Promise(() => {});
      if (reject) return {ok:false, json:async () => ({error:'Lịch sử không hợp lệ'})};
      const moves = body.get('moves') ? body.get('moves').split(';').map(c => c.split(',').map(Number)) : [];
      if (body.get('action') === 'reply' && body.get('mode') === 'ai' && moves.length % 2 === (body.get('human') === 'X' ? 1 : 0)) moves.push([24,24]);
      return {ok:true, json:async () => ({moves, winningLine:[], winner:null, draw:false})};
    }});
  vm.runInContext(source, context);
  return {store, nodes, calls, run: code => vm.runInContext(code, context)};
}
const cfg = {mode:'ai', human:'X', difficulty:'HARD'};
const flush = () => new Promise(resolve => setImmediate(resolve));
test('reload validates saved history before resuming a pending AI reply', async () => {
  const app = boot(files.serialize(cfg, [[25,25]]));
  assert.equal(app.calls[0].get('action'), 'state');
  await flush();
  assert.deepEqual(app.calls.map(c => c.get('action')), ['state','reply']);
  assert.equal(files.parse(app.store.get('caroai.game.v1')).moves.length, 2);
});
test('local game restores without an AI reply', async () => {
  const app = boot(files.serialize({...cfg,mode:'local'}, [[25,25]])); await flush();
  assert.equal(app.calls.length, 1); assert.equal(app.run('config.mode'), 'local');
});
test('saves human move before server reply and saves undo while offline', async () => {
  const app = boot(null, {pending:true});
  app.run('cancelRequest(); play(25,25)');
  assert.equal(files.parse(app.store.get('caroai.game.v1')).moves.length, 1);
  app.run('undo()');
  assert.equal(files.parse(app.store.get('caroai.game.v1')).moves.length, 0);
});
test('blocked storage keeps play working and reports export fallback', async () => {
  const app = boot(null, {blocked:true}); await flush();
  app.run('play(25,25)'); await flush();
  assert.equal(app.run('game.moves.length'), 2);
  assert.match(app.nodes.get('file-status').textContent, /Không thể tự lưu/);
});
test('invalid JSON falls back to a new game', async () => {
  const app = boot('{'); await flush(); assert.equal(app.run('game.moves.length'), 0);
});
test('server-rejected history is retained and cannot trigger an AI reply', async () => {
  const saved = files.serialize(cfg, [[25,25]]);
  const app = boot(saved, {reject:true}); await flush();
  assert.equal(app.calls.length, 1); assert.equal(app.store.get('caroai.game.v1'), saved);
  assert.match(app.run('failure'), /không hợp lệ/);
});
