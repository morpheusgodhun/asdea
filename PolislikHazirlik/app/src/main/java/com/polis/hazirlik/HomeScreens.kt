package com.polis.hazirlik

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import kotlinx.coroutines.launch

private val ListPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 96.dp)

@Composable
fun HomeScreen(nav: NavController) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val titles = listOf("Kategoriler", "Tüm Testler", "Çözüm Geçmişi", "Favori Sorular")
    val labels = listOf("Kategoriler", "Testler", "Geçmiş", "Favoriler")
    val icons = listOf(Icons.Default.Folder, Icons.Default.Checklist, Icons.Default.History, Icons.Default.Star)
    Scaffold(
        topBar = { AppBar(titles[tab]) },
        bottomBar = {
            NavigationBar {
                titles.indices.forEach { i ->
                    NavigationBarItem(
                        selected = tab == i, onClick = { tab = i },
                        icon = { Icon(icons[i], contentDescription = labels[i]) },
                        label = { Text(labels[i], maxLines = 1) }
                    )
                }
            }
        }
    ) { pad ->
        Box(Modifier.padding(pad).fillMaxSize()) {
            when (tab) {
                0 -> CategoriesTab(nav)
                1 -> TestsTab(nav)
                2 -> HistoryTab(nav)
                else -> FavoritesTab(nav)
            }
        }
    }
}

@Composable
fun CategoriesTab(nav: NavController) {
    val dao = App.dao
    val cats by remember { dao.categories() }.collectAsState(null)
    var showAdd by remember { mutableStateOf(false) }
    var rename by remember { mutableStateOf<CategoryInfo?>(null) }
    var delete by remember { mutableStateOf<CategoryInfo?>(null) }

    Box(Modifier.fillMaxSize()) {
        val list = cats
        if (list != null && list.isEmpty()) {
            EmptyState("Henüz kategori yok.\nSağ alttaki + düğmesiyle ilk kategorini oluştur.")
        }
        LazyColumn(contentPadding = ListPadding, verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(list ?: emptyList(), key = { it.id }) { c ->
                Card(Modifier.fillMaxWidth().clickable { nav.navigate("category/${c.id}") }) {
                    Row(
                        Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp, end = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(c.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("${c.testCount} test · ${c.questionCount} soru",
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        IconButton(onClick = { rename = c }) { Icon(Icons.Default.Edit, "Adı değiştir") }
                        IconButton(onClick = { delete = c }) { Icon(Icons.Default.Delete, "Sil") }
                    }
                }
            }
        }
        FloatingActionButton(
            onClick = { showAdd = true },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
        ) { Icon(Icons.Default.Add, "Kategori ekle") }
    }

    if (showAdd) TextInputDialog("Yeni Kategori", "Kategori adı", onDismiss = { showAdd = false }) { name ->
        showAdd = false
        App.scope.launch { dao.insertCategory(Category(name = name)) }
    }
    rename?.let { c ->
        TextInputDialog("Kategori Adını Değiştir", "Kategori adı", initial = c.name,
            onDismiss = { rename = null }) { name ->
            rename = null
            App.scope.launch { dao.renameCategory(c.id, name) }
        }
    }
    delete?.let { c ->
        ConfirmDialog(
            title = "Kategori silinsin mi?",
            message = "“${c.name}” kategorisi silinecek.\n\n" +
                "• Kategoriye bağlı ${c.testCount} test ve ${c.questionCount} soru kalıcı olarak silinir.\n" +
                "• Bu testlerin yarım kalmış çözümleri de silinir.\n" +
                "• Tamamlanmış çözüm geçmişi kayıtları ise (sorular ve cevaplarla birlikte) olduğu gibi korunur.\n\n" +
                "Bu işlem geri alınamaz.",
            confirmText = "Sil",
            onDismiss = { delete = null }
        ) {
            delete = null
            App.scope.launch { dao.deleteCategoryFully(c.id) }
        }
    }
}

@Composable
fun TestsTab(nav: NavController) {
    val tests by remember { App.dao.allTests() }.collectAsState(null)
    var showAdd by remember { mutableStateOf(false) }
    Box(Modifier.fillMaxSize()) {
        TestListContent(tests, nav, showCategory = true,
            emptyText = "Henüz test yok.\nSağ alttaki + düğmesiyle test oluştur.")
        FloatingActionButton(
            onClick = { showAdd = true },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
        ) { Icon(Icons.Default.Add, "Test ekle") }
    }
    if (showAdd) AddTestDialog(null) { showAdd = false }
}

@Composable
fun AddTestDialog(fixedCategoryId: Long?, onDismiss: () -> Unit) {
    val cats by remember { App.dao.categories() }.collectAsState(emptyList())
    var name by rememberSaveable { mutableStateOf("") }
    var catId by rememberSaveable { mutableLongStateOf(fixedCategoryId ?: 0L) }
    var err by remember { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Yeni Test") },
        text = {
            Column {
                OutlinedTextField(
                    value = name, onValueChange = { name = it; err = null },
                    label = { Text("Test adı") }, singleLine = true, modifier = Modifier.fillMaxWidth()
                )
                if (fixedCategoryId == null) {
                    Spacer(Modifier.height(12.dp))
                    Text("Kategori", fontWeight = FontWeight.Bold)
                    if (cats.isEmpty()) Text("Önce Kategoriler sekmesinden bir kategori oluşturmalısın.")
                    Column(Modifier.heightIn(max = 220.dp).verticalScroll(rememberScrollState())) {
                        cats.forEach { c ->
                            Row(
                                Modifier.fillMaxWidth().clickable { catId = c.id },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(selected = catId == c.id, onClick = { catId = c.id })
                                Text(c.name)
                            }
                        }
                    }
                }
                err?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                when {
                    name.isBlank() -> err = "Test adı boş olamaz."
                    catId == 0L -> err = "Bir kategori seçmelisin."
                    else -> {
                        val n = name.trim(); val c = catId
                        App.scope.launch {
                            App.dao.insertTest(QuizTest(categoryId = c, name = n, createdAt = System.currentTimeMillis()))
                        }
                        onDismiss()
                    }
                }
            }) { Text("Oluştur") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Vazgeç") } }
    )
}

@Composable
fun TestListContent(tests: List<TestInfo>?, nav: NavController, showCategory: Boolean, emptyText: String) {
    var rename by remember { mutableStateOf<TestInfo?>(null) }
    var delete by remember { mutableStateOf<TestInfo?>(null) }
    if (tests != null && tests.isEmpty()) EmptyState(emptyText)
    LazyColumn(contentPadding = ListPadding, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items(tests ?: emptyList(), key = { it.id }) { t ->
            Card(Modifier.fillMaxWidth().clickable { nav.navigate("test/${t.id}") }) {
                Row(
                    Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp, end = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(t.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold,
                            maxLines = 2, overflow = TextOverflow.Ellipsis)
                        if (showCategory) Text(t.categoryName, color = MaterialTheme.colorScheme.primary)
                        Text("${t.questionCount} soru · ${formatDate(t.createdAt)}",
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = { rename = t }) { Icon(Icons.Default.Edit, "Adı değiştir") }
                    IconButton(onClick = { delete = t }) { Icon(Icons.Default.Delete, "Sil") }
                }
            }
        }
    }
    rename?.let { t ->
        TextInputDialog("Test Adını Değiştir", "Test adı", initial = t.name, onDismiss = { rename = null }) { n ->
            rename = null
            App.scope.launch { App.dao.renameTest(t.id, n) }
        }
    }
    delete?.let { t ->
        ConfirmDialog(
            title = "Test silinsin mi?",
            message = "“${t.name}” testi silinecek.\n\n" +
                "• Testteki ${t.questionCount} soru kalıcı olarak silinir.\n" +
                "• Yarım kalmış çözümü de silinir.\n" +
                "• Tamamlanmış çözüm geçmişi kayıtları ise olduğu gibi korunur.\n\n" +
                "Bu işlem geri alınamaz.",
            confirmText = "Sil", onDismiss = { delete = null }
        ) {
            delete = null
            App.scope.launch { App.dao.deleteTestFully(t.id) }
        }
    }
}

@Composable
fun HistoryTab(nav: NavController) {
    val list by remember { App.dao.finishedSummaries() }.collectAsState(null)
    val l = list
    if (l != null && l.isEmpty()) EmptyState("Henüz tamamlanmış bir çözümün yok.")
    LazyColumn(contentPadding = ListPadding, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items(l ?: emptyList(), key = { it.id }) { a ->
            AttemptCard(a, showTest = true) { nav.navigate("result/${a.id}") }
        }
    }
}

@Composable
fun FavoritesTab(nav: NavController) {
    val list by remember { App.dao.favorites() }.collectAsState(null)
    val l = list
    if (l != null && l.isEmpty()) {
        EmptyState("Favori sorun yok.\nTest çözerken soru ekranındaki yıldıza dokunarak ekleyebilirsin.")
    }
    LazyColumn(contentPadding = ListPadding, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items(l ?: emptyList(), key = { it.id }) { f ->
            Card(Modifier.fillMaxWidth().clickable { nav.navigate("favorite/${f.id}") }) {
                Row(
                    Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp, end = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(f.text, maxLines = 3, overflow = TextOverflow.Ellipsis)
                        Text("${f.categoryName} › ${f.testName}", color = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = { App.scope.launch { App.dao.setFavorite(f.id, false) } }) {
                        Icon(Icons.Default.Star, "Favorilerden çıkar", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
}

@Composable
fun FavoriteDetailScreen(nav: NavController, questionId: Long) {
    val dao = App.dao
    val q by remember(questionId) { dao.questionFlow(questionId) }.collectAsState(null)
    val favs by remember { dao.favorites() }.collectAsState(emptyList())
    val info = favs.firstOrNull { it.id == questionId }
    Scaffold(topBar = { AppBar("Favori Soru", onBack = { nav.popBackStack() }) }) { pad ->
        val question = q
        if (question == null) {
            Box(Modifier.padding(pad)) { EmptyState("Bu soru artık mevcut değil.") }
            return@Scaffold
        }
        Column(Modifier.padding(pad).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (info != null) Text("${info.categoryName} › ${info.testName}", color = MaterialTheme.colorScheme.primary)
            QuestionContent(question.text, question.options(), null, question.correct, null)
            ExplanationCard(question.explanation)
            OutlinedButton(
                onClick = {
                    App.scope.launch { dao.setFavorite(questionId, false) }
                    nav.popBackStack()
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Favorilerden Çıkar") }
        }
    }
}

@Composable
fun CategoryScreen(nav: NavController, categoryId: Long) {
    val dao = App.dao
    val cat by remember(categoryId) { dao.category(categoryId) }.collectAsState(null)
    val tests by remember(categoryId) { dao.testsIn(categoryId) }.collectAsState(null)
    var showAdd by remember { mutableStateOf(false) }
    Scaffold(
        topBar = { AppBar(cat?.name ?: "Kategori", onBack = { nav.popBackStack() }) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAdd = true },
                icon = { Icon(Icons.Default.Add, null) },
                text = { Text("Test Ekle") }
            )
        }
    ) { pad ->
        Box(Modifier.padding(pad).fillMaxSize()) {
            TestListContent(tests, nav, showCategory = false,
                emptyText = "Bu kategoride henüz test yok.\n“Test Ekle” ile oluştur.")
        }
    }
    if (showAdd) AddTestDialog(categoryId) { showAdd = false }
}
