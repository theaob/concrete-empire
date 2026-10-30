# Concrete Empire (eski adı: Kat Karşılığı Tycoon)

Tek dosyalık HTML/JS tarayıcı inşaat tycoon oyunu. Oyuncu arsa sahipleriyle kat karşılığı pazarlık yapar,
inşa eder, daireleri satar veya kiraya verir. Oyun sonsuzdur (bitiş yok). Web sürümü claude.ai'de
"Kat Karşılığı Tycoon" artifact'i olarak yayında; Android'e imzalı APK olarak paketlenir.

## Klasörler

| Yol | İçerik |
|---|---|
| `game/index.html` | **Oyunun kendisi (asıl kaynak).** Web sürümü ve APK bu dosyadan. |
| `android/build.sh` | İmzalı APK derleme betiği. Ayarlar en üstte (ad, paket, sürüm). |
| `android/src/` | Android kabuğu: manifest, `MainActivity.java` (tam ekran WebView), simgeler. `assets/index.html` derlemede `game/`'den kopyalanır. |
| `android/tools/` | (İsteğe bağlı, repoda yok) SDK'sız derleme araçları: aapt2, android.jar, d8, ecj, apksigner. Yoksa `build.sh` `$ANDROID_HOME`'daki Android SDK'yı kullanır. |
| `android/zipalign.py` | Basit zipalign (Python). |
| `android/katkarsiligi-release.jks` + `keystore-password.txt` | **İmza anahtarı ve şifresi. Repoya girmez** (`.gitignore`). Yerelde bu klasöre koyulur; CI GitHub Secrets'tan alır. **Kaybetme:** güncellemeler aynı anahtarla imzalanmazsa telefona üstüne kurulmaz. |
| `android/out/` | Derlenen APK (repoya girmez). |
| `tests/smoke.mjs` | Playwright duman testi: Türkçe ve İngilizce açılış, sayfa hataları, İngilizcede kalan Türkçe metin. |
| `docs/translation-rules.md` | İngilizce çeviri kuralları ve terim sözlüğü. |
| `docs/icon-512.png` | Uygulama simgesinin kaynağı (mipmap'ler bundan üretildi). |
| `.github/workflows/ci.yml` | Test → imzalı APK → (main'de) itch.io. |

## APK derleme

**CI (önerilen):** Her push ve PR'da GitHub Actions testleri çalıştırır ve APK'yı derleyip "apk" artifact'i olarak yükler.
`v*` etiketinde GitHub Release'e de ekler. `main`'e her push'ta web sürümü ve APK itch.io'ya gönderilir.

Gerekli GitHub Secrets (Settings → Secrets and variables → Actions):

| Secret | Değer |
|---|---|
| `ANDROID_KEYSTORE_BASE64` | `base64 -w0 katkarsiligi-release.jks` çıktısı |
| `ANDROID_KEYSTORE_PASSWORD` | `keystore-password.txt` içeriği (keystore ve anahtar şifresi aynı) |
| `ANDROID_KEY_ALIAS` | İsteğe bağlı, varsayılan `katkarsiligi` |
| `BUTLER_API_KEY`, `ITCH_GAME` | İsteğe bağlı itch.io yayını (`ITCH_GAME` örn. `kullanici/concrete-empire`) |

İmza secret'ları yoksa CI APK'yı geçici bir test anahtarıyla imzalar (`-TESTKEY.apk`); bu APK kurulu sürümün üstüne kurulmaz.

**Yerel:** Linux x64, Java 17+, Python 3, `zip` ve ya `android/tools/` ya da Android SDK (build-tools 34.0.0, platform 34).
Anahtar ve şifre dosyasını `android/` içine koy, sonra:

```bash
# build.sh başında VERSION_CODE'u 1 artır, VERSION_NAME'i güncelle
bash android/build.sh
```

Betik sürüm adını ve oyun adını otomatik olarak oyuna (Ayarlar'daki sürüm satırı, `GAME_NAME`) ve
uygulama adına yazar. Çıktı: `android/out/ConcreteEmpire-<sürüm>.apk` (v1+v2+v3 imzalı, minSdk 24, targetSdk 34).

Testler: `npm install && npx playwright install chromium && npm test`.

## Mevcut durum (sürüm 2.3)

**Arayüz**
- Üst bar: nakit, anlık kira (₺/sn, güçlendirme açıkken "×2"), net değer, firma seviyesi, itibar, çimento/demir.
  İnşaat hızı (m²/sn) üst barda gösterilmez.
- Şantiye başlığında yüzde / "Teslim" damgası tek satırda kalır.
- "Maketten daire sat" ve hibe butonları tam genişlik.
- Ayarlar en altında "Concrete Empire · Sürüm X".

**Denge**
- Proje büyüklüğü firma seviyesine bağlı (`blockCap`, `lvFloors`, `lvUpf`): seviye 1–3 tek blok az katlı,
  4'te ikinci blok, ~2000 daireli 10 bloklu siteler ve devlet ihaleleri 20. seviyede (`TENDER_LV=20`).
- Seviye eşiği `12·L²`; 1→20 yaklaşık 70 proje. Eski kayıtlar bir kez yarı seviyeye çevrildi (`lvMig2`).
- İtibar kazancı 100'e kalan mesafeyle orantılı (`addIti`); kayıplar tam düşer.

**Dil**
- Türkçe / İngilizce. `T(tr,en)` yardımcısı; dil Ayarlar'dan değişir, sayfa yeniden yüklenir.
  Varsayılan cihaz dili. Statik HTML metinleri `data-en` öznitelikleriyle.
- İngilizcede para `$1.4M`, yüzde sayıdan sonra. Ev sahipleri, bina adları ve ihale şehirleri İngilizce
  karşılıklarıyla değişir; kayıttaki adlar yüklemede çevrilir (`localizeNames`).

**Reklam (ana gelir modeli)**
- `Ads.showRewarded(placement)` → yalnızca reklam sonuna kadar izlenirse `true`.
  APK'da `window.AndroidAds.showRewarded(placement,id)` köprüsü varsa gerçek reklam; sonucu
  `window.__adResult(id, ok)` ile bildirir. Köprü yoksa 5 saniyelik yer tutucu reklam gösterilir.
- **Hibe:** Nakit bitince reklam → borçsuz hibe: şantiyenin `30 + 3·(seviye−1)` saniyelik işi (1. seviyede 30 sn, 20.'de 87 sn),
  kalan ihtiyacı aşmaz. Firma seviyesine göre kendiliğinden ayarlanır; hibeyi artıran geliştirme yok (`grantSecs`).
  Reklam hiçbir yerde kredi vermez; banka kredisi ve "Banka ilişkileri" geliştirmesi kaldırıldı. Eski borç sabit %1,5/ay
  faizle kira ve satıştan ödenmeye devam eder.
- **Kira ×2 / Hız ×2:** Her reklam 3 dk açar, en fazla 30 dk'ya kadar uzar; süre gerçek saatle işler.
  Uygulama kapalıyken biriken kira katlanmaz.

**Hata düzeltmeleri**
- Şantiyeye dokunma kendi basma/kaldırma takibiyle sayılır (resim her saniye yeniden çizilse de).
- Yeniden çizimde döngüsel animasyonlar kaldığı yerden devam eder; camlar yalnız yeni eklenince animasyon oynatır.
- Kirayla yavaş ilerlerken şantiye ancak 3 sn üst üste nakit yoksa "durdu" olur.

**Android**
- Paket `com.onur.concreteempire`, son sürüm 2.3 (versionCode 12).
- 1.x sürümleri `com.onur.katkarsiligi` paketindeydi; kayıt Ayarlar > Kayıt kodu ile taşınır.
- Kayıt anahtarları (`kk-tycoon-*`) eski adla kaldı, değiştirilirse kayıtlar kaybolur.

## Kalıcı tasarım kararları

- Oyun sonsuz; tek seferde tek şantiye; inşaat sürerken pazarlık kapalı.
- Kalite (Ekonomik/Standart/Lüks) başta bir kez seçilir; inşaat sırasında soru yok.
- Animasyonlar cihazın "hareketi azalt" ayarından bağımsız oynar.
- Sade üst bar (logo/tarih yok); geliştirmeler ayrı sekmede; piyasada yalnız çimento ve demir.

## Açık işler

- **Gerçek reklam ağı (AdMob):** Uygulama kimliği ve ödüllü reklam birimi kimliği gerekli. Google Mobile Ads
  kütüphanesi `dl.google.com` / `maven.google.com` üzerinden iner; Java tarafında `AndroidAds` köprüsü yazılacak.
- Geliştirmeler sekmesinde ve marka panelinde m²/sn bilgisi hâlâ görünüyor.
