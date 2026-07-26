package com.opotest

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.opotest.ui.navigation.NavGraph
import com.opotest.ui.theme.OPOTESTTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            OPOTESTTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    NavGraph()
                }
            }
        }
    }
}
