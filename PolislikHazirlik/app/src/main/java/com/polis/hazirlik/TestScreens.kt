package com.polis.hazirlik

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import kotlinx.coroutines.launch

@Composable
fun TestScreen(nav: NavController, testId: Long) {
    val dao = App.dao
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val info by remember(testId) { dao.testInfo(testId) }.collectAsState(null)
    val questions by remember(testId) { dao.questions(testId) }.collectAsState(emptyList())
    val unfinished by remember(testId) { dao.unfinishedSummary(testId) }.collectAsState(null)
    val attempts by remember(testId) { dao.finishedSummariesForTest(testId) }.collectAsState(emptyList())
    var rename by remember { mutableStateOf(false) }
    var deleteTest by remember { mutableStateOf(false) }
    var deleteQ by remember { mutableStateOf<Question?>(null) }
    var restart by remember { mutableStateOf(false) }

    fun start() {
        scope.launch {
            val id = dao.startAttempt(testId)
            if (id == 0L) Toast.makeText(ctx, "Bu testte henüz soru yok. Önce soru ekle.", Toast.LENGTH_SHORT).show()
            else nav.navigate("solve/$id")
        }
    }

    Scaffold(
        topBar = {
            AppBar(info?.name ?: "Test", onBack = { nav.popBackStack() }) {
                IconButton(onClick = { rename = true }) { Icon(Icons.Default.Edit, "Test adını değiştir") }
                IconButton(onClick = { deleteTest = true }) { Icon(Icons.Default.Delete, "Testi sil") }
            }
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { nav.navigate("edit/$testId/0") },
                icon = { Icon(Icons.Default.Add, null) },
                text = { Text("Soru Ekle") }
            )
        }
    ) { pad ->
        LazyColumn(
            Modifier.padding(pad).fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Kategori: ${info?.categoryName ?: ""}", fontWeight = FontWeight.Bold)
                        Text("${questions.size} soru")
                        info?.let { Text("Oluşturulma: ${formatDate(it.createdAt)}") }
                    }
                }
            }
            item {
                val u = unfinished
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (u != null) {
                        Text("Yarım kalan çözümün var: ${u.answered}/${u.total} soru cevaplandı, " +
                            "${u.currentIndex + 1}. sorudan devam edebilirsin.")
                        Button(onClick = { nav.navigate("solve/${u.id}") }, modifier = Modifier.fillMaxWidth()) {
                            Text("Kaldığım Yerden Devam Et")
                        }
                        OutlinedButton(onClick = { restart = true }, modifier = Modifier.fillMaxWidth()) {
                            Text("Baştan Başla")
                        }
                    } else {
                        Button(onClick = { start() }, modifier = Modifier.fillMaxWidth()) { Text("Çözmeye Başla") }
                    }
                }
            }
            item { SectionTitle("Sorular (${questions.size})") }
            if (questions.isEmpty()) item {
                Text("Henüz soru yok. “Soru Ekle” ile ilk sorunu gir.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            itemsIndexed(questions, key = { _, q -> q.id }) { i, q ->
                Card(Modifier.fillMaxWidth().clickable { nav.navigate("edit/$testId/${q.id}") }) {
                    Column(Modifier.padding(start = 16.dp, top = 12.dp, end = 8.dp, bottom = 4.dp)) {
                        Text("${i + 1}. ${q.text}", maxLines = 3, overflow = TextOverflow.Ellipsis)
                        Text("Doğru cevap: ${q.correct}", color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            IconButton(enabled = i > 0, onClick = {
                                App.scope.launch { dao.swapPositions(questions[i - 1], q) }
                            }) { Icon(Icons.Default.ArrowUpward, "Yukarı taşı") }
                            IconButton(enabled = i < questions.lastIndex, onClick = {
                                App.scope.launch { dao.swapPositions(q, questions[i + 1]) }
                            }) { Icon(Icons.Default.ArrowDownward, "Aşağı taşı") }
                            IconButton(onClick = { nav.navigate("edit/$testId/${q.id}") }) {
                                Icon(Icons.Default.Edit, "Düzenle")
                            }
                            IconButton(onClick = { deleteQ = q }) { Icon(Icons.Default.Delete, "Sil") }
                        }
                    }
                }
            }
            item { SectionTitle("Önceki çözümler (${attempts.size})") }
            if (attempts.isEmpty()) item {
                Text("Bu testi henüz tamamlamadın.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            items(attempts, key = { "a${it.id}" }) { a ->
                AttemptCard(a, showTest = false) { nav.navigate("result/${a.id}") }
            }
        }
    }

    if (rename) TextInputDialog("Test Adını Değiştir", "Test adı", initial = info?.name ?: "",
        onDismiss = { rename = false }) { n ->
        rename = false
        App.scope.launch { dao.renameTest(testId, n) }
    }
    if (deleteTest) ConfirmDialog(
        title = "Test silinsin mi?",
        message = "Bu test ve içindeki ${questions.size} soru kalıcı olarak silinir. Yarım kalmış çözümü de silinir. " +
            "Tamamlanmış çözüm geçmişi kayıtları olduğu gibi korunur.\n\nBu işlem geri alınamaz.",
        confirmText = "Sil", onDismiss = { deleteTest = false }
    ) {
        deleteTest = false
        App.scope.launch { dao.deleteTestFully(testId) }
        nav.popBackStack()
    }
    deleteQ?.let { q ->
        ConfirmDialog(
            title = "Soru silinsin mi?",
            message = "Bu soru testten silinecek. Daha önce çözdüğün kayıtlardaki kopyası değişmeden korunur.",
            confirmText = "Sil", onDismiss = { deleteQ = null }
        ) {
            deleteQ = null
            App.scope.launch { dao.deleteQuestion(q.id) }
        }
    }
    if (restart) ConfirmDialog(
        title = "Baştan başlansın mı?",
        message = "Yarım kalan çözümün silinecek ve yeni bir çözüm başlayacak. " +
            "Tamamlanmış çözümlerin ve sonuçların etkilenmez.",
        confirmText = "Baştan Başla", onDismiss = { restart = false }
    ) {
        restart = false
        start()
    }
}

@Composable
fun QuestionEditScreen(nav: NavController, testId: Long, qid: Long) {
    val scope = rememberCoroutineScope()
    val ctx = LocalContext.current
    var text by rememberSaveable { mutableStateOf("") }
    var a by rememberSaveable { mutableStateOf("") }
    var b by rememberSaveable { mutableStateOf("") }
    var c by rememberSaveable { mutableStateOf("") }
    var d by rememberSaveable { mutableStateOf("") }
    var e by rememberSaveable { mutableStateOf("") }
    var expl by rememberSaveable { mutableStateOf("") }
    var correct by rememberSaveable { mutableStateOf("") }
    var loaded by rememberSaveable { mutableStateOf(qid == 0L) }
    var dirty by rememberSaveable { mutableStateOf(false) }
    var err by remember { mutableStateOf<String?>(null) }
    var showDiscard by remember { mutableStateOf(false) }

    LaunchedEffect(qid) {
        if (!loaded) {
            App.dao.getQuestion(qid)?.let {
                text = it.text; a = it.optA; b = it.optB; c = it.optC; d = it.optD; e = it.optE
                expl = it.explanation; correct = it.correct
            }
            loaded = true
        }
    }

    fun leave() { if (dirty) showDiscard = true else nav.popBackStack() }
    BackHandler { leave() }

    fun save(addNew: Boolean) {
        val opts = listOf(a, b, c, d, e).map { it.trim() }
        when {
            text.isBlank() -> { err = "Soru metni boş olamaz."; return }
            opts.take(4).any { it.isEmpty() } -> { err = "A, B, C ve D şıkları boş olamaz."; return }
            correct.isEmpty() -> { err = "Doğru cevabı seçmelisin."; return }
            opts[LETTERS.indexOf(correct)].isEmpty() -> { err = "Boş bir şık doğru cevap olarak seçilemez."; return }
        }
        err = null
        scope.launch {
            val dao = App.dao
            if (qid == 0L) {
                dao.insertQuestion(
                    Question(testId = testId, text = text.trim(), optA = opts[0], optB = opts[1], optC = opts[2],
                        optD = opts[3], optE = opts[4], correct = correct, explanation = expl.trim(),
                        position = dao.nextPosition(testId))
                )
            } else {
                val old = dao.getQuestion(qid)
                if (old == null) { err = "Bu soru artık mevcut değil."; return@launch }
                dao.updateQuestion(old.copy(text = text.trim(), optA = opts[0], optB = opts[1], optC = opts[2],
                    optD = opts[3], optE = opts[4], correct = correct, explanation = expl.trim()))
            }
            dirty = false
            if (addNew) {
                text = ""; a = ""; b = ""; c = ""; d = ""; e = ""; expl = ""; correct = ""
                Toast.makeText(ctx, "Soru kaydedildi.", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(ctx, "Kaydedildi.", Toast.LENGTH_SHORT).show()
                nav.popBackStack()
            }
        }
    }

    Scaffold(topBar = {
        AppBar(if (qid == 0L) "Yeni Soru" else "Soruyu Düzenle", onBack = { leave() })
    }) { pad ->
        Column(
            Modifier.padding(pad).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedTextField(text, { text = it; dirty = true }, label = { Text("Soru metni") },
                minLines = 3, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(a, { a = it; dirty = true }, label = { Text("A şıkkı") }, minLines = 1, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(b, { b = it; dirty = true }, label = { Text("B şıkkı") }, minLines = 1, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(c, { c = it; dirty = true }, label = { Text("C şıkkı") }, minLines = 1, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(d, { d = it; dirty = true }, label = { Text("D şıkkı") }, minLines = 1, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(e, { e = it; dirty = true }, label = { Text("E şıkkı (isteğe bağlı)") }, minLines = 1, modifier = Modifier.fillMaxWidth())
            Text("Doğru cevap", fontWeight = FontWeight.Bold)
            val opts = listOf(a, b, c, d, e)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                LETTERS.forEachIndexed { i, l ->
                    FilterChip(
                        selected = correct == l,
                        onClick = { correct = l; dirty = true },
                        enabled = opts[i].isNotBlank(),
                        label = { Text(l) }
                    )
                }
            }
            Text("Boş bırakılan şık doğru cevap olarak seçilemez.", style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedTextField(expl, { expl = it; dirty = true }, label = { Text("Açıklama / çözüm notu (isteğe bağlı)") },
                minLines = 2, modifier = Modifier.fillMaxWidth())
            err?.let { Text(it, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold) }
            Button(onClick = { save(false) }, modifier = Modifier.fillMaxWidth()) { Text("Kaydet") }
            if (qid == 0L) {
                OutlinedButton(onClick = { save(true) }, modifier = Modifier.fillMaxWidth()) {
                    Text("Kaydet ve Yeni Soru Ekle")
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    if (showDiscard) ConfirmDialog(
        title = "Kaydedilmemiş değişiklikler",
        message = "Kaydetmeden çıkarsan girdiğin bilgiler kaybolur.",
        confirmText = "Kaydetmeden Çık", onDismiss = { showDiscard = false }
    ) {
        showDiscard = false
        dirty = false
        nav.popBackStack()
    }
}
