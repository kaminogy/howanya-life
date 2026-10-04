package com.kamino.howanyalife.mobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.glance.appwidget.updateAll
import androidx.lifecycle.lifecycleScope
import com.kamino.howanyalife.mobile.ui.HowanyaTheme
import com.kamino.howanyalife.mobile.ui.RoomScreen
import com.kamino.howanyalife.mobile.widget.HowanyaWidget
import com.kamino.howanyalife.ui.CatViewModel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val viewModel: CatViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HowanyaTheme {
                RoomScreen(viewModel)
            }
        }
    }

    override fun onStop() {
        super.onStop()
        // Show the state just saved on pause in the home-screen widget.
        lifecycleScope.launch {
            viewModel.awaitSaved()
            HowanyaWidget().updateAll(applicationContext)
        }
    }
}
