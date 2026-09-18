package com.coffeeos.erp

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/** Entry point Hilt + WorkManager (HiltWorkerFactory) + Firestore offline persistence. */
@HiltAndroidApp
class CoffeeosApp : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        // Cache offline Firestore: baca tetap jalan saat internet mati.
        FirebaseFirestore.getInstance().firestoreSettings =
            FirebaseFirestoreSettings.Builder().setPersistenceEnabled(true).build()
    }
}
