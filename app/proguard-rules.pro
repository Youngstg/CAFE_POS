# ===================================================================
# CoffeeOS ERP (SuKopi POS) — Production ProGuard / R8 Rules
# ===================================================================

# --- Room SQLite Database ---
-keep class com.coffeeos.erp.core.data.local.** { *; }
-keep class * extends androidx.room.RoomDatabase
-keep class * extends androidx.room.Dao
-dontwarn androidx.room.paging.**

# --- Domain & Model Classes (JSON / DataStore / Sync) ---
-keep class com.coffeeos.erp.core.domain.** { *; }
-keepclassmembers class * implements java.io.Serializable { *; }
-keepclassmembers enum * { *; }

# --- Dagger / Hilt Dependency Injection ---
-keep class * extends androidx.hilt.work.HiltWorker { *; }
-keep class dagger.hilt.** { *; }
-keep class com.coffeeos.erp.di.** { *; }
-keepclassmembers class * {
    @javax.inject.Inject <init>(...);
    @dagger.Provides *;
}

# --- WorkManager ---
-keep class androidx.work.** { *; }
-keep class * extends androidx.work.Worker { *; }
-keep class * extends androidx.work.CoroutineWorker { *; }
-keep class * extends androidx.work.ListenableWorker { *; }

# --- Firebase Services (Firestore, Auth, Messaging, Analytics, Crashlytics) ---
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.firebase.**
-dontwarn com.google.android.gms.**

# --- Kotlin Coroutines ---
-keepclassmembers class kotlinx.coroutines.** { *; }
-dontwarn kotlinx.coroutines.**

# --- Jetpack Compose & Material 3 ---
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# --- CameraX & ML Kit Barcode ---
-keep class com.google.mlkit.** { *; }
-keep class androidx.camera.** { *; }
-dontwarn com.google.mlkit.**
-dontwarn androidx.camera.**

# --- Network & SSL ---
-dontwarn org.conscrypt.**
-dontwarn okhttp3.**
