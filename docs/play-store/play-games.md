# Google Play Games Hizmetleri (PGS v2)

Oyun Android'de Play Games'e bağlanır: oyuncu cihazda Play Games'e girişliyse açılışta **otomatik** bağlanır,
değilse Ayarlar › Google Play Games'te "Giriş yap" düğmesi çıkar. Bağlıyken başarımlar açılır, liderlik
tablolarına skor gider; Ayarlar'dan Google'ın başarım ve liderlik ekranları açılır. Giriş zorunlu değil.

Kodda nerede:

| Yer | Ne yapar |
|---|---|
| `android/src/res/values/games-ids.xml` | Proje kimliği + başarım/liderlik kimlikleri. **Proje kimliği boşsa Play Games tamamen kapalı** (oyun eskisi gibi çalışır). |
| `android/src/java/.../App.java` | Proje kimliği varsa `PlayGamesSdk.initialize`. |
| `android/src/java/.../PlayGamesBridge.java` | `window.AndroidGames` köprüsü: giriş, `unlock`, `submitScore`, ekranlar. |
| `game/index.html` › `PGS_ACH`, `PGS_LB` | Hangi başarımın ne zaman açıldığı, skorların ne olduğu. Her saniye kontrol; her girişte hepsi yeniden gönderilir (girişsiz kazanılanlar da sayılır). |

## Play Console'da yapılacaklar (Onur)

Play Games, Play Console'da ayrı bir proje olarak kurulur. Sıra önemli: önce imza anahtarlarının SHA-1'leri lazım.

1. **SHA-1'leri al.**
   - *Play uygulama imzalama anahtarı* (Play'den kurulan sürümler bununla imzalanır): Play Console › Concrete Empire ›
     Test ve yayınla › Uygulama bütünlüğü › **Uygulama imzalama** › "Uygulama imzalama anahtarı sertifikası" › SHA-1.
     (İlk AAB yüklenmiş olmalı; kapalı test sürümü yüklendiyse hazırdır.)
   - *Yükleme anahtarı* (`katkarsiligi`, itch.io/elden kurulan APK'lar): aynı sayfada "Yükleme anahtarı sertifikası"
     › SHA-1, ya da `keytool -list -v -keystore katkarsiligi-release.jks -alias katkarsiligi`.
2. **Play Games projesini oluştur.** Play Console › Concrete Empire › Büyüme (Grow users) › **Play Games Hizmetleri** ›
   Kurulum ve yönetim › Yapılandırma. "Hayır, oyunum henüz Google API'lerini kullanmıyor" › yeni proje oluştur.
   Bu, arka planda bir Google Cloud projesi açar.
3. **OAuth izin ekranı.** Yapılandırma sayfasındaki bağlantıyla Google Cloud Console'a geç › OAuth izin ekranı:
   Kullanıcı türü **Harici**, uygulama adı `Concrete Empire`, destek e-postası, geliştirici e-postası. Kapsam ekleme.
   Kaydet. (Yayınlama durumu "Test" kalabilir; Play Games girişi için doğrulama gerekmez.)
4. **Kimlik bilgisi (Android).** Play Console › Play Games › Yapılandırma › **Kimlik bilgisi ekle** › Tür: Android ›
   "OAuth istemcisi oluştur" (Cloud Console'da: Android, paket `com.onur.concreteempire`, 1. adımdaki
   **Play imzalama SHA-1**'i) › geri dönüp listeden o istemciyi seç › Kaydet.
   Aynısını **yükleme anahtarı SHA-1**'iyle ikinci bir kimlik bilgisi olarak tekrarla (itch.io APK'sı da giriş yapabilsin).
   CI'ın test anahtarıyla imzalanan `-TESTKEY` APK'lar Play Games'e giremez; bu normal.
5. **Başarımlar.** Play Games › Başarımlar › Başarım oluştur. Aşağıdaki 13'ü (en az 5 tane şart). Ad/açıklama Türkçe,
   "Çeviri ekle" ile İngilizce. Puanlar 5'in katı, toplam 540 (sınır 1000). Simge isteğe bağlı (512×512 PNG).
6. **Liderlik tabloları.** Play Games › Liderlik tabloları › oluştur: aşağıdaki 3'ü. Biçim **Sayısal**, ondalık **0**,
   sıralama **Büyük daha iyi**.
7. **Test kullanıcıları.** Play Games › **Test kullanıcıları** › kendi hesabını ve kapalı test grubunu ekle.
   Play Games yayınlanana kadar yalnız bunlar giriş yapabilir.
8. **Kimlikleri bana gönder.** Yapılandırma sayfasındaki **Proje kimliği** (rakamlar) ve Başarımlar/Liderlik
   sayfasındaki **"Kaynakları al" (Get resources)** düğmesinin verdiği XML'i thread'e yapıştır; `games-ids.xml`'i
   ben doldururum. (Kendin yapacaksan: değerleri `ach_*` / `lb_*` satırlarına aşağıdaki tabloya göre yaz, ad değiştirme.)
9. **Veri güvenliği formunu güncelle** (`README.md` › Veri güvenliği tablosu) ve gizlilik politikası sayfası
   (`docs/privacy.html`, bu PR'la güncellendi) yayında olsun.
10. **Yayınla.** Kimlikli sürüm kapalı testte çalışınca Play Games › **Yayınlama** › Play Games'i yayınla
    (uygulamadan ayrı yayınlanır, inceleme kısa). Yayından sonra başarımlar silinemez, sadece gizlenebilir.

## Başarımlar

| Anahtar (`ach_…`) | Ad (TR) | Açıklama (TR) | Name (EN) | Description (EN) | Puan | Ne zaman |
|---|---|---|---|---|---|---|
| `first_handover` | İlk teslim | İlk binanı teslim et. | First handover | Hand over your first building. | 10 | Biten proje ≥ 1 |
| `projects_10` | Mahalle müteahhidi | Tek şirketle 10 proje bitir. | Neighbourhood builder | Finish 10 projects with one firm. | 20 | Biten proje ≥ 10 |
| `projects_50` | Şehrin yüzü | Tek şirketle 50 proje bitir. | Face of the city | Finish 50 projects with one firm. | 50 | Biten proje ≥ 50 |
| `level_5` | Büyüyen firma | Firma seviyesi 5'e ulaş. | Growing firm | Reach Firm level 5. | 15 | Seviye ≥ 5 |
| `level_10` | Köklü firma | Firma seviyesi 10'a ulaş. | Established firm | Reach Firm level 10. | 30 | Seviye ≥ 10 |
| `level_20` | Devletin müteahhidi | Firma seviyesi 20'ye ulaş, devlet ihalelerini aç. | Government contractor | Reach Firm level 20 and unlock government tenders. | 60 | Seviye ≥ 20 |
| `level_30` | İnşaat devi | Firma seviyesi 30'a ulaş. | Construction giant | Reach Firm level 30. | 100 | Seviye ≥ 30 |
| `networth_100m` | Yüz milyonluk firma | Net değerini 100 milyona çıkar. | Hundred-million firm | Grow your net worth to 100 million. | 15 | Net değer ≥ 100 mn |
| `networth_1b` | Milyarder | Net değerini 1 milyara çıkar. | Billionaire | Grow your net worth to 1 billion. | 40 | ≥ 1 mr |
| `networth_1t` | Trilyoner | Net değerini 1 trilyona çıkar. | Trillionaire | Grow your net worth to 1 trillion. | 100 | ≥ 1.000.000 mn |
| `reputation_90` | Güvenilir isim | İtibarını 90'a çıkar. | Trusted name | Raise your Reputation to 90. | 40 | İtibar ≥ 90 |
| `brand_1` | Marka doğuyor | Yeni şirket kurup ilk marka puanını kazan. | A brand is born | Start a new firm and earn your first brand point. | 30 | Marka ≥ 1 |
| `streak_7` | Bir hafta aralıksız | 7 gün üst üste tüm günlük görevleri bitir. | Seven-day streak | Finish every daily task 7 days in a row. | 30 | Günlük seri rekoru ≥ 7 |

## Liderlik tabloları

| Anahtar (`lb_…`) | Ad (TR) | Name (EN) | Skor |
|---|---|---|---|
| `net_worth` | Net değer (milyon) | Net worth (millions) | Şu anki net değer, milyon olarak tam sayı |
| `firm_level` | Firma seviyesi | Firm level | Firma seviyesi |
| `lifetime_earnings` | Toplam kazanç (milyon) | Lifetime earnings (millions) | Tüm şirketler boyunca kazanılan toplam, milyon |

Skorlar en fazla dakikada bir ve yalnız arttığında gönderilir; Play Games her oyuncunun en iyisini tutar.

## Sonraya kalan

- **Bulut kaydı (Saved Games):** Kayıt hâlâ yalnız cihazda. Play Games'in Saved Games API'siyle cihaz değişiminde
  ilerleme taşınabilir; ayrı bir iş olarak yapılabilir (Play Console'da "Kayıtlı oyunlar" açılmalı).
