# Concrete Empire (eski adı: Kat Karşılığı Tycoon)

Tek dosyalık HTML/JS tarayıcı inşaat tycoon oyunu. Oyuncu arsa sahipleriyle kat karşılığı pazarlık yapar,
inşa eder, daireleri satar veya kiraya verir. Oyun sonsuzdur (bitiş yok). Web sürümü claude.ai'de
"Kat Karşılığı Tycoon" artifact'i olarak yayında; Android'e imzalı APK olarak paketlenir.

## Klasörler

| Yol | İçerik |
|---|---|
| `game/index.html` | **Oyunun kendisi (asıl kaynak).** Web sürümü ve APK bu dosyadan. |
| `android/build.sh` | İmzalı APK + Play için imzalı AAB derleme betiği (Gradle'ı çalıştırır). Ayarlar en üstte (ad, paket, sürüm). |
| `android/build.gradle`, `settings.gradle` | Gradle/Android Gradle Plugin projesi; Play Games kütüphanesi burada. Değerleri `build.sh` verir. |
| `android/src/` | Android kabuğu: manifest, `MainActivity.java` (tam ekran WebView), `PlayGamesBridge.java` (Play Games köprüsü), `res/values/games-ids.xml` (Play Games kimlikleri), simgeler. `assets/index.html` derlemede `game/`'den kopyalanır. |
| `android/katkarsiligi-release.jks` + `keystore-password.txt` | **İmza anahtarı ve şifresi. Repoya girmez** (`.gitignore`). Yerelde bu klasöre koyulur; CI GitHub Secrets'tan alır. **Kaybetme:** güncellemeler aynı anahtarla imzalanmazsa telefona üstüne kurulmaz. |
| `android/out/` | Derlenen APK (repoya girmez). |
| `tests/smoke.mjs` | Playwright duman testi: Türkçe ve İngilizce açılış, sayfa hataları, İngilizcede kalan Türkçe metin. |
| `docs/translation-rules.md` | İngilizce çeviri kuralları ve terim sözlüğü. |
| `docs/icon-512.png` | Uygulama simgesinin kaynağı (mipmap'ler bundan üretildi). |
| `docs/play-store/` | Google Play mağaza metinleri (TR/EN), görseller, Veri güvenliği cevapları, kapalı test adımları. Play Games kurulumu: `play-games.md`. |
| `docs/privacy.html` | Gizlilik politikası (GitHub Pages: `https://theaob.github.io/concrete-empire/privacy.html`). |
| `.github/workflows/ci.yml` | Test → imzalı APK + AAB → (main'de) itch.io. |
| `.github/workflows/pages.yml` | Web sürümü ve gizlilik politikası GitHub Pages'e: `https://theaob.github.io/concrete-empire/`. Settings › Pages › Source: **GitHub Actions** olmalı. |

## APK derleme

**CI (önerilen):** Her push ve PR'da GitHub Actions testleri çalıştırır, APK'yı ve Google Play için AAB'yi derleyip
"apk" ve "aab" artifact'leri olarak yükler. `v*` etiketinde ikisini de GitHub Release'e ekler. Play adımları: `docs/play-store/README.md`. `main`'e her push'ta web sürümü ve APK itch.io'ya gönderilir.

Gerekli GitHub Secrets (Settings → Secrets and variables → Actions):

| Secret | Değer |
|---|---|
| `ANDROID_KEYSTORE_BASE64` | `base64 -w0 katkarsiligi-release.jks` çıktısı |
| `ANDROID_KEYSTORE_PASSWORD` | `keystore-password.txt` içeriği (keystore ve anahtar şifresi aynı) |
| `ANDROID_KEY_ALIAS` | İsteğe bağlı, varsayılan `katkarsiligi` |
| `BUTLER_API_KEY`, `ITCH_GAME` | İsteğe bağlı itch.io yayını (`ITCH_GAME` örn. `kullanici/concrete-empire`) |

İmza secret'ları yoksa CI APK'yı geçici bir test anahtarıyla imzalar (`-TESTKEY.apk`); bu APK kurulu sürümün üstüne kurulmaz.

**Yerel:** Java 17+, Gradle 8.13+ (`gradle` PATH'te ya da `GRADLE=/yol/gradle`) ve Android SDK (`ANDROID_HOME`; build-tools 36.0.0, platform 36).
Gradle ilk çalışmada Android Gradle Plugin'i ve Play Games kütüphanesini Google Maven'dan indirir.
Anahtar ve şifre dosyasını `android/` içine koy, sonra:

```bash
# build.sh başında VERSION_CODE'u 1 artır, VERSION_NAME'i güncelle
bash android/build.sh
```

Betik sürüm adını ve oyun adını otomatik olarak oyuna (Ayarlar'daki sürüm satırı, `GAME_NAME`) ve
uygulama adına yazar. Çıktı: `android/out/ConcreteEmpire-<sürüm>.apk` (v1+v2+v3 imzalı, minSdk 24, targetSdk 36) ve aynı adla `.aab`.

Testler: `npm install && npx playwright install chromium && npm test`.

## Mevcut durum (sürüm 2.4)

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

**Google Play Games**
- Android'de Play Games Hizmetleri v2: otomatik giriş, 13 başarım, 3 liderlik tablosu (net değer, firma seviyesi,
  toplam kazanç). Ayarlar › Google Play Games satırı yalnız APK'da görünür. Köprü `window.AndroidGames`,
  kurallar `PGS_ACH` / `PGS_LB`. Kimlikler `android/src/res/values/games-ids.xml`; proje kimliği boşken kapalı.
  Kurulum: `docs/play-store/play-games.md`.

**Reklam (ana gelir modeli)**
- `Ads.showRewarded(placement)` → yalnızca reklam sonuna kadar izlenirse `true`.
  APK'da `window.AndroidAds.showRewarded(placement,id)` köprüsü varsa gerçek reklam; sonucu
  `window.__adResult(id, ok, neden)` ile bildirir (`neden`: `noad` = reklam yüklenemedi, `skipped`, `busy`).
  Köprü yoksa (web) 5 saniyelik yer tutucu reklam gösterilir.
- **AdMob (Android, v2.4):** `AdsBridge.java`. Açılışta önce UMP izin formu (AEA/BK), sonra Mobile Ads SDK ve bir
  ödüllü reklam önceden yüklenir; dokunuşta yoksa 8 sn yüklenmesi beklenir. Kimlikler `res/values/admob-ids.xml`
  (ödüllü birim boşsa reklam kapalı, yer tutucu çalışır). Ayarlar'da GDPR gerektiğinde "Gizlilik seçenekleri" düğmesi çıkar.
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
- Paket `com.onur.concreteempire`, son sürüm 2.4 (versionCode 14).
- Play Games `androidx.fragment` 1.1.0 getiriyor (Play Console "eski SDK" uyarısı); `build.gradle` 1.8.9'a sabitler, `build.sh` 1.1.x geri gelirse derlemeyi durdurur.
- 1.x sürümleri `com.onur.katkarsiligi` paketindeydi; kayıt Ayarlar > Kayıt kodu ile taşınır.
- Kayıt anahtarları (`kk-tycoon-*`) eski adla kaldı, değiştirilirse kayıtlar kaybolur.

## Kalıcı tasarım kararları

- Oyun sonsuz; tek seferde tek şantiye; inşaat sürerken pazarlık kapalı.
- Kalite (Ekonomik/Standart/Lüks) başta bir kez seçilir; inşaat sırasında soru yok.
- Animasyonlar cihazın "hareketi azalt" ayarından bağımsız oynar.
- Sade üst bar (logo/tarih yok); geliştirmeler ayrı sekmede; piyasada yalnız çimento ve demir.

## Açık işler

- **AdMob gerçek kimlikleri:** `android/src/res/values/admob-ids.xml` şimdilik Google'ın test kimliklerini taşıyor
  (hep "Test Ad" gösterir, para kazandırmaz). Gerçek uygulama + ödüllü birim kimlikleri girilecek; adımlar
  `docs/play-store/admob.md`.
- Geliştirmeler sekmesinde ve marka panelinde m²/sn bilgisi hâlâ görünüyor.
