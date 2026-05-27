package com.example.sdamgia.ui.screens

import android.content.res.AssetManager
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sdamgia.model.AnimationState
import com.example.sdamgia.ui.components.StatBar
import com.example.sdamgia.viewmodel.GameViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

private fun isBusy(state: com.example.sdamgia.model.GameState): Boolean =
    state.isDead || state.isSleeping || state.animationState in setOf(
        AnimationState.EATING, AnimationState.PLAYING, AnimationState.BATHING
    )

@Composable
fun MainGameScreen(
    viewModel: GameViewModel,
    onNavigateToShop: () -> Unit,
    onNavigateToProblem: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val state by viewModel.gameState.observeAsState(com.example.sdamgia.model.GameState())
    val isBlinking by viewModel.isBlinking.observeAsState(false)
    val blinkColorInt by viewModel.blinkColor.observeAsState()
    val showWakeDialog by viewModel.showWakeConfirmDialog.observeAsState(false)
    val context = LocalContext.current
    var frameIndex by remember { mutableIntStateOf(0) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var renameText by remember { mutableStateOf("") }

    val frameCount = viewModel.getAnimationFrameCount(state)
    val path = viewModel.getAnimationPath(state)
    val imageBitmap = remember(path, frameIndex, state.animationState) {
        loadPetImage(context.assets, path, frameCount, frameIndex)
    }

    LaunchedEffect(frameCount) {
        if (frameCount > 1) while (isActive) { delay(400L); frameIndex = (frameIndex + 1) % frameCount }
    }

    if (showRenameDialog) {
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text("Переименовать питомца") },
            text = {
                OutlinedTextField(
                    value = renameText, onValueChange = { renameText = it },
                    label = { Text("Новое имя") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { if (renameText.isNotBlank()) { viewModel.renamePet(renameText); showRenameDialog = false } }),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF7E57C2))
                )
            },
            confirmButton = {
                TextButton(onClick = { if (renameText.isNotBlank()) { viewModel.renamePet(renameText); showRenameDialog = false } }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showRenameDialog = false }) { Text("Отмена") } }
        )
    }

    if (showWakeDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.cancelWake() },
            title = { Text("Разбудить питомца?") },
            text = { Text("Питомец слишком устал. Если разбудить сейчас, счастье уменьшится на 15.") },
            confirmButton = { TextButton(onClick = { viewModel.confirmWake() }) { Text("Разбудить") } },
            dismissButton = { TextButton(onClick = { viewModel.cancelWake() }) { Text("Пусть спит") } }
        )
    }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(state.petName, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            TextButton(onClick = { renameText = state.petName; showRenameDialog = true }) { Text("✏️", fontSize = 16.sp) }
        }

        val status = getStatusText(state)
        if (status.isNotEmpty()) Text(status, fontSize = 14.sp, fontWeight = FontWeight.Medium,
            color = when { state.isDead -> Color.Red; state.isSleeping -> Color(0xFF64B5F6)
                state.lowStats.isNotEmpty() -> Color(0xFFFF5722); else -> MaterialTheme.colorScheme.onSurfaceVariant })

        Spacer(Modifier.height(8.dp))

        Box(modifier = Modifier.size(180.dp).background(Color(0xFFF5F5F5), RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center) {
            if (imageBitmap != null) {
                Image(bitmap = imageBitmap, contentDescription = "Pet",
                    modifier = Modifier.size(160.dp), contentScale = ContentScale.Fit)
                if (isBlinking && blinkColorInt != null) {
                    Box(modifier = Modifier.matchParentSize()
                        .background(Color(blinkColorInt!!).copy(alpha = 0.45f), RoundedCornerShape(12.dp)))
                }
            }
        }

        if (state.lowStats.isNotEmpty()) {
            Spacer(Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                if ("hunger" in state.lowStats) Text("🍖", fontSize = 18.sp)
                if ("happiness" in state.lowStats) Text("😢", fontSize = 18.sp)
                if ("hygiene" in state.lowStats) Text("💩", fontSize = 18.sp)
                if ("energy" in state.lowStats) Text("⚡", fontSize = 18.sp)
            }
        }

        Spacer(Modifier.height(4.dp))
        if (state.isDead) Button(onClick = { viewModel.revive() },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))) { Text("Воскресить") }

        Spacer(Modifier.height(8.dp))
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            Column(modifier = Modifier.padding(10.dp)) {
                StatBar("Сытость", state.hunger, color = Color(0xFFFF7043))
                StatBar("Счастье", state.happiness, color = Color(0xFFFFD54F))
                StatBar("Чистота", state.hygiene, color = Color(0xFF4DD0E1))
                StatBar("Энергия", state.energy, color = Color(0xFF81C784))
            }
        }

        Spacer(Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            StatBadge("Уровень", "${state.level}", Color(0xFF7E57C2))
            StatBadge("Очки", "${state.coins}", Color(0xFFFFA726))
            StatBadge("Опыт", "${state.experience}/${viewModel.expForNextLevel()}", Color(0xFF42A5F5))
        }

        Spacer(Modifier.height(12.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ActionButton("Кормить", Color(0xFFFF7043), onNavigateToProblem, !isBusy(state), Modifier.weight(1f))
            ActionButton("Играть", Color(0xFFFFD54F), { viewModel.play() }, !isBusy(state), Modifier.weight(1f))
        }
        Spacer(Modifier.height(4.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ActionButton("Мыть", Color(0xFF4DD0E1), { viewModel.bathe() }, !isBusy(state), Modifier.weight(1f))
            SleepActionButton(state.isSleeping, state.isDead, state.energy, state.animationState,
                onToggle = { viewModel.toggleSleep() }, Modifier.weight(1f))
        }

        Spacer(Modifier.height(4.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            TextButton(onClick = onNavigateToShop) { Text("Магазин") }
            TextButton(onClick = onNavigateToSettings) { Text("Настройки") }
        }
        Spacer(Modifier.height(16.dp))
    }
}

private fun getStatusText(state: com.example.sdamgia.model.GameState): String {
    if (state.isDead) return "Питомец умер!"
    if (state.isSleeping) return "Спит Хр-р-р..."
    return when (state.animationState) {
        AnimationState.EATING -> "Кушает"
        AnimationState.PLAYING -> "Играет"
        AnimationState.BATHING -> "Купается"
        else -> when {
            "hunger" in state.lowStats -> "Хочет кушать!"
            "happiness" in state.lowStats -> "Хочет играть!"
            "hygiene" in state.lowStats -> "Хочет мыться!"
            "energy" in state.lowStats -> "Хочет спать!"
            else -> ""
        }
    }
}

private fun loadPetImage(assets: AssetManager, path: String, frameCount: Int, frameIndex: Int): ImageBitmap? {
    return try {
        val p = if (frameCount > 1) "$path${frameIndex + 1}.png" else path
        val s = assets.open(p); val b = BitmapFactory.decodeStream(s); s.close(); b?.asImageBitmap()
    } catch (_: Exception) {
        try { val s = assets.open("images/default.png"); val b = BitmapFactory.decodeStream(s); s.close(); b?.asImageBitmap() } catch (_: Exception) { null }
    }
}

@Composable
fun StatBadge(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = color)
        Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun ActionButton(text: String, color: Color, onClick: () -> Unit, enabled: Boolean, modifier: Modifier = Modifier) {
    Button(onClick = onClick, enabled = enabled, modifier = modifier.height(40.dp), shape = RoundedCornerShape(10.dp),
        colors = ButtonDefaults.buttonColors(containerColor = color, disabledContainerColor = color.copy(alpha = 0.4f))) {
        Text(text, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White) }
}

@Composable
fun SleepActionButton(isSleeping: Boolean, isDead: Boolean, energy: Float, animState: AnimationState, onToggle: () -> Unit, modifier: Modifier = Modifier) {
    val text = if (isSleeping) "Будить" else "Спать"
    val color = Color(0xFF64B5F6)
    val baseEnabled = !isDead && (isSleeping || animState == AnimationState.IDLE)

    val isHardLocked = isSleeping && energy < 20
    val isSoftLocked = isSleeping && energy in 20f..<60f
    val isFullyEnabled = isSleeping && energy >= 60f

    val btnEnabled = when {
        isSleeping.not() -> baseEnabled
        !isSleeping -> false
        isHardLocked -> false
        isSoftLocked -> true
        isFullyEnabled -> true
        else -> false
    }

    Box(modifier = modifier.height(40.dp)) {
        Button(
            onClick = onToggle,
            enabled = btnEnabled,
            modifier = Modifier.fillMaxSize(),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = color,
                disabledContainerColor = color.copy(alpha = 0.4f)
            )
        ) {
            Text(text, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                color = when { isSoftLocked -> Color.White.copy(alpha = 0.5f)
                    else -> Color.White })
        }

        if (isSoftLocked) {
            Canvas(modifier = Modifier.matchParentSize().padding(2.dp).clip(RoundedCornerShape(8.dp))) {
                val gap = 20f
                var startX = -size.height
                while (startX < size.width + size.height) {
                    drawLine(color = Color(0x55000000), start = Offset(startX, 0f), end = Offset(startX + size.height, size.height),
                        strokeWidth = 6f)
                    startX += gap
                }
            }
        }
    }
}
