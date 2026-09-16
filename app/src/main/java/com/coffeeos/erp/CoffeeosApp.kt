package com.coffeeos.erp

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/** Entry point Hilt. Semua Repository/ViewModel di-inject dari sini. */
@HiltAndroidApp
class CoffeeosApp : Application()
