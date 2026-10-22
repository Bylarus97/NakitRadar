# NakitRadar — Android Kampanya Keşif & Nakit İade (Cashback) Strateji Uygulaması

**NakitRadar**, telefonunuzda yüklü olan operatör (**Turkcell**), ön ödemeli nakit iade cüzdanı (**Paycell, Nays, Papara, Tosla, Hadi, Pokus**) ve banka/kart kampanya uygulamalarını (**Garanti BonusFlaş, Akbank Juzdan, Yapı Kredi World Mobil, İş Bankası Maximum, Ziraat Bankkart, QNB**) tarayarak:

1. **Uygulama İçi Gizli Sekmelerdeki Kampanyaları Gösterir:** Hangi kampanyanın uygulamanın hangi gizli menüsünde (*Bana Özel, Senin İçin, Fırsatlar, Pasaj, Hediye Dünyası*) olduğunu adım adım listeler.
2. **Nakit İade & Kombo (Stacking) Stratejisi Üretir:**
   - **⚡ Turkcell + Paycell + Banka Sinerji Zinciri:** Örneğin Turkcell faturasını veya Pasaj alışverişini Paycell üzerinden ödeyerek iki uygulamanın nakit iade kampanyasını aynı anda tetikleme.
   - **🔥 Çifte Kazanç Kombosu (Stacking):** Banka kartı kampanyası + ön ödemeli cüzdan (Paycell / Nays) nakit iadesini birleştirme.
   - **✂️ Harcama Bölme (Split-Payment) Stratejisi:** Yüksek tutarlı alışverişleri kampanya üst limitlerine takılmamak için 2-3 kart/cüzdan arasında optimum parçalara bölme.
3. **Otomatik Bildirim Yakalayıcı (`NotificationListenerService`) & AI Kampanya Analizi:** Banka ve cüzdan uygulamalarından gelen bildirimleri veya yapıştırılan kampanya metinlerini otomatik çözümleyip strateji motoruna ekler.

---

## Hemen Deneme ve APK İndirme

- **Canlı Tarayıcı Simülatörü:** [`preview/index.html`](preview/index.html) dosyasını tarayıcıda açarak tüm özellikleri anında test edebilirsiniz.
- **Otomatik Android APK (GitHub Actions):** Bu depoda yer alan [`.github/workflows/build-apk.yml`](.github/workflows/build-apk.yml) iş akışı, her güncellemede **Actions** sekmesi altında `NakitRadar-Android-APK` (`app-debug.apk`) dosyasını otomatik derler.
