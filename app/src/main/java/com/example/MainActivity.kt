package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.example.data.repository.DappRepository
import com.example.data.supabase.SupabaseConfig
import com.example.navigation.DappNavGraph
import com.example.ui.theme.DappTheme
import com.example.ui.theme.GraphiteVoid
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var repository: DappRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        repository = DappRepository(applicationContext)

        setContent {
            DappTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = GraphiteVoid
                ) {
                    DappNavGraph(repository = repository)
                }
            }
        }

        // Tangkap link konfirmasi email "dapp://login-callback#..." saat app baru dibuka dari link ini
        handleAuthDeeplink(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // Tangkap link konfirmasi email saat app sudah berjalan (singleTask)
        handleAuthDeeplink(intent)
    }

    private fun handleAuthDeeplink(intent: Intent?) {
        val data = intent?.data ?: return
        if (data.scheme == "dapp" && data.host == "login-callback") {
            lifecycleScope.launch {
                SupabaseConfig.client.handleDeeplinks(intent) {
                    // Sesi berhasil dibuat dari link konfirmasi email.
                    lifecycleScope.launch {
                        repository.onEmailConfirmed()
                    }
                }
            }
        }
    }
}
