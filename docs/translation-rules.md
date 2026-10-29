# Adding English to "Concrete Empire" (a Turkish construction tycoon game)

You are given one chunk of a single-file browser game. All player-visible text is Turkish today.
Your job: make every player-visible string bilingual, WITHOUT changing any game logic.

## Helpers already defined (earlier in the file, do not redefine)
- `T(tr, en)` → returns `en` when the language is English, otherwise `tr`. Use it for every visible string.
  Works anywhere, including in top-level constant tables (language is fixed for the page's lifetime).
  Inside template literals: `${T('Nakit','Cash')}` or `${T(`${n} daire`,`${n} units`)}`.
- `LANG` is `'tr'` or `'en'`. Use `LANG==='en'` when a whole expression differs (word order, plurals, grammar).
- `UP(s)` / `LOW(s)` → locale-correct upper/lower case. Replace `.toLocaleUpperCase('tr')` with `UP(...)` and `.toLocaleLowerCase('tr')` with `LOW(...)`.
- `DEC(s)` → swaps '.' for ',' only in Turkish. Replace hard-coded `.replace('.',',')` on displayed numbers with `DEC(...)`.
- `NUM(v)` → rounded number with the right thousands separator. Replace `.toLocaleString('tr-TR')` with `NUM(...)` (or `toLocaleString(LANG==='en'?'en-US':'tr-TR')`).
- `fmt(m)` (money), `fmtM2(v)`, `pc(v)` (percent) and `nr(v)` are already bilingual — leave calls to them as they are.
- `gen(name)` returns the possessive ("Kemal Bey'in" / "Kemal Bey's") — already bilingual.

## Rules
1. Change ONLY player-visible text: labels, buttons, headings, descriptions, log/news messages, tooltips (title=), aria-labels, placeholders, dialogue lines, generated names shown to the player.
2. Do NOT change: identifiers, object keys, `data-a`/`data-c`/`data-k` values, ids, class names, CSS, storage keys, numbers, formulas, control flow, or comments' meaning (you may leave comments alone).
3. Percent signs: Turkish puts % before the number (`%30`), English after (`30%`). Handle with T or LANG.
4. Turkish suffix tricks (e.g. `'${x}'ta`, vowel-harmony helpers, `'den'/'dan'`): in English produce natural English instead.
5. English plurals: make "1 unit" / "2 units" right where it's cheap (`n===1?'unit':'units'`). Don't contort code for it.
6. Keep English short and punchy, same tone as the Turkish (playful, construction-site flavour). It must fit the same UI space on a phone — prefer concise wording.
7. People's names in NAMES (e.g. 'Hatice Teyze', 'Kemal Bey') stay as they are — they are characters. Translate everything else around them.
8. Arrays of strings (dialogue lines, flavour texts, month names, rank titles, phase names) → make the array itself bilingual, e.g. `open:T([...tr...],[...en...])` keeping the SAME number of entries and the same `{n}` / `{u}` placeholders.
9. Edit the file IN PLACE with the Edit tool. Do not reformat or reorder code. Keep each line's structure; only text changes.
10. When done with a JS chunk, run `node --check <file>` on a copy renamed to .js to make sure it still parses (`cp X.txt /tmp/x.js && node --check /tmp/x.js`). Then grep the chunk for remaining Turkish characters (`grep -n "[çğıöşüÇĞİÖŞÜ]"`) and confirm every remaining hit is inside the Turkish side of a T()/LANG choice, a name, or a code comment.

## Glossary (use these consistently)
daire → unit · bina → building · şantiye → site · ev sahibi → landowner (short: owner) · parsel/arsa → plot ·
kat karşılığı → land-for-units deal · pazarlık → haggling / deal · müteahhit → developer ·
itibar → Reputation · nakit → Cash · net değer → Net worth · firma seviyesi / Sv. → Firm level / Lv. ·
marka değeri → Brand value · kira → rent · kiraya ver/kirala → rent out · sat → sell · maketten satış → off-plan sale ·
anahtar teslimi / teslim → handover / Handed over · teslimde → On handover · ihale / devlet ihalesi → tender / government tender ·
çimento → cement · demir → rebar · kat → floor · blok → block · ekip → crew · kalfa → journeyman · usta → master builder ·
Hafriyat → Excavation · Temel → Foundation · Betonarme karkas → Concrete frame · Karkas → Frame · Duvar ve çatı → Walls & roof ·
Cephe ve sıva → Facade & plaster · İnce işler → Finishing · Anahtar teslimi → Handover ·
Ekonomik / Standart / Lüks → Economy / Standard / Luxury · kalite → quality ·
Şantiye (tab) → Site · Geliştirmeler → Upgrades (short: Upgr.) · Görevler → Tasks · Ayarlar → Settings · Haber → News ·
Günlük görevler → Daily tasks · kredi → loan · faiz → interest · borç → debt · banka → bank ·
mn / mr (millions/billions) → M / B (already handled by fmt) · ay → month · hafta → week · sn → s ·
Şehir dışı → Outskirts · Banliyö → Suburbs · Şehir merkezi → City centre · Sahil → Seafront ·
Kurnaz → Shrewd · İnatçı → Stubborn · Duygusal → Sentimental · Nakde sıkışık → Cash-strapped
