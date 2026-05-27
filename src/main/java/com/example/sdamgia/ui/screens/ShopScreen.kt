package com.example.sdamgia.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sdamgia.model.ShopItem
import com.example.sdamgia.viewmodel.GameViewModel

@Composable
fun ShopScreen(viewModel: GameViewModel, onNavigateBack: () -> Unit) {
    val state by viewModel.gameState.observeAsState(com.example.sdamgia.model.GameState())
    val shopAvailable = viewModel.isShopAvailable()

    Column(modifier = Modifier.fillMaxSize().padding(start = 16.dp, top = 16.dp, end = 16.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Магазин", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text("💰 ${state.coins}", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFFA726))
        }
        Spacer(Modifier.height(4.dp))
        Text("Очки тратятся на улучшения", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

        if (!shopAvailable) {
            Spacer(Modifier.height(24.dp))
            Text("Магазин откроется на ${GameViewModel.SHOP_MIN_LEVEL} уровне (текущий: ${state.level})",
                fontSize = 15.sp, color = Color(0xFFFFA726), fontWeight = FontWeight.Medium)
        }

        Spacer(Modifier.height(12.dp))

        LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(ShopItem.getItems()) { item ->
                val levelReq = (item.id + 5) / 2 + 4
                val available = shopAvailable && state.level >= levelReq
                ShopItemCard(item = item, isPurchased = item.id in state.purchasedUpgrades,
                    canAfford = state.coins >= item.price, isAvailable = available,
                    levelReq = levelReq, currentLevel = state.level, onBuy = { viewModel.buyUpgrade(item.id) })
            }
        }

        TextButton(onClick = onNavigateBack, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) { Text("← Назад к питомцу") }
    }
}

@Composable
fun ShopItemCard(item: ShopItem, isPurchased: Boolean, canAfford: Boolean, isAvailable: Boolean, levelReq: Int, currentLevel: Int, onBuy: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = when { isPurchased -> Color(0xFFE8F5E9)
            !isAvailable -> Color(0xFFF5F5F5); else -> MaterialTheme.colorScheme.surfaceVariant })) {
        Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(item.name, fontWeight = FontWeight.Bold, fontSize = 15.sp,
                    color = if (!isAvailable && !isPurchased) Color.Gray else MaterialTheme.colorScheme.onSurface)
                Text(item.description, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(item.effect, fontSize = 11.sp, color = if (isPurchased) Color(0xFF4CAF50) else Color(0xFF7E57C2))
                if (!isAvailable && !isPurchased) Text("Требуется ${levelReq}-й уровень", fontSize = 11.sp, color = Color(0xFFFF5722))
            }
            Spacer(Modifier.width(8.dp))
            when { isPurchased -> Text("Куплено", color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                !isAvailable -> Text("🔒", fontSize = 20.sp)
                else -> Button(onClick = onBuy, enabled = canAfford, shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFA726), disabledContainerColor = Color.Gray.copy(alpha = 0.3f))) {
                    Text("${item.price} 💰", fontSize = 13.sp, fontWeight = FontWeight.Bold) }
            }
        }
    }
}
