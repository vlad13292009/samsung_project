package com.example.sdamgia

import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import com.example.sdamgia.data.StatsOverlayService
import com.example.sdamgia.ui.navigation.AppNavGraph
import com.example.sdamgia.ui.theme.SdamgiaTheme
import com.example.sdamgia.viewmodel.GameViewModel

class MainActivity : ComponentActivity() {

    private var gameViewModel: GameViewModel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val viewModel: GameViewModel = viewModel()
            gameViewModel = viewModel

            SdamgiaTheme {
                val navController = rememberNavController()
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    AppNavGraph(
                        navController = navController,
                        viewModel = viewModel,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        gameViewModel?.soundManager?.playMusic()
        StatsOverlayService.stop(this)
    }

    override fun onStop() {
        super.onStop()
        gameViewModel?.soundManager?.pauseMusic()
        val vm = gameViewModel
        if (vm != null && vm.isOverlayEnabled() && canDrawOverlays()) {
            StatsOverlayService.start(this)
        } else {
            StatsOverlayService.stop(this)
        }
    }

    private fun canDrawOverlays(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) Settings.canDrawOverlays(this) else true
    }
}
