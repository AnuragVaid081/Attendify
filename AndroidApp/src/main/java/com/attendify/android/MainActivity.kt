package com.attendify.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.attendify.android.navigation.AttendifyNavGraph
import com.attendify.android.ui.theme.AttendifyTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AttendifyTheme(darkTheme = true) {
                AttendifyNavGraph()
            }
        }
    }
}
