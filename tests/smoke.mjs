// Smoke test: the game loads in Turkish and English without page errors.
// In English it also lists visible text that still contains Turkish letters (warning only; names are allowed).
// Usage: node tests/smoke.mjs [path/to/index.html]
import { chromium } from 'playwright';
import { pathToFileURL } from 'node:url';
import { resolve } from 'node:path';

const file = resolve(process.argv[2] || 'game/index.html');
const url = pathToFileURL(file).href;
const browser = await chromium.launch();
let failed = false;

for (const lang of ['tr', 'en']) {
  const ctx = await browser.newContext({ viewport: { width: 400, height: 860 }, locale: lang === 'tr' ? 'tr-TR' : 'en-US' });
  await ctx.addInitScript(l => { try { localStorage.setItem('kk-tycoon-lang-v1', l); } catch (e) {} }, lang);
  const page = await ctx.newPage();
  const errors = [];
  page.on('pageerror', e => errors.push(String(e)));
  page.on('console', m => { if (m.type() === 'error') errors.push(m.text()); });
  await page.goto(url);
  await page.waitForTimeout(3000);
  const info = await page.evaluate(() => ({ title: document.title, text: document.body.innerText.length, lang: typeof LANG !== 'undefined' ? LANG : null }));
  console.log(`[${lang}] title="${info.title}" LANG=${info.lang} visible chars=${info.text}`);
  if (info.lang !== lang) { console.error(`[${lang}] LANG is ${info.lang}`); failed = true; }
  if (info.text < 50) { console.error(`[${lang}] page looks empty`); failed = true; }
  if (errors.length) { console.error(`[${lang}] page errors:\n  ` + errors.join('\n  ')); failed = true; }
  if (lang === 'en') {
    const tr = await page.evaluate(() => {
      const out = new Set(), w = document.createTreeWalker(document.body, NodeFilter.SHOW_TEXT);
      for (let n; (n = w.nextNode());) {
        const el = n.parentElement, t = n.textContent.trim();
        if (!t || !el || !el.offsetParent || !/[çğıöşüÇĞİÖŞÜ]/.test(t)) continue;
        out.add(t.slice(0, 80));
      }
      return [...out];
    });
    if (tr.length) console.warn(`[en] visible text with Turkish letters (check, names are fine):\n  ` + tr.join('\n  '));
  }
  await ctx.close();
}
await browser.close();
process.exit(failed ? 1 : 0);
