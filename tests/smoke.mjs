// Smoke test: the game loads in Turkish and English without page errors, and talks to the Play Games bridge.
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
// Play Games bridge: with a fake window.AndroidGames the Settings row shows, sign-in reaches the bridge,
// and once signed in already-earned achievements and scores are sent.
{
  const ctx = await browser.newContext({ viewport: { width: 400, height: 860 }, locale: 'en-US' });
  await ctx.addInitScript(() => {
    localStorage.setItem('kk-tycoon-lang-v1', 'en');
    window.__calls = [];
    window.AndroidGames = {
      available: () => true,
      signIn: () => { window.__calls.push(['signIn']); setTimeout(() => window.__pgsState(true, 'Tester'), 50); },
      unlock: k => { window.__calls.push(['unlock', k]); return true; },
      submitScore: (k, v) => { window.__calls.push(['score', k, v]); return true; },
      showAchievements: () => window.__calls.push(['showAchievements']),
      showLeaderboards: () => window.__calls.push(['showLeaderboards']),
    };
  });
  const page = await ctx.newPage();
  const errors = [];
  page.on('pageerror', e => errors.push(String(e)));
  await page.goto(url);
  await page.waitForTimeout(500);
  await page.evaluate(() => { S.lv = 6; S.rep = 2; setTab('set'); });
  const shown = await page.evaluate(() => !document.getElementById('pgsSec').hidden);
  await page.click('[data-a="pgsin"]');
  await page.waitForTimeout(300);
  await page.click('[data-a="pgsach"]');
  const calls = await page.evaluate(() => window.__calls);
  const has = (...c) => calls.some(x => c.every((v, i) => x[i] === v));
  const ok = shown && has('signIn') && has('unlock', 'first_handover') && has('unlock', 'level_5') && !has('unlock', 'level_10')
    && has('score', 'firm_level', 6) && has('showAchievements') && !errors.length;
  console.log(`[pgs] settings row ${shown ? 'shown' : 'MISSING'}, bridge calls: ${calls.map(c => c.join(':')).join(', ')}`);
  if (!ok) { console.error('[pgs] Play Games bridge check failed' + (errors.length ? ':\n  ' + errors.join('\n  ') : '')); failed = true; }
  await ctx.close();
}
await browser.close();
process.exit(failed ? 1 : 0);
