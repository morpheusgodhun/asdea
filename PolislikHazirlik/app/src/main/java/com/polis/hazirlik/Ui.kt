package com.polis.hazirlik

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

val LETTERS = listOf("A", "B", "C", "D", "E")
private val TR = Locale("tr", "TR")

fun formatDate(ms: Long): String = SimpleDateFormat("dd.MM.yyyy HH:mm", TR).format(Date(ms))
fun pct(v: Double): String = String.format(TR, "%%%.1f", v)
fun Question.options() = listOf(optA, optB, optC, optD, optE)
fun AttemptQuestion.options() = listOf(optA, optB, optC, optD, optE)

private val GreenBg = Color(0xFFDDF3E0)
private val GreenFg = Color(0xFF2E7D32)
private val RedBg = Color(0xFFFADBD8)
private val RedFg = Color(0xFFC62828)

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Color(0xFF1F3A68),
            onPrimary = Color.White,
            primaryContainer = Color(0xFFD9E3F8),
            onPrimaryContainer = Color(0xFF0B1C3A),
            secondaryContainer = Color(0xFFE3E8F2)
        ),
        content = content
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppBar(title: String, onBack: (() -> Unit)? = null, actions: @Composable RowScope.() -> Unit = {}) {
    TopAppBar(
        title = { Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        navigationIcon = {
            if (onBack != null) IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri")
            }
        },
        actions = actions,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primary,
            titleContentColor = Color.White,
            navigationIconContentColor = Color.White,
            actionIconContentColor = Color.White
        )
    )
}

@Composable
fun TextInputDialog(
    title: String, label: String, initial: String = "", confirmText: String = "Kaydet",
    onDismiss: () -> Unit, onConfirm: (String) -> Unit
) {
    var value by rememberSaveable { mutableStateOf(initial) }
    var isErr by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = value, onValueChange = { value = it; isErr = false },
                label = { Text(label) }, singleLine = true, isError = isErr,
                supportingText = { if (isErr) Text("Ad boş olamaz.") },
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(onClick = { if (value.isBlank()) isErr = true else onConfirm(value.trim()) }) { Text(confirmText) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Vazgeç") } }
    )
}

@Composable
fun ConfirmDialog(
    title: String, message: String, confirmText: String,
    onDismiss: () -> Unit, onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(confirmText) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Vazgeç") } }
    )
}

@Composable
fun EmptyState(text: String) {
    Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 16.sp,
            lineHeight = 24.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
    }
}

@Composable
fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 12.dp, bottom = 2.dp))
}

@Composable
fun AttemptCard(a: AttemptSummary, showTest: Boolean, onClick: () -> Unit) {
    Card(Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                if (showTest) {
                    Text(a.testName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(a.categoryName, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(formatDate(a.finishedAt ?: a.startedAt))
                Text("Doğru ${a.correct} · Yanlış ${a.wrong} · Boş ${a.blank}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(pct(a.percent), style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        }
    }
}

/**
 * Soru + şıklar. correct == null  -> çözme modu (seçili şık vurgulanır).
 * correct != null -> inceleme modu (doğru yeşil, yanlış seçim kırmızı).
 * E şıkkı boşsa gösterilmez.
 */
@Composable
fun QuestionContent(
    text: String, options: List<String>, selected: String?, correct: String?,
    onSelect: ((String) -> Unit)?
) {
    val cs = MaterialTheme.colorScheme
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(text, fontSize = 19.sp, lineHeight = 28.sp, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(4.dp))
        options.forEachIndexed { idx, opt ->
            if (opt.isNotBlank()) {
                val letter = LETTERS[idx]
                val reveal = correct != null
                val isSel = letter == selected
                val isCorrect = reveal && letter == correct
                val isWrongSel = reveal && isSel && !isCorrect
                val container = when {
                    isCorrect -> GreenBg
                    isWrongSel -> RedBg
                    isSel -> cs.primaryContainer
                    else -> cs.surfaceVariant.copy(alpha = 0.4f)
                }
                val border = when {
                    isCorrect -> GreenFg
                    isWrongSel -> RedFg
                    isSel -> cs.primary
                    else -> Color.Transparent
                }
                Card(
                    colors = CardDefaults.cardColors(containerColor = container),
                    border = BorderStroke(2.dp, border),
                    modifier = Modifier.fillMaxWidth()
                        .then(if (onSelect != null) Modifier.clickable { onSelect(letter) } else Modifier)
                ) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.Top) {
                        Text("$letter)", fontWeight = FontWeight.Bold, fontSize = 17.sp,
                            modifier = Modifier.width(30.dp))
                        Column(Modifier.weight(1f)) {
                            Text(opt, fontSize = 17.sp, lineHeight = 24.sp)
                            if (isCorrect) Text("Doğru cevap", color = GreenFg, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            if (isWrongSel) Text("Senin cevabın", color = RedFg, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ExplanationCard(explanation: String) {
    if (explanation.isNotBlank()) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(14.dp)) {
                Text("Açıklama / Çözüm Notu", fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text(explanation, fontSize = 16.sp, lineHeight = 23.sp)
            }
        }
    }
}
