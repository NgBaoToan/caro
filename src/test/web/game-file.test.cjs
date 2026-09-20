const {test} = require('node:test');
const assert = require('node:assert/strict');
const files = require('../../main/resources/web/game-file.js');
const config = {mode: 'ai', human: 'O', difficulty: 'HARD'};
const base = () => JSON.parse(files.serialize(config, [[25,25],[26,25]]));

test('round trip preserves every move, mode, side and difficulty', () => {
  for (const mode of ['ai','local']) for (const human of ['X','O']) for (const difficulty of ['EASY','MEDIUM','HARD']) {
    const cfg = {mode, human, difficulty};
    const result = files.parse(files.serialize(cfg, [[0,0],[49,49],[1,0]]));
    assert.deepEqual(result.config, cfg);
    assert.deepEqual(result.moves, [[0,0],[49,49],[1,0]]);
  }
});
test('ignores forged winner and turn; clones data instead of trusting metadata', () => {
  const input = base(); input.winner = 'O'; input.turn = 'X';
  const result = files.parse(JSON.stringify(input));
  assert.equal(result.winner, undefined); assert.equal(result.turn, undefined);
  assert.equal(result.moves.length, 2);
});
test('rejects bad JSON, versions, dimensions and configuration', () => {
  for (const text of ['null','[]','{','"hi"']) assert.throws(() => files.parse(text));
  for (const patch of [{version: 2},{size: 15},{winLength: 6},{format:'other'},{config:{...config,human:'Z'}}]) {
    assert.throws(() => files.parse(JSON.stringify({...base(),...patch})));
  }
});
test('rejects duplicate, fractional, string and out-of-range coordinates', () => {
  for (const moves of [[[0,0],[0,0]], [[-1,0]], [[50,0]], [[1.5,2]], [['1',2]], [[1]], [null]]) {
    assert.throws(() => files.parse(JSON.stringify({...base(), moves})));
  }
});
test('all 2500 coordinates fit within the file size limit', () => {
  const moves = Array.from({length:2500}, (_,i) => [i%50,Math.floor(i/50)]);
  const saved = files.serialize(config,moves);
  assert.ok(Buffer.byteLength(saved) < files.MAX_BYTES);
  assert.equal(files.parse(saved).moves.length,2500);
});
test('limits UTF-8 bytes and accepts a UTF-8 BOM', () => {
  assert.throws(() => files.parse(' '.repeat(files.MAX_BYTES+1)));
  assert.throws(() => files.parse('あ'.repeat(23000)));
  assert.equal(files.parse('\uFEFF'+files.serialize(config,[])).moves.length,0);
});
