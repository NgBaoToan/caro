'use strict';
// Only plain, versioned game data crosses the file boundary. The server also
// replays every move to check legality and recomputes turn/winner from history.
const CaroFiles = (() => {
  const MAX_BYTES = 65536;
  function normalize(data) {
    if (!data || data.format !== 'caroai' || data.version !== 1 || data.size !== 50 || data.winLength !== 5) {
      throw new Error('Tệp không phải ván CaroAI phiên bản 1 (bàn 50 × 50).');
    }
    const c = data.config;
    if (!c || !['ai', 'local'].includes(c.mode) || !['X', 'O'].includes(c.human)
        || !['EASY', 'MEDIUM', 'HARD'].includes(c.difficulty)) throw new Error('Cấu hình trong tệp không hợp lệ.');
    if (!Array.isArray(data.moves) || data.moves.length > 2500) throw new Error('Lịch sử ván cờ không hợp lệ.');
    const seen = new Set();
    const moves = data.moves.map(move => {
      if (!Array.isArray(move) || move.length !== 2 || !move.every(n => Number.isInteger(n) && n >= 0 && n < 50)) {
        throw new Error('Tệp chứa tọa độ không hợp lệ.');
      }
      const key = move.join(',');
      if (seen.has(key)) throw new Error('Tệp chứa ô đã được đánh nhiều lần.');
      seen.add(key);
      return [...move];
    });
    return {format: 'caroai', version: 1, size: 50, winLength: 5,
      config: {mode: c.mode, human: c.human, difficulty: c.difficulty}, moves};
  }
  function parse(text) {
    if (new TextEncoder().encode(text).length > MAX_BYTES) throw new Error('Tệp vượt quá 64 KB.');
    let data;
    try { data = JSON.parse(text.replace(/^\uFEFF/, '')); }
    catch { throw new Error('Không đọc được JSON. Hãy chọn tệp đã lưu từ CaroAI.'); }
    return normalize(data);
  }
  function serialize(config, moves) {
    return JSON.stringify(normalize({format: 'caroai', version: 1, size: 50, winLength: 5, config, moves}));
  }
  return {MAX_BYTES, parse, serialize};
})();
if (typeof module !== 'undefined') module.exports = CaroFiles;
