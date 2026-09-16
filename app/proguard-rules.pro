# CoffeeOS ERP — minify nonaktif di build.gradle, file ini wajib ada untuk build release.
# Pertahankan model Room/Hilt/Firebase jika minify dinyalakan suatu saat.
-keep class com.coffeeos.erp.core.data.local.** { *; }
-keep class com.coffeeos.erp.core.domain.** { *; }
-keep class androidx.room.** { *; }
-dontwarn org.conscrypt.**
