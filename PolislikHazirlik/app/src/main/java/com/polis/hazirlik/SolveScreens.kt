package com.polis.hazirlik

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import androidx.navigation.NavController
import kotlinx.coroutines.launch

@Composable
fun SolveScreen(nav: NavController, attemptId: Long) {
    val dao = App.dao
    val scope = rememberCoroutineScope()
    val attempt by remember(attemptId) { dao.attemptFlow(attemptId) }.collectAsState(null)
    val qs by remember(attemptId) { dao.attemptQuestions(attemptId) }.collectAsState(emptyList())
    val favIds by remember { dao.favoriteIds() }.collectAsState(emptyList())
    var index by rememberSaveable { mutableIntStateOf(-1) }
    var confirmFinish by remember { mutableStateOf(false) }

    LaunchedEffect(attemptId) {
        if (index < 0) index = dao.getAttempt(attemptId)?.currentIndex ?: 0
    }

    if (qs.isEmpty() || index < 0) {
        Scaffold(topBar = { AppBar("Test", onBack = { nav.popBackStack() }) }) { pad ->
            Box(Modifier.padding(pad).fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        return
    }

    val i = index.coerceIn(0, qs.lastIndex)
    val q = qs[i]
    val oid = q.originalQuestionId
    val isFav = oid != null && favIds.contains(oid)
    val scroll = rememberScrollState()
    LaunchedEffect(i) { scroll.scrollTo(0) }

    fun go(n: Int) {
        val t = n.coerceIn(0, qs.lastIndex)
        index = t
        App.scope.launch { dao.setIndex(attemptId, t) } // kalınan soru da kaydedilir
    }

    fun finish() {
        scope.launch {
            dao.finishAttempt(attemptId, System.currentTimeMillis())
            nav.navigate("result/$attemptId") { popUpTo("solve/{id}") { inclusive = true } }
        }
    }

    val blanks = qs.count { it.selected == null }

    Scaffold(
        topBar = {
            AppBar(attempt?.testName ?: "Test", onBack = { nav.popBackStack() }) {
                if (oid != null) IconButton(onClick = { App.scope.launch { dao.setFavorite(oid, !isFav) } }) {
                    Icon(if (isFav) Icons.Default.Star else Icons.Default.StarBorder,
                        contentDescription = if (isFav) "Favorilerden çıkar" else "Favorilere ekle")
                }
            }
        },
        bottomBar = {
            Surface(tonalElevation = 3.dp) {
                Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { go(i - 1) }, enabled = i > 0, modifier = Modifier.weight(1f)) {
                            Text("Önceki")
                        }
                        Button(onClick = { go(i + 1) }, enabled = i < qs.lastIndex, modifier = Modifier.weight(1f)) {
                            Text("Sonraki")
                        }
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        TextButton(onClick = { nav.popBackStack() }) { Text("Testten Çık") }
                        TextButton(onClick = { confirmFinish = true }) { Text("Testi Bitir") }
                    }
                }
            }
        }
    ) { pad ->
        Column(Modifier.padding(pad).fillMaxSize()) {
            Text("Soru ${i + 1} / ${qs.size}", fontWeight = FontWeight.Bold, fontSize = 16.sp,
                modifier = Modifier.padding(start = 16.dp, top = 10.dp))
            LazyRow(
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                itemsIndexed(qs) { n, item ->
                    FilterChip(
                        selected = n == i, onClick = { go(n) }, label = { Text("${n + 1}") },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = if (item.selected != null) MaterialTheme.colorScheme.secondaryContainer
                            else Color.Transparent
                        )
                    )
                }
            }
            Column(Modifier.weight(1f).verticalScroll(scroll).padding(16.dp)) {
                QuestionContent(
                    text = q.text, options = q.options(), selected = q.selected, correct = null,
                    onSelect = { letter -> App.scope.launch { dao.setSelected(q.id, letter) } } // anında DB'ye yazılır
                )
            }
        }
    }

    if (confirmFinish) {
        if (blanks > 0) {
            AlertDialog(
                onDismissRequest = { confirmFinish = false },
                title = { Text("Boş sorular var") },
                text = { Text("$blanks soru boş bırakıldı. Boş sorulara dönebilir ya da testi bu hâliyle bitirebilirsin.") },
                confirmButton = {
                    TextButton(onClick = { confirmFinish = false; finish() }) { Text("Yine de Bitir") }
                },
                dismissButton = {
                    TextButton(onClick = {
                        confirmFinish = false
                        go(qs.indexOfFirst { it.selected == null }.coerceAtLeast(0))
                    }) { Text("Boş Sorulara Dön") }
                }
            )
        } else {
            ConfirmDialog("Testi bitir", "Tüm sorular cevaplandı. Testi bitirmek istiyor musun?", "Bitir",
                onDismiss = { confirmFinish = false }) { confirmFinish = false; finish() }
        }
    }
}

@Composable
fun ResultScreen(nav: NavController, attemptId: Long) {
    val dao = App.dao
    val s by remember(attemptId) { dao.attemptSummary(attemptId) }.collectAsState(null)
    val qs by remember(attemptId) { dao.attemptQuestions(attemptId) }.collectAsState(emptyList())
    Scaffold(topBar = { AppBar("Test Sonucu", onBack = { nav.popBackStack() }) }) { pad ->
        val sum = s ?: return@Scaffold
        LazyColumn(
            Modifier.padding(pad).fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(sum.testName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text("${sum.categoryName} · ${formatDate(sum.finishedAt ?: sum.startedAt)}",
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(8.dp))
                        Text(pct(sum.percent), fontSize = 40.sp, fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary)
                        Text("Başarı yüzdesi (doğru / toplam soru)", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(8.dp))
                        StatRow("Toplam soru", sum.total)
                        StatRow("Cevaplanan", sum.answered)
                        StatRow("Doğru", sum.correct)
                        StatRow("Yanlış", sum.wrong)
                        StatRow("Boş", sum.blank)
                    }
                }
            }
            item {
                Button(onClick = { nav.navigate("review/$attemptId/0") }, modifier = Modifier.fillMaxWidth()) {
                    Text("Soruları İncele")
                }
            }
            itemsIndexed(qs, key = { _, q -> q.id }) { n, q ->
                val (label, color) = when {
                    q.selected == null -> "Boş" to Color(0xFF757575)
                    q.selected == q.correct -> "Doğru" to Color(0xFF2E7D32)
                    else -> "Yanlış" to Color(0xFFC62828)
                }
                Card(Modifier.fillMaxWidth().clickable { nav.navigate("review/$attemptId/$n") }) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("${n + 1}. ${q.text}", maxLines = 2, overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f))
                        Spacer(Modifier.width(8.dp))
                        Text(label, color = color, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun StatRow(label: String, value: Int) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, fontSize = 16.sp)
        Text("$value", fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun ReviewScreen(nav: NavController, attemptId: Long, startIndex: Int) {
    val dao = App.dao
    val attempt by remember(attemptId) { dao.attemptFlow(attemptId) }.collectAsState(null)
    val qs by remember(attemptId) { dao.attemptQuestions(attemptId) }.collectAsState(emptyList())
    var index by rememberSaveable { mutableIntStateOf(startIndex) }
    val scroll = rememberScrollState()
    LaunchedEffect(index) { scroll.scrollTo(0) }

    Scaffold(
        topBar = { AppBar(attempt?.testName ?: "İnceleme", onBack = { nav.popBackStack() }) },
        bottomBar = {
            if (qs.isNotEmpty()) Surface(tonalElevation = 3.dp) {
                Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { index = (index - 1).coerceAtLeast(0) }, enabled = index > 0,
                        modifier = Modifier.weight(1f)) { Text("Önceki") }
                    Button(onClick = { index = (index + 1).coerceAtMost(qs.lastIndex) }, enabled = index < qs.lastIndex,
                        modifier = Modifier.weight(1f)) { Text("Sonraki") }
                }
            }
        }
    ) { pad ->
        if (qs.isEmpty()) return@Scaffold
        val i = index.coerceIn(0, qs.lastIndex)
        val q = qs[i]
        val (status, color) = when {
            q.selected == null -> "Bu soruyu boş bıraktın." to Color(0xFF757575)
            q.selected == q.correct -> "Doğru cevapladın." to Color(0xFF2E7D32)
            else -> "Yanlış cevapladın." to Color(0xFFC62828)
        }
        Column(Modifier.padding(pad).fillMaxSize().verticalScroll(scroll).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Soru ${i + 1} / ${qs.size}", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text(status, color = color, fontWeight = FontWeight.Bold)
            QuestionContent(q.text, q.options(), q.selected, q.correct, null)
            Text("Senin cevabın: ${q.selected ?: "—"}   ·   Doğru cevap: ${q.correct}", fontWeight = FontWeight.Medium)
            ExplanationCard(q.explanation)
        }
    }
}
