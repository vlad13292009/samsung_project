package com.example.sdamgia.ui.screens

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sdamgia.data.StatsOverlayService
import com.example.sdamgia.viewmodel.GameViewModel

@Composable
fun SettingsScreen(viewModel: GameViewModel, onNavigateBack: () -> Unit) {
    val state by viewModel.gameState.observeAsState(com.example.sdamgia.model.GameState())
    val context = LocalContext.current
    var showOverlayDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Настройки", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(24.dp))

        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Звуковые эффекты", fontSize = 15.sp)
                    Button(onClick = { viewModel.toggleSoundEffects() },
                        colors = ButtonDefaults.buttonColors(containerColor = if (state.isSoundEnabled) Color(0xFF4CAF50) else Color.Gray),
                        shape = RoundedCornerShape(8.dp)) { Text(if (state.isSoundEnabled) "ВКЛ" else "ВЫКЛ", fontWeight = FontWeight.Bold) }
                }
                Spacer(Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Фоновая музыка", fontSize = 15.sp)
                    Button(onClick = { viewModel.toggleMusic() },
                        colors = ButtonDefaults.buttonColors(containerColor = if (state.isMusicEnabled) Color(0xFF4CAF50) else Color.Gray),
                        shape = RoundedCornerShape(8.dp)) { Text(if (state.isMusicEnabled) "ВКЛ" else "ВЫКЛ", fontWeight = FontWeight.Bold) }
                }
                Spacer(Modifier.height(16.dp))
                Text("Громкость музыки", fontSize = 15.sp)
                Slider(value = state.musicVolume, onValueChange = { viewModel.setMusicVolume(it) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = SliderDefaults.colors(thumbColor = Color(0xFF7E57C2), activeTrackColor = Color(0xFF7E57C2)))
                Text("${(state.musicVolume * 100).toInt()}%", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Оверлей в свёрнутом виде", fontSize = 15.sp)
                    Button(onClick = {
                        if (!state.isOverlayEnabled) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(context)) {
                                showOverlayDialog = true
                            } else {
                                viewModel.setOverlayEnabled(true)
                                StatsOverlayService.start(context)
                            }
                        } else {
                            viewModel.setOverlayEnabled(false)
                            StatsOverlayService.stop(context)
                        }
                    },
                        colors = ButtonDefaults.buttonColors(containerColor = if (state.isOverlayEnabled) Color(0xFF4CAF50) else Color.Gray),
                        shape = RoundedCornerShape(8.dp)) { Text(if (state.isOverlayEnabled) "ВКЛ" else "ВЫКЛ", fontWeight = FontWeight.Bold) }
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Кэш задач", fontSize = 15.sp)
                Spacer(Modifier.height(4.dp))
                Text("${viewModel.getCacheSizeMb()} МБ / ${state.maxCacheSizeMb} МБ", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Slider(value = state.maxCacheSizeMb.toFloat(), onValueChange = { viewModel.setMaxCacheSizeMb(it.toInt()) },
                    valueRange = 1f..50f, steps = 48,
                    modifier = Modifier.fillMaxWidth(),
                    colors = SliderDefaults.colors(thumbColor = Color(0xFF7E57C2), activeTrackColor = Color(0xFF7E57C2)))
                Text("Загруженные задачи старше остальных будут удалены", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        Spacer(Modifier.height(32.dp))
        TextButton(onClick = onNavigateBack, modifier = Modifier.fillMaxWidth()) { Text("← Назад") }
        Spacer(Modifier.height(16.dp))
    }

    if (showOverlayDialog) {
        AlertDialog(
            onDismissRequest = { showOverlayDialog = false },
            title = { Text("Требуется разрешение") },
            text = { Text("Для оверлея необходимо разрешение «Поверх других приложений». Разрешить его в настройках?") },
            confirmButton = {
                TextButton(onClick = {
                    showOverlayDialog = false
                    val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}"))
                    context.startActivity(intent)
                }) { Text("Открыть настройки") }
            },
            dismissButton = { TextButton(onClick = { showOverlayDialog = false }) { Text("Отмена") } }
        )
    }
}
