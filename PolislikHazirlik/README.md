# Polislik Sınavına Hazırlık (Android, çevrimdışı)

Kotlin + Jetpack Compose + Room (SQLite). İnternet izni bile yoktur.

## Telefona kurma
1. Android Studio'yu (Koala veya üzeri) aç → **Open** → bu klasörü seç. İlk açılışta Gradle senkronizasyonu için internet gerekir (yalnızca derleme sırasında).
2. Telefonda *Geliştirici seçenekleri → USB hata ayıklama*'yı aç, kabloyla bağla, üstte telefonu seçip **Run ▶** de.
3. Ya da APK üret: **Build → Build APK(s)** → `app/build/outputs/apk/debug/app-debug.apk` dosyasını telefona atıp kur.

## Not
Veritabanı: `polislik.db` (uygulamanın özel alanı). Uygulamayı kaldırırsan veriler silinir; güncellemelerin üstüne kurarsan korunur.
