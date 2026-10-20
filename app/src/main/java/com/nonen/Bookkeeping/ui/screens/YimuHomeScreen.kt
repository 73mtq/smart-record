package com.nonen.Bookkeeping.ui.screens

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.nonen.Bookkeeping.export.BackupExporter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.viewinterop.AndroidView
import android.content.res.ColorStateList
import com.google.android.material.floatingactionbutton.FloatingActionButton as NativeFloatingActionButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.graphics.Typeface
import com.nonen.Bookkeeping.ui.components.HomeAccountBookPopup
import com.nonen.Bookkeeping.ui.components.YimuNativeHomeList
import com.nonen.Bookkeeping.ui.components.bindNativeTemplateDrawer
import com.nonen.Bookkeeping.R
import com.nonen.Bookkeeping.data.db.BookEntity
import com.nonen.Bookkeeping.data.db.TransactionEntity
import com.nonen.Bookkeeping.data.db.CategoryEntity
import com.nonen.Bookkeeping.data.repo.CategoryRepository
import com.nonen.Bookkeeping.data.repo.TemplateRepository
import com.nonen.Bookkeeping.data.prefs.SettingsStore
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun YimuHomeScreen(
    vm: HomeViewModel,
    books: List<BookEntity>,
    selectedBookId: Long,
    templateRepo: TemplateRepository,
    categoryRepo: CategoryRepository,
    settings: SettingsStore,
    onSelectBook: (BookEntity) -> Unit,
    onCreateBook: (String) -> Unit,
    onAdd: () -> Unit,
    onSearch: () -> Unit,
    onEdit: (Long) -> Unit,
    onTemplate: () -> Unit,
    onBudget: () -> Unit,
    onApplyTemplate: (Long) -> Unit,
) {
    val transactions by vm.transactions.collectAsState()
    val today by vm.today.collectAsState()
    val month by vm.month.collectAsState()
    val templates by remember(selectedBookId) { templateRepo.observeAll(selectedBookId) }.collectAsState(initial = emptyList())
    var categories by remember { mutableStateOf<List<CategoryEntity>>(emptyList()) }
    LaunchedEffect(Unit) { categories = categoryRepo.getAll() }
    val hideAmount by settings.hideAmount.collectAsState(initial = false)
    val noteFirst by settings.noteFirst.collectAsState(initial = false)
    val simpleCategory by settings.simpleCategory.collectAsState(initial = false)
    val showTime by settings.showTransactionTime.collectAsState(initial = false)
    val showAssetIcon by settings.showAssetIcon.collectAsState(initial = false)
    val context = LocalContext.current
    var listScrolled by remember { mutableStateOf(false) }
    var drawerOpen by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    var bookPopup by remember { mutableStateOf(false) }
    var nativeBookPopup by remember { mutableStateOf(false) }
    var bookAnchor by remember { mutableStateOf<View?>(null) }
    var createBook by remember { mutableStateOf(false) }
    var more by remember { mutableStateOf(false) }
    var monthPicker by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var confirmDelete by remember { mutableStateOf(false) }
    var showSelectionStats by remember { mutableStateOf(false) }
    var pendingExport by remember { mutableStateOf<List<TransactionEntity>>(emptyList()) }
    val export = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(
        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
    )) { uri ->
        val snapshot = pendingExport
        pendingExport = emptyList()
        if (uri != null) scope.launch {
            withContext(Dispatchers.IO) {
                val bytes = BackupExporter.buildXlsx(snapshot, bookNameMap = books.associate { it.bookId to it.bookName })
                requireNotNull(context.contentResolver.openOutputStream(uri)).use { it.write(bytes) }
            }
        }
    }
    LaunchedEffect(selectedBookId, month) { selected = emptySet() }
    Box(Modifier.fillMaxSize().semantics { testTagsAsResourceId = true }) {
        Column(Modifier.fillMaxSize()) {
            Row(Modifier.fillMaxWidth().testTag("tool_bar").statusBarsPadding().height(45.dp), verticalAlignment = Alignment.CenterVertically) {
                val toolbarColor = MaterialTheme.colorScheme.onSurface.toArgb()
                AndroidView(modifier = Modifier.height(45.dp), factory = { viewContext ->
                    val scale = viewContext.resources.displayMetrics.density
                    fun dp(value: Int) = (value * scale + 0.5f).toInt()
                    LinearLayout(viewContext).apply {
                        id = R.id.account_book_layout
                        gravity = Gravity.CENTER_VERTICAL
                        setPaddingRelative(dp(15), 0, 0, 0)
                        addView(ImageView(viewContext).apply {
                            id = R.id.account_book_icon
                            setImageResource(R.mipmap.ic_main_account_book)
                            scaleType = ImageView.ScaleType.CENTER_INSIDE
                        }, LinearLayout.LayoutParams(dp(22), dp(45)))
                        addView(TextView(viewContext).apply {
                            id = R.id.account_book_name
                            textSize = 16f
                            gravity = Gravity.CENTER_VERTICAL
                            setTypeface(typeface, Typeface.BOLD)
                        }, LinearLayout.LayoutParams(-2, dp(45)).apply { marginStart = dp(8) })
                        setOnClickListener {
                            bookAnchor = this
                            nativeBookPopup = true
                        }
                    }
                }, update = { view ->
                    view.findViewById<TextView>(R.id.account_book_name).apply {
                        text = books.firstOrNull { it.bookId == selectedBookId }?.bookName.orEmpty()
                        setTextColor(toolbarColor)
                    }
                    view.findViewById<ImageView>(R.id.account_book_icon).imageTintList =
                        ColorStateList.valueOf(view.context.getColor(R.color.textColorPrimary))
                })
                Spacer(Modifier.weight(1f))
                if (listScrolled) HomeImageButton(R.mipmap.ic_menu_add_bill, "btn_add_bill", R.string.action_add_record, 9, onAdd)
                HomeImageButton(R.mipmap.ic_main_search, "btn_search", R.string.action_search, 11, onSearch)
                Box {
                    HomeImageButton(R.mipmap.ic_menu, "btn_more", R.string.yimu_more, 10) { more = true }
                    DropdownMenu(more, { more = false }) {
                        DropdownMenuItem(text = { Text(stringResource(R.string.yimu_templates)) }, onClick = { more = false; onTemplate() })
                        DropdownMenuItem(text = { Text(stringResource(R.string.yimu_month_budget)) }, onClick = { more = false; onBudget() })
                    }
                }
                Spacer(Modifier.width(5.dp))
            }
            Box(Modifier.weight(1f).fillMaxWidth()) {
                Column(Modifier.fillMaxSize()) {
                    PullToRefreshBox(isRefreshing = false, onRefresh = vm::refreshToday, modifier = Modifier.weight(1f)) {
                        YimuNativeHomeList(transactions, today, hideAmount, noteFirst, simpleCategory, showTime,
                            showAssetIcon, selected, onEdit,
                            onToggleSelection = { id -> selected = if (id in selected) selected - id else selected + id },
                            onLongSelect = { id -> selected = selected + id },
                            onAdd = onAdd, onBudget = onBudget, onChooseMonth = { monthPicker = true },
                            onOpenTemplates = { drawerOpen = true },
                            onScrolled = { listScrolled = it }, modifier = Modifier.fillMaxSize())
                    }
                    if (selected.isNotEmpty()) Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        TextButton(onClick = { selected = emptySet() }) { Text("取消") }
                        TextButton(onClick = { showSelectionStats = true }) { Text("统计") }
                        TextButton(onClick = {
                            pendingExport = transactions.filter { it.id in selected }
                            export.launch("账单_$month.xlsx")
                        }) { Text("导出") }
                        if (selected.size == 1) TextButton(onClick = { onEdit(selected.single()); selected = emptySet() }) { Text("编辑") }
                        TextButton(onClick = { confirmDelete = true }) { Text("删除 (${selected.size})") }
                    }
                }
                if (listScrolled) {
                    Box(
                        Modifier.fillMaxWidth().height(2.dp).align(Alignment.TopCenter)
                            .background(Brush.verticalGradient(listOf(Color(0x1B000000), Color(0x0C000000)))),
                    )
                }
                if (selected.isEmpty() && !listScrolled) {
                    val primary = MaterialTheme.colorScheme.primary.toArgb()
                    val foreground = MaterialTheme.colorScheme.onPrimary.toArgb()
                    AndroidView(modifier = Modifier.align(Alignment.BottomEnd).padding(20.dp).size(56.dp),
                        factory = { viewContext -> NativeFloatingActionButton(viewContext).apply {
                            id = R.id.add_btn
                            setImageResource(R.mipmap.ic_add_ai_bill)
                            setMaxImageSize((32 * resources.displayMetrics.density).toInt())
                            contentDescription = viewContext.getString(R.string.action_add_record)
                            setOnClickListener { onAdd() }
                        } }, update = {
                            it.backgroundTintList = ColorStateList.valueOf(primary)
                            it.imageTintList = ColorStateList.valueOf(foreground)
                        })
                }
                if (drawerOpen) {
                    Box(Modifier.fillMaxSize().padding(start = 280.dp).clickable { drawerOpen = false })
                    BackHandler { drawerOpen = false }
                    AndroidView(
                        modifier = Modifier.width(280.dp).fillMaxHeight(),
                        factory = { viewContext ->
                            LayoutInflater.from(viewContext).inflate(R.layout.yimu_home_drawer, null, false)
                        },
                        update = { view ->
                            bindNativeTemplateDrawer(view, templates, categories) { template ->
                                drawerOpen = false
                                scope.launch {
                                    if (template.moduleLock && template.cost > 0.0) {
                                        checkNotNull(templateRepo.applyTemplate(template.moduleBillId))
                                    } else onApplyTemplate(template.moduleBillId)
                                }
                            }
                        },
                    )
                    Box(
                        Modifier.offset(x = 280.dp).width(6.dp).fillMaxHeight()
                            .background(Brush.horizontalGradient(listOf(Color(0x14000000), Color.Transparent))),
                    )
                }
            }
        }
    }
    if (nativeBookPopup) {
        val anchor = bookAnchor
        if (anchor != null) {
            DisposableEffect(anchor) {
                val popup = HomeAccountBookPopup(
                    context = context,
                    anchor = anchor,
                    books = books,
                    onSelect = { onSelectBook(it); nativeBookPopup = false },
                    onCreate = { nativeBookPopup = false; createBook = true },
                    onManage = { nativeBookPopup = false; bookPopup = true },
                    onDismissed = { nativeBookPopup = false },
                )
                popup.show()
                onDispose { popup.dismiss() }
            }
        }
    }
    if (bookPopup || createBook) BookChoiceDialog(
        books, selectedBookId, onSelectBook, onCreateBook, startInCreate = createBook,
    ) {
        bookPopup = false
        createBook = false
    }
    if (showSelectionStats) {
        val selection = transactions.filter { it.id in selected }
        val income = selection.filter { it.amount > 0 }.sumOf { it.amount }
        val expense = -selection.filter { it.amount < 0 }.sumOf { it.amount }
        AlertDialog(onDismissRequest = { showSelectionStats = false }, title = { Text("选中账单统计") },
            text = { Text("共 ${selection.size} 条\n收入 ${homeMoney(income)}\n支出 ${homeMoney(expense)}\n结余 ${homeMoney(income - expense)}") },
            confirmButton = { TextButton(onClick = { showSelectionStats = false }) { Text("确定") } })
    }
    if (monthPicker) MonthPickerDialog(month, today, { vm.selectMonth(it); monthPicker = false }, { monthPicker = false })
    if (confirmDelete) AlertDialog(onDismissRequest = { confirmDelete = false }, text = { Text("删除选中的 ${selected.size} 条账单？") },
        confirmButton = { TextButton(onClick = { vm.deleteSelected(selected); selected = emptySet(); confirmDelete = false }) { Text("删除") } },
        dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("取消") } })
}

@Composable
fun BookChoiceDialog(books: List<BookEntity>, selected: Long, onSelect: (BookEntity) -> Unit, onCreate: (String) -> Unit, startInCreate: Boolean = false, onDismiss: () -> Unit) {
    var create by remember { mutableStateOf(startInCreate) }
    var name by remember { mutableStateOf("") }
    AlertDialog(onDismissRequest = onDismiss, title = { Text(stringResource(if (create) R.string.yimu_new_book else R.string.yimu_book)) },
        text = {
            if (create) OutlinedTextField(name, { name = it }, label = { Text(stringResource(R.string.yimu_book_name)) }, singleLine = true)
            else Column {
                TextButton(onClick = { create = true }, modifier = Modifier.testTag("add_account_book")) { Text(stringResource(R.string.yimu_new_book)) }
                books.forEach { book ->
                    TextButton(onClick = { onSelect(book); onDismiss() }) {
                        Text(book.bookName, fontWeight = if (book.bookId == selected) FontWeight.Bold else FontWeight.Normal)
                    }
                }
            }
        },
        confirmButton = { if (create) TextButton(enabled = name.isNotBlank(), onClick = { onCreate(name); onDismiss() }) { Text("保存") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } })
}

@Composable
private fun HomeImageButton(icon: Int, tag: String, description: Int, padding: Int, onClick: () -> Unit) {
    AndroidView(modifier = Modifier.size(40.dp), factory = { context -> ImageView(context).apply {
        id = when (tag) { "btn_search" -> R.id.btn_search; "btn_more" -> R.id.btn_more; else -> R.id.btn_add_bill }
        setImageResource(icon)
        val inset = (padding * resources.displayMetrics.density + 0.5f).toInt()
        setPadding(inset, inset, inset, inset)
        contentDescription = context.getString(description)
        setOnClickListener { onClick() }
    } }, update = {
        it.imageTintList = ColorStateList.valueOf(it.context.getColor(R.color.textColorPrimary))
    })
}

private fun homeMoney(amount: Double) = String.format(Locale.US, "%,.2f", amount)
