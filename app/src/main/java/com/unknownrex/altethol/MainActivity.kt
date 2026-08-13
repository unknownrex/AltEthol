package com.unknownrex.altethol

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.unknownrex.altethol.core.ui.theme.AltEtholTheme
import com.unknownrex.altethol.navigation.AppNavHost

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AltEtholTheme {
                AppNavHost()
            }
        }
    }
}
