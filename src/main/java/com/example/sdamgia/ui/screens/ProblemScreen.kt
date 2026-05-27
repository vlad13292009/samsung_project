package com.example.sdamgia.ui.screens

import android.annotation.SuppressLint
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.sdamgia.viewmodel.GameViewModel

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun ProblemScreen(viewModel: GameViewModel, onNavigateBack: () -> Unit) {
    val session by viewModel.problemSession.observeAsState(com.example.sdamgia.model.ProblemSession())
    var userAnswer by remember { mutableStateOf("") }
    var showSolution by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (session.problem == null && !session.isLoading && session.error == null) viewModel.startSolving()
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Решение задач", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        Text("Реши задачу — получи награду!", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(8.dp))

        when {
            session.isLoading -> {
                Column(modifier = Modifier.weight(1f).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center) {
                    CircularProgressIndicator()
                    Spacer(Modifier.height(12.dp)); Text("Загружаем задачу...")
                }
            }
            session.error != null -> {
                Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)), shape = RoundedCornerShape(10.dp)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Ошибка", fontWeight = FontWeight.Bold, color = Color.Red)
                            Spacer(Modifier.height(4.dp)); Text(session.error ?: "", fontSize = 14.sp)
                            Spacer(Modifier.height(12.dp))
                            Button(onClick = { viewModel.startSolving() }, modifier = Modifier.fillMaxWidth()) { Text("Попробовать снова") }
                        }
                    }
                }
                TextButton(onClick = onNavigateBack, modifier = Modifier.fillMaxWidth()) { Text("← Назад") }
            }
            session.problem != null -> {
                val problem = session.problem!!

                Card(modifier = Modifier.weight(1f).fillMaxWidth(), shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Text("Задача #${problem.id}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp))

                        if (problem.html.isNotBlank()) {
                            val isDark = isSystemInDarkTheme()
                            val themedHtml = if (isDark) {
                                problem.html.replace("</style>",
                                    """body { color: #e0e0e0; background: transparent; }
                                        img { filter: brightness(0.9); }
                                        a { color: #90caf9; }
                                    </style>""")
                            } else problem.html

                            AndroidView(
                                modifier = Modifier.fillMaxSize().padding(horizontal = 4.dp),
                                factory = { ctx ->
                                    WebView(ctx).apply {
                                        webViewClient = WebViewClient()
                                        settings.javaScriptEnabled = false
                                        settings.builtInZoomControls = true
                                        settings.displayZoomControls = false
                                        settings.setSupportZoom(true)
                                        settings.loadWithOverviewMode = true
                                        settings.useWideViewPort = true
                                        setBackgroundColor(android.graphics.Color.TRANSPARENT)
                                        loadDataWithBaseURL("https://math-ege.sdamgia.ru", themedHtml, "text/html", "UTF-8", null)
                                    }
                                }
                            )
                        } else {
                            Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(10.dp)) {
                                Text(problem.text, fontSize = 14.sp)
                            }
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))

                when {
                    session.isSolved -> {
                        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)), shape = RoundedCornerShape(10.dp)) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("✅ Правильно!", fontWeight = FontWeight.Bold, color = Color(0xFF4CAF50), fontSize = 18.sp)
                                Text("Питомец накормлен! +очки, +опыт", fontSize = 13.sp, color = Color.Black)
                            }
                        }
                    }
                    session.attempts >= session.maxAttempts -> {
                        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0)), shape = RoundedCornerShape(10.dp)) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("❌ Неправильно", fontWeight = FontWeight.Bold, color = Color(0xFFFF5722), fontSize = 18.sp)
                                Text("Правильный ответ: ${problem.answer}", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color.Black)
                                Spacer(Modifier.height(4.dp))
                                Button(onClick = { showSolution = !showSolution }, modifier = Modifier.fillMaxWidth()) { Text(if (showSolution) "Скрыть решение" else "Показать решение") }
                                if (showSolution) { Spacer(Modifier.height(4.dp)); Text(problem.solution, fontSize = 13.sp) }
                            }
                        }
                    }
                    else -> {
                        OutlinedTextField(value = userAnswer, onValueChange = { userAnswer = it }, label = { Text("Ваш ответ") },
                            modifier = Modifier.fillMaxWidth(), singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = { if (userAnswer.isNotBlank()) viewModel.submitAnswer(userAnswer) }),
                            shape = RoundedCornerShape(10.dp), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF7E57C2)))
                        Spacer(Modifier.height(6.dp))
                        Button(onClick = { if (userAnswer.isNotBlank()) { viewModel.submitAnswer(userAnswer); userAnswer = "" } },
                            modifier = Modifier.fillMaxWidth().height(44.dp), shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7E57C2))) {
                            Text("Ответить (попытка ${session.attempts + 1}/${session.maxAttempts})", fontWeight = FontWeight.Bold) }
                    }
                }

                Spacer(Modifier.height(4.dp))
                TextButton(onClick = { viewModel.dismissProblem(); onNavigateBack() }, modifier = Modifier.fillMaxWidth()) { Text("← Закончить") }
            }
            else -> {
                Column(modifier = Modifier.weight(1f), verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center) {
                    Button(onClick = { viewModel.startSolving() }, modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7E57C2))) {
                        Text("Начать задачу", fontWeight = FontWeight.Bold, fontSize = 16.sp) }
                }
                TextButton(onClick = onNavigateBack, modifier = Modifier.fillMaxWidth()) { Text("← Назад к питомцу") }
            }
        }
    }
}
