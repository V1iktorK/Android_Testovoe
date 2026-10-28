#!/usr/bin/env node
// tools/dl.js - отказоустойчивая загрузка больших файлов: диапазоны, повторы, докачка.
// Использование: node tools\dl.js <url> <outFile> [chunkMB]
// Прогресс хранится в <outFile>.parts - повторный запуск докачивает только недостающее.
const https = require('https');
const http = require('http');
const fs = require('fs');

const url = process.argv[2];
const out = process.argv[3];
const chunkMB = parseInt(process.argv[4] || '8', 10);
if (!url || !out) { console.error('usage: node dl.js <url> <outFile> [chunkMB]'); process.exit(2); }
const partsFile = out + '.parts';

function head(u) {
  return new Promise((resolve) => {
    const mod = u.startsWith('https') ? https : http;
    const req = mod.request(u, { method: 'HEAD', timeout: 30000 }, (res) => {
      resolve({ status: res.statusCode, len: parseInt(res.headers['content-length'] || '0', 10), loc: res.headers.location });
    });
    req.on('error', () => resolve({ status: 0, len: 0 }));
    req.on('timeout', () => { req.destroy(); resolve({ status: 0, len: 0 }); });
    req.end();
  });
}

function getRange(u, start, end) {
  return new Promise((resolve, reject) => {
    const mod = u.startsWith('https') ? https : http;
    const req = mod.get(u, { headers: { Range: `bytes=${start}-${end}` }, timeout: 120000 }, (res) => {
      if (res.statusCode !== 206 && res.statusCode !== 200) { res.resume(); return reject(new Error('status ' + res.statusCode)); }
      const chunks = [];
      res.on('data', (c) => chunks.push(c));
      res.on('end', () => resolve(Buffer.concat(chunks)));
      res.on('error', reject);
    });
    req.on('timeout', () => { req.destroy(); reject(new Error('timeout')); });
    req.on('error', reject);
  });
}

async function main() {
  let u = url;
  let h = await head(u);
  for (let i = 0; i < 5 && h.loc; i++) { u = h.loc; h = await head(u); }
  let total = h.len;
  if (!total) throw new Error('не удалось определить размер файла (HEAD/redirect)');
  const chunk = chunkMB * 1024 * 1024;

  let done = {};
  if (fs.existsSync(partsFile)) {
    try { done = JSON.parse(fs.readFileSync(partsFile, 'utf8')); } catch (e) { done = {}; }
  }
  if (fs.existsSync(out) && fs.statSync(out).size === total && Object.keys(done).length * 1 >= 0) {
    const complete = Object.keys(done).length;
    if (complete * chunk >= total) { console.log('уже загружено: ' + out); return; }
  }
  const fd = fs.existsSync(out) ? fs.openSync(out, 'r+') : fs.openSync(out, 'w+');
  try { fs.ftruncateSync(fd, total); } catch (e) { /* размер уже верный */ }
  try {
    for (let start = 0; start < total; start += chunk) {
      const key = String(start);
      const end = Math.min(start + chunk - 1, total - 1);
      if (done[key] === end) continue;
      let ok = false;
      for (let attempt = 1; attempt <= 10 && !ok; attempt++) {
        try {
          const buf = await getRange(u, start, end);
          if (buf.length !== end - start + 1) throw new Error(`короткий ответ ${buf.length}`);
          fs.writeSync(fd, buf, 0, buf.length, start);
          done[key] = end;
          fs.writeFileSync(partsFile, JSON.stringify(done));
          ok = true;
        } catch (e) {
          process.stdout.write(`\nretry ${attempt} @${(start / 1048576).toFixed(1)}MB: ${e.message}`);
          await new Promise((r) => setTimeout(r, 1500 * attempt));
        }
      }
      if (!ok) throw new Error('диапазон не загружен: ' + start);
      process.stdout.write('.');
    }
  } finally {
    fs.closeSync(fd);
  }
  const size = fs.statSync(out).size;
  fs.unlinkSync(partsFile);
  console.log(`\nOK: ${out} (${(size / 1048576).toFixed(1)} MB)`);
}

main().catch((e) => { console.error('\nERR: ' + e.message); process.exit(1); });
