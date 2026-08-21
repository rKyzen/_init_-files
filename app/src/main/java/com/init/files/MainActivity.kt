package com.init.files

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.init.files.data.local.InitDatabaseHelper
import com.init.files.data.local.LocalRepository
import com.init.files.theme.InitTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val dbHelper = InitDatabaseHelper(this)
        val localRepository = LocalRepository(dbHelper)

        setContent {
            LaunchedEffect(Unit) {
                localRepository.initialize()
            }

            val systemInDark = isSystemInDarkTheme()
            val themeMode by localRepository.themeModeFlow.collectAsState(initial = "DARK")

            val isDark = when (themeMode) {
                "LIGHT" -> false
                "SYSTEM" -> systemInDark
                else -> true
            }

            InitTheme(darkTheme = isDark) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainNavigation(localRepository = localRepository)
                }
            }
        }
    }
}
