package com.coffeeos.erp.di

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.coffeeos.erp.core.data.local.AppDatabase
import com.coffeeos.erp.core.data.local.MIGRATION_4_5
import com.coffeeos.erp.core.data.local.MIGRATION_5_6
import com.coffeeos.erp.core.data.local.MIGRATION_6_7
import com.coffeeos.erp.core.data.local.PosDao
import com.coffeeos.erp.printing.FakePdfPrinter
import com.coffeeos.erp.printing.PrinterRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides @Singleton
    fun provideDb(@ApplicationContext ctx: Context): AppDatabase =
        Room.databaseBuilder(ctx, AppDatabase::class.java, "coffeeos.db")
            // Migrasi proper — JANGAN pakai fallbackToDestructiveMigration di production
            // (akan hapus semua data lokal saat version DB naik)
            .addMigrations(MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7)
            // Fallback hanya untuk install baru (belum ada DB sama sekali)
            .fallbackToDestructiveMigrationOnDowngrade()
            .addCallback(SeedCallback())
            .build()

    @Provides fun provideDao(db: AppDatabase): PosDao = db.posDao()

    @Provides @Singleton
    fun providePrinter(fake: FakePdfPrinter): PrinterRepository = fake

    @Provides @Singleton
    fun provideFirestore(): com.google.firebase.firestore.FirebaseFirestore =
        com.google.firebase.firestore.FirebaseFirestore.getInstance()

    @Provides @Singleton
    fun provideFirebaseAuth(): com.google.firebase.auth.FirebaseAuth =
        com.google.firebase.auth.FirebaseAuth.getInstance()
}

/** Seed demo 1 outlet agar APK langsung bisa didemokan offline. */
private class SeedCallback : RoomDatabase.Callback() {
    override fun onCreate(db: SupportSQLiteDatabase) {
        super.onCreate(db)
        // Seed via DAO menyusul di MainActivity (butuh coroutine); tabel kosong = aman.
        // Data demo diinsert oleh DemoSeeder saat pertama login (lihat core/data/seed).
    }
}

/** Scope aplikasi untuk seeder & sync. */
@Singleton
class AppScope @javax.inject.Inject constructor() {
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    fun launch(block: suspend () -> Unit) = scope.launch { block() }
}
