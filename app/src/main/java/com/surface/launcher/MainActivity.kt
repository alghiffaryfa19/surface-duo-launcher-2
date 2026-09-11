package com.surface.launcher

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.microsoft.device.dualscreen.twopanelayout.TwoPaneLayout
import com.surface.launcher.ui.theme.DualScreenExperienceTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DualScreenExperienceTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.Transparent
                ) {
                    LauncherApp()
                }
            }
        }
    }
}

@Composable
fun LauncherApp() {
    TwoPaneLayout(
        pane1 = { Pane1Content() },
        pane2 = { Pane2Content() }
    )
}

@Composable
fun Pane1Content() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text("Pane 1 (Empty)", color = Color.White)
    }
}

@Composable
fun Pane2Content() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text("Pane 2 (App Grid)", color = Color.White)
    }
}
