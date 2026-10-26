package app.moviematch

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Edge-to-edge with `SystemBarStyle.auto`: the system bar icons follow the device's
        // light/dark mode, which is the same signal `MovieMatchTheme` uses (plus Material You on
        // Android 12+).
        enableEdgeToEdge()
        setContent {
            App()
        }
    }
}
