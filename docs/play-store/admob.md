# AdMob kurulumu (ödüllü reklam)

Kod hazır (v2.4): `AdsBridge.java` + `android/src/res/values/admob-ids.xml`. Şu an dosyada Google'ın **test
kimlikleri** var; uygulama her yerde "Test Ad" gösterir ve para kazandırmaz. Gerçek gelir için:

## 1. AdMob hesabı ve uygulama
1. <https://admob.google.com> → Google hesabınla kaydol (ödeme bilgileri, vergi formu sonra da girilebilir).
2. **Uygulamalar › Uygulama ekle** → Android → "Uygulama Google Play'de listeleniyor mu?" → Evet, `com.onur.concreteempire`'yi seç
   (henüz yayında görünmüyorsa "Hayır" deyip sonra mağazaya bağla).
3. **Uygulama ayarları**'ndaki **Uygulama kimliği** (`ca-app-pub-…~…`, tilde ile) → `admob_app_id`.

## 2. Ödüllü reklam birimi
1. Uygulama › **Reklam birimleri › Reklam birimi ekle › Ödüllü (Rewarded)**.
2. Ad: `rewarded_main`. Ödül ayarı önemsiz (oyun ödülü kendisi veriyor), örn. 1 × "reward".
3. **Reklam birimi kimliği** (`ca-app-pub-…/…`, eğik çizgi ile) → `admob_rewarded_unit_id`.

İkisini `android/src/res/values/admob-ids.xml`'e yaz (ya da bana gönder, ben PR'la eklerim). Kimlikler gizli değil,
repoya girebilir.

## 3. Gizlilik mesajı (GDPR, AB/BK)
AdMob › **Gizlilik ve mesajlaşma › Avrupa düzenlemeleri** → mesaj oluştur, uygulamayı seç, gizlilik politikası
URL'si `https://theaob.github.io/concrete-empire/privacy.html`, yayınla. Bu yoksa AB'deki oyunculara form çıkmaz
ve sınırlı reklam gelir. Oyun formu kendisi gösterir; Ayarlar › Reklamlar › Gizlilik seçenekleri ile tekrar açılır.

## 4. app-ads.txt (önerilir)
AdMob, Play Store'daki **geliştirici web sitesinin kök alanında** `app-ads.txt` arar
(ör. `https://ornek.com/app-ads.txt`). `theaob.github.io/concrete-empire` alt yol olduğu için sayılmaz; ya
`theaob.github.io` adlı bir repo açıp köküne koymak ya da kendi alan adını kullanmak gerekir. Satır AdMob'da
**Uygulamalar › app-ads.txt** altında hazır verilir. Eksik olması reklamı durdurmaz ama doldurma oranını düşürür.

## 5. Test
- Kendi telefonunda **gerçek** kimliklerle sürekli reklam tıklama/izleme yapma (hesap askıya alınabilir). AdMob ›
  **Ayarlar › Test cihazları**'na telefonunu ekle; o cihazda gerçek kimlikle de test reklamı gelir.
- Yeni reklam birimi ilk saatlerde "reklam yok" dönebilir; oyunda "Şu an reklam yok, biraz sonra tekrar dene" yazar.

## 6. Play Console (v2.4 yayınlanırken, aynı anda)
- Uygulama içeriği › **Reklamlar: Evet**.
- Uygulama içeriği › **Reklam kimliği: Evet**, amaç "Reklam veya pazarlama" (manifest'te `AD_ID` izni var;
  beyan "Hayır" kalırsa Play sürümü reddeder).
- **Veri güvenliği**: `README.md` › "AdMob (v2.4)" tablosundaki satırları ekle.
