package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.data.repository.DappRepository
import com.example.navigation.DappNavGraph
import com.example.ui.theme.DappTheme
import com.example.ui.theme.GraphiteVoid

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
    }
}
