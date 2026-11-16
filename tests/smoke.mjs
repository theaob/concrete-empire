// Smoke test: the game loads in Turkish and English without page errors, and talks to the Play Games, AdMob and reminder bridges.
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
// Estate manager and time away: an untouched plot is signed after AUTO_SECS without a losing deal, switching the
// manager off stops that, and time away is capped by the Night watchman
// upgrade (1 hour at first), and within that cap the firm hands over buildings and reports them.
{
  const ctx = await browser.newContext({ viewport: { width: 400, height: 860 }, locale: 'en-US' });
  await ctx.addInitScript(() => localStorage.setItem('kk-tycoon-lang-v1', 'en'));
  const page = await ctx.newPage();
  const errors = [];
  page.on('pageerror', e => errors.push(String(e)));
  await page.goto(url);
  await page.waitForTimeout(500);
  const r = await page.evaluate(() => {
    clearInterval(window.__kkTimer);
    const row = !!document.querySelector('#listings .auto [data-a="auto"]');
    S.auto = false; for (let i = 0; i < AUTO_SECS + 5; i++) tick();
    const offIdle = activeBuilds() === 0;
    S.auto = true; let guard = 0; while (!activeBuilds() && guard++ < 600) tick();
    const p = S.projects.find(x => x.status === 'build');
    const signed = !!p, sound = !!p && p.dev * priceOf(p) >= p.total;
    const cap0 = offCap(); S.up.watch = 2; const cap2 = offCap();
    S.seen = Date.now() - 5 * 3600 * 1000; accrueAway();
    return { caps: cap0 === 3600 && cap2 === 3 * 3600 && S.away && S.away.secs === 3 * 3600 && S.away.capH === 3, row, offIdle, signed, sound, done: S.away ? S.away.done : -1, amt: S.away ? S.away.amt : 0, banner: !!document.querySelector('#away .away') || (render(), !!document.querySelector('#away .away')) };
  });
  const ok = r.caps && r.row && r.offIdle && r.signed && r.sound && r.done >= 1 && r.amt > 0 && r.banner && !errors.length;
  console.log(`[auto] toggle ${r.row ? 'shown' : 'MISSING'}, off=${r.offIdle ? 'idle' : 'SIGNED'}, on=${r.signed ? 'signed' : 'NOT SIGNED'}, profitable=${r.sound}, away cap 1h -> 3h at watchman lv 2: ${r.caps ? 'ok' : 'WRONG'}, 5h away: ${r.done} handed over, banner ${r.banner ? 'shown' : 'MISSING'}`);
  if (!ok) { console.error('[auto] estate manager / time away check failed' + (errors.length ? ':\n  ' + errors.join('\n  ') : '')); failed = true; }
  await ctx.close();
}
// Haggling: every generated plot has a profitable deal at the owner's bottom line, a losing ask needs a second tap,
// an offer above the ask is capped at the ask, a handshake survives closing the sheet, and it can be called off.
{
  const ctx = await browser.newContext({ viewport: { width: 400, height: 860 }, locale: 'en-US' });
  await ctx.addInitScript(() => localStorage.setItem('kk-tycoon-lang-v1', 'en'));
  const page = await ctx.newPage();
  const errors = [];
  page.on('pageerror', e => errors.push(String(e)));
  await page.goto(url);
  await page.waitForTimeout(500);
  const r = await page.evaluate(() => {
    S.auto = false;
    let bad = 0;
    for (const rep of [0, 1, 3, 6]) for (const lv of [1, 3, 8]) { S.rep = rep; S.lv = lv;
      for (let i = 0; i < 100; i++) { S.listings = []; const l = genListing(); if (Math.max(1, resF(l)) > beF(l)) bad++; } }
    S.rep = 0; S.lv = 1; S.listings = []; refill();
    const l = S.listings[0], U = unitsOf(l); l.ask = 90; openNeg(l.id);
    const zone = !!document.querySelector('#neg .zone .ok');
    agreeAsk(); const needsSecondTap = N.sure === true && !N.deal;
    l.ask = beF(l) / U * 100; N.sure = false; N.o = U; makeOffer();
    const capped = !!N.deal && N.deal.o === askF(l);
    closeNeg(); openNeg(l.id); const kept = !!N.deal && N.deal.o === askF(l);
    const pat = l.pat; undoDeal(); const undone = !N.deal && !l.deal && l.pat === pat - 1;
    closeNeg(); skipPlot(l.id); const skipped = S.listings.length === 1 && S.listings[0].id !== l.id;
    return { bad, zone, needsSecondTap, capped, kept, undone, skipped };
  });
  const ok = r.bad === 0 && r.zone && r.needsSecondTap && r.capped && r.kept && r.undone && r.skipped && !errors.length;
  console.log(`[haggle] unwinnable plots=${r.bad}/1200, range bar=${r.zone}, loss needs 2nd tap=${r.needsSecondTap}, over-ask capped=${r.capped}, handshake kept=${r.kept}, called off=${r.undone}, skipped=${r.skipped}`);
  if (!ok) { console.error('[haggle] negotiation check failed' + (errors.length ? ':\n  ' + errors.join('\n  ') : '')); failed = true; }
  await ctx.close();
}
// AdMob bridge: with a fake window.AndroidAds a watched ad resolves true, an unavailable ad resolves false with
// reason 'noad', and the Settings privacy row (GDPR) shows and reaches the bridge.
{
  const ctx = await browser.newContext({ viewport: { width: 400, height: 860 }, locale: 'en-US' });
  await ctx.addInitScript(() => {
    localStorage.setItem('kk-tycoon-lang-v1', 'en');
    window.__ads = []; window.__adNext = [true, ''];
    window.AndroidAds = {
      showRewarded: (p, id) => { window.__ads.push(['show', p]); const [ok, why] = window.__adNext; setTimeout(() => window.__adResult(id, ok, why), 50); },
      privacyOptionsRequired: () => true,
      showPrivacyOptions: () => window.__ads.push(['privacy']),
    };
  });
  const page = await ctx.newPage();
  const errors = [];
  page.on('pageerror', e => errors.push(String(e)));
  await page.goto(url);
  await page.waitForTimeout(500);
  const watched = await page.evaluate(() => Ads.showRewarded('grant'));
  const noad = await page.evaluate(async () => { window.__adNext = [false, 'noad']; const ok = await Ads.showRewarded('boost-rent'); return [ok, Ads.why, Ads.failMsg('x')]; });
  await page.evaluate(() => setTab('set'));
  const shown = await page.evaluate(() => !document.getElementById('adsSec').hidden);
  if (shown) await page.click('[data-a="adprivacy"]');
  const calls = await page.evaluate(() => window.__ads);
  const ok = watched === true && noad[0] === false && noad[1] === 'noad' && noad[2] !== 'x' && shown
    && calls.some(c => c[0] === 'privacy') && calls.filter(c => c[0] === 'show').length === 2 && !errors.length;
  console.log(`[ads] watched=${watched}, unavailable=${JSON.stringify(noad)}, privacy row ${shown ? 'shown' : 'MISSING'}, calls: ${calls.map(c => c.join(':')).join(', ')}`);
  if (!ok) { console.error('[ads] AdMob bridge check failed' + (errors.length ? ':\n  ' + errors.join('\n  ') : '')); failed = true; }
  await ctx.close();
}
// Reminder bridge: with a fake window.AndroidNotify the game asks for permission once (only after the first
// handover), plans an "away" reminder at the Night watchman cap plus a next-morning daily one, shows the Settings row,
// and an Off toggle clears the plan.
{
  const ctx = await browser.newContext({ viewport: { width: 400, height: 860 }, locale: 'en-US' });
  await ctx.addInitScript(() => {
    localStorage.setItem('kk-tycoon-lang-v1', 'en');
    window.__nt = { perm: 'ask', asks: 0, plans: [] };
    window.AndroidNotify = {
      permission: () => window.__nt.perm,
      requestPermission: () => { window.__nt.asks++; window.__nt.perm = 'granted'; setTimeout(() => window.__notifyState(), 10); },
      openSettings: () => {},
      setPlan: j => window.__nt.plans.push(JSON.parse(j)),
    };
  });
  const page = await ctx.newPage();
  const errors = [];
  page.on('pageerror', e => errors.push(String(e)));
  await page.goto(url);
  await page.waitForTimeout(500);
  const early = await page.evaluate(() => { Notify.sync(true); return window.__nt.asks; });
  const r = await page.evaluate(async () => {
    S.rep = 1; const fake = !S.projects.length; if (fake) S.projects.push({ id: 999 });   // the away reminder needs a building
    Notify.sync(true); await new Promise(res => setTimeout(res, 100));
    Notify.sync(true); if (fake) S.projects.pop();
    const plan = window.__nt.plans[window.__nt.plans.length - 1] || [];
    const away = plan.find(x => x.id === 'away'), daily = plan.find(x => x.id === 'daily');
    setTab('set');
    const shown = !document.getElementById('notifSec').hidden;
    document.querySelector('#setNotif [data-a="set"][data-c="off"]').click();
    const off = window.__nt.plans[window.__nt.plans.length - 1];
    return { asks: window.__nt.asks, away: away && away.after === offCap() && away.quiet, daily: daily && new Date(daily.at).getHours() === 10 && daily.at > Date.now(), shown, off: Array.isArray(off) && off.length === 0 };
  });
  const ok = early === 0 && r.asks === 1 && r.away && r.daily && r.shown && r.off && !errors.length;
  console.log(`[notify] asked before handover=${early}, after=${r.asks}, away plan=${r.away}, daily plan=${r.daily}, settings row ${r.shown ? 'shown' : 'MISSING'}, off clears=${r.off}`);
  if (!ok) { console.error('[notify] reminder bridge check failed' + (errors.length ? ':\n  ' + errors.join('\n  ') : '')); failed = true; }
  await ctx.close();
}
await browser.close();
process.exit(failed ? 1 : 0);
