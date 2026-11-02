# Google Play: mağaza kaydı ve kapalı test

Bu klasör Play Console'a girilecek her şeyi tutar: mağaza metinleri (TR/EN), görseller, Veri güvenliği ve
içerik derecelendirmesi cevapları, kapalı test adımları.

| Dosya | Play Console'da nereye |
|---|---|
| `../icon-512.png` | Ana mağaza kaydı › Uygulama simgesi (512×512) |
| `feature-graphic-tr.png`, `feature-graphic-en.png` | Ana mağaza kaydı › Öne çıkan grafik (1024×500) |
| `screenshots/tr/1-5.png`, `screenshots/en/1-5.png` | Ana mağaza kaydı › Telefon ekran görüntüleri (1080×2160) |
| `../privacy.html` | Uygulama içeriği › Gizlilik politikası (GitHub Pages adresi, aşağıda) |
| CI'daki **aab** artifact'i | Test › Kapalı test › Yeni sürüm oluştur |

Ekran görüntüleri Playwright ile oyunun kendisinden alındı (400×800, ×2,7).

## AAB nereden gelir

Her push ve PR'da CI, APK'nın yanında `ConcreteEmpire-<sürüm>.aab` üretir (Actions › ilgili çalışma ›
Artifacts › **aab**, zip içinde). AAB, `ANDROID_KEYSTORE_BASE64` ile aynı `katkarsiligi` anahtarıyla imzalanır;
Play bu anahtarı **yükleme anahtarı** olarak kaydeder, kullanıcılara giden sürümü Google kendi
uygulama imzalama anahtarıyla imzalar. Dosya adında `-TESTKEY` varsa secret'lar eksiktir; o dosyayı Play'e yükleme.

- targetSdk 36 (Android 16): Google Play, 31 Ağustos 2026'dan sonra yeni uygulamalarda bunu ister. minSdk 24.
- Her yeni yüklemede `android/build.sh` içindeki `VERSION_CODE` bir artmalı; Play aynı numarayı ikinci kez kabul etmez.
- Play'den kurulan sürüm Google'ın anahtarıyla imzalanacağı için itch.io'daki APK'nın üstüne kurulmaz
  (ve tersi). Oyuncu kaydını Ayarlar › Kayıt kodu ile taşır.

## Gizlilik politikası adresi

`docs/privacy.html` ve oyunun web sürümü `.github/workflows/pages.yml` ile GitHub Pages'e yayınlanır. Bir kerelik ayar: repo
**Settings › Pages › Build and deployment › Source: GitHub Actions**. Web sürümü: https://theaob.github.io/concrete-empire/ . Gizlilik adresi:

**https://theaob.github.io/concrete-empire/privacy.html**

---

## Mağaza metinleri

### Türkçe (tr-TR, varsayılan dil)

**Uygulama adı** (en çok 30): `Concrete Empire: İnşaat Tycoon`

**Kısa açıklama** (en çok 80):
> Arsa sahibiyle kat karşılığı pazarlık et, bina dik, daireleri sat ya da kirala!

**Tam açıklama** (en çok 4000):

```
Concrete Empire'da küçük bir müteahhit olarak başlıyorsun. Arsa sahipleriyle kat karşılığı pazarlık et, binayı kendi cebinden yap, kalan daireleri sat ya da kiraya ver. Firmanı büyüt, tek bloktan koca sitelere, sonunda devlet ihalelerine uzan!

🤝 GERÇEK BİR PAZARLIK
Her ev sahibinin bir mizacı ve sabrı var. Kaç daire vereceğini, ne kadar nakit ekleyeceğini sen belirlersin. Fazla sıkıştırırsan masadan kalkar, yeni parsel gelir.

🏗️ ŞANTİYEYİ İZLE
Hafriyat, temel, betonarme karkas, duvar, cephe… Bina kat kat yükselir. Şantiyeye dokunarak işi hızlandır.

🏠 SAT YA DA KİRALA
Teslimde daireleri tek seferde sat ya da kiraya verip düzenli kira geliri kur. Ekonomik, Standart ya da Lüks kalite seç; kalite fiyatı ve itibarını etkiler.

📈 FİRMANI BÜYÜT
Kalfa, usta ekibi, kalıpçı ve demirci ekipleri tut; geliştirmelerle firmanı güçlendir. Çimento ve demir fiyatlarını takip et. Görevleri tamamla, itibarını yükselt.

🏛️ DEVLET İHALELERİ
Firma seviyesi 20'de okul, üniversite, köprü, tünel, otoyol, baraj ve havalimanı ihaleleri açılır.

✔ Sonu olmayan oyun, kendi hızında oyna
✔ İnternet gerekmez, hesap gerekmez
✔ Google Play Games başarımları ve liderlik tabloları
✔ Türkçe ve İngilizce
```

### English (en-US)

**App name** (max 30): `Concrete Empire: Build Tycoon`

**Short description** (max 80):
> Haggle with landowners, raise apartment blocks, then sell or rent out the units!

**Full description** (max 4000):

```
In Concrete Empire you start as a small contractor. Strike flat-for-land deals with landowners: they give you the plot, you build out of your own pocket and hand them the agreed number of units. The rest are yours to sell or rent out. Grow from single blocks to huge estates, and finally to government tenders!

🤝 REAL HAGGLING
Every landowner has a temper and limited patience. You decide how many units to give and how much cash to add. Push too hard and they walk away from the table.

🏗️ WATCH YOUR SITE RISE
Excavation, foundation, concrete frame, walls, facade… the building goes up floor by floor. Tap the site to speed things up.

🏠 SELL OR RENT
On handover, sell every unit at once or rent them out for steady income. Pick Economy, Standard or Luxury quality; it changes prices and your reputation.

📈 GROW YOUR FIRM
Hire foremen, master crews, formwork and rebar teams, and invest in upgrades. Keep an eye on cement and rebar prices. Complete tasks and build your reputation.

🏛️ GOVERNMENT TENDERS
At firm level 20, schools, universities, bridges, tunnels, motorways, dams and airports unlock.

✔ Endless game, play at your own pace
✔ No internet, no account needed
✔ Google Play Games achievements and leaderboards
✔ English and Turkish
```

**Kategori:** Oyun › Simülasyon. **Etiketler:** Tycoon, İnşaat, Boşta kalma (idle), Tek oyunculu, Çevrimdışı.

---

## Uygulama içeriği (App content) cevapları

Bu sürümde (AdMob yok; Play Games var) cevaplar:

| Bölüm | Cevap |
|---|---|
| Gizlilik politikası | `https://theaob.github.io/concrete-empire/privacy.html` |
| Uygulama erişimi | Tüm işlevler özel erişim olmadan kullanılabilir |
| Reklamlar | **Hayır**, reklam içermiyor (reklam düğmeleri şimdilik yer tutucu; AdMob eklenince "Evet" yapılacak) |
| İçerik derecelendirmesi | Kategori: Oyun. Şiddet, cinsellik, küfür, uyuşturucu, kumar: hayır. Kullanıcılar etkileşim kurabiliyor mu / içerik paylaşıyor mu: hayır. Konum paylaşımı: hayır. Dijital satın alma: hayır. Beklenen sonuç: PEGI 3 / Herkes |
| Hedef kitle | **13-15, 16-17, 18+** (13 altını seçme: Aile politikası ve reklam kısıtları devreye girer). "Çocuklara hitap ediyor mu?": Hayır |
| Haber uygulaması | Hayır |
| Veri güvenliği | Aşağıda |
| Kamu uygulaması / Finans özellikleri / Sağlık | Hayır / Hiçbiri / Hayır |
| Reklam kimliği (Advertising ID) | Hayır, kullanmıyor |

### Veri güvenliği (Data safety)

Play Games Hizmetleri eklendiği için form artık **"Hayır" değil**. Oyunun kendisi veri göndermez (kayıt cihazdaki
WebView depolamasında durur, oyun ekranı çevrimdışı), ama Play Games SDK'sının topladıklarını beyan etmek gerekiyor.
Google'ın listesi: [Play Games Hizmetleri veri toplama](https://developer.android.com/games/pgs/data-collection).
O sayfaya göre önerilen cevaplar (formu doldururken sayfayla bir daha karşılaştır):

- Veri topluyor ya da paylaşıyor mu? **Evet**. Aktarımda şifreleniyor mu? **Evet** (HTTPS).
- Kullanıcı silme isteyebiliyor mu? **Evet**: Play Games profili veya Google Hesabı üzerinden.

| Veri türü | Toplanıyor | Paylaşılıyor | Zorunlu mu | Amaç |
|---|---|---|---|---|
| Kişisel bilgiler › Kullanıcı kimlikleri (Play Games oyuncu kimliği, oyuncu adı/avatar) | Evet | Hayır | İsteğe bağlı (giriş zorunlu değil) | Uygulama işlevselliği, Hesap yönetimi |
| Uygulama etkinliği › Diğer işlemler (başarımlar, skorlar) | Evet | Hayır | İsteğe bağlı | Uygulama işlevselliği |
| Uygulama bilgileri ve performansı › Teşhis | Evet | Hayır | Zorunlu | Analiz |
| Uygulama etkinliği › Uygulama etkileşimleri (SDK analizi) | Evet | Hayır | Zorunlu | Analiz |

"Paylaşılıyor: Hayır" çünkü veri Google'a hizmet sağlayıcı olarak gidiyor; Play'in tanımında bu paylaşım sayılmaz.

**AdMob eklendiğinde** güncellenecekler (Google'ın AdMob Veri güvenliği rehberine göre):

| Veri türü | Toplanıyor | Paylaşılıyor | Amaç |
|---|---|---|---|
| Cihaz veya diğer kimlikler (Reklam kimliği) | Evet | Evet | Reklam veya pazarlama, Analiz, Sahtekarlık önleme |
| Uygulama etkinliği › Uygulama etkileşimleri | Evet | Evet | Reklam, Analiz |
| Uygulama bilgileri ve performansı › Kilitlenme ve teşhis | Evet | Evet | Analiz |
| Konum › Yaklaşık konum (IP'den) | Evet | Evet | Reklam, Sahtekarlık önleme |

Aktarımda şifreli: Evet. Kullanıcı silme isteyebilir: Hayır (veri Google'da). O zaman "Reklamlar: Evet" ve
manifest'e `com.google.android.gms.permission.AD_ID` da eklenmeli.

---

## Kapalı test ve 14 gün kuralı

13 Kasım 2023'ten sonra açılan **kişisel** geliştirici hesaplarında üretime çıkmak için: bir kapalı testte
**en az 12 test kullanıcısı**, **kesintisiz 14 gün** boyunca katılmış (opt-in) kalmalı. Sayaç 12. kişi katıldığı
gün başlar; biri ayrılırsa ve 12'nin altına düşülürse sayaç durur. 14 gün dolunca Kontrol paneli › "Üretime
erişim başvurusu" açılır.

Adım adım:

1. **Hesap doğrulama**: Play Console'da kimlik doğrulaması ve Play Console mobil uygulamasıyla Android cihaz
   doğrulaması bitmiş olmalı (yoksa uygulama oluşturma kilitli kalır).
2. **Uygulama oluştur**: Tüm uygulamalar › Uygulama oluştur. Ad `Concrete Empire`, varsayılan dil Türkçe,
   Oyun, Ücretsiz. Beyanları onayla.
3. **Paket adını sabitleyen ilk yükleme**: Test ve yayınla › Test › **Kapalı test** › Alfa (varsayılan kanal) ›
   Sürüm oluştur. "Play Uygulama İmzalama" sorulursa **Google tarafından oluşturulan anahtar (önerilen)**.
   CI'daki `aab` artifact'ini indir, zipten `ConcreteEmpire-x.y.aab`'yi çıkar ve yükle. Sürüm notu: "İlk kapalı test sürümü".
4. **Test kullanıcıları**: Aynı kanalda Test kullanıcıları › E-posta listesi oluştur, 12'den fazla Gmail adresi
   ekle (15-20 koy; biri bırakırsa sayaç durmasın). Ya da bir Google Grubu kullan. Geri bildirim adresi gir.
5. **Ülkeler**: Kanalın Ülkeler/bölgeler sekmesinde Türkiye (ve istersen diğerleri).
6. **Kontrol panelindeki görevler**: Uygulama içeriği (yukarıdaki tablo), Ana mağaza kaydı (metin + görseller),
   Uygulama kategorisi ve iletişim bilgileri (geliştirici e-postası zorunlu).
7. **İncelemeye gönder**: Yayınlama genel bakışı › Değişiklikleri incelemeye gönder. İlk inceleme birkaç saatten
   birkaç güne sürebilir.
8. **Katılım linki**: Onaydan sonra kanalın Test kullanıcıları sekmesindeki **"Web'de katıl"** linkini testçilere
   gönder. Her biri linkte "Test kullanıcısı ol"a basmalı ve uygulamayı Play'den **kurmalı**. 14 gün boyunca
   kaldırmasınlar.
9. **14 gün sonra**: Kontrol paneli › Üretime erişim başvurusu. Formda test sürecini (kaç kişi, ne geri bildirim
   geldi, ne düzeltildi) anlat. Onay gelince üretim kanalına sürüm çıkar.

Test sırasında yeni sürüm: `VERSION_CODE`'u artır, PR'ı birleştir, CI'nın `aab`'ini aynı kapalı kanala yükle.
Testçiler güncellemeyi Play'den alır, 14 gün sayacı sıfırlanmaz.
