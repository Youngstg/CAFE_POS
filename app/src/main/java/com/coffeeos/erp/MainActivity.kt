package com.coffeeos.erp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.coffeeos.erp.ui.navigation.CoffeeosNavGraph
import com.coffeeos.erp.ui.theme.CoffeeosTheme
import dagger.hilt.android.AndroidEntryPoint

/** Single-Activity Compose. Navigasi berbasis role (lihat CoffeeosNavGraph). */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CoffeeosTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    CoffeeosNavGraph()
                }
            }
        }
    }
}
