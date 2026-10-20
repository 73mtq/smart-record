package com.nonen.Bookkeeping.ui.screens

import android.content.res.ColorStateList
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.unit.dp
import android.view.View
import androidx.core.view.ViewCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlin.math.roundToInt
import com.google.android.material.navigation.NavigationBarView
import com.nonen.Bookkeeping.AppContainer
import com.nonen.Bookkeeping.R
import com.nonen.Bookkeeping.ui.vmFactory
import kotlinx.coroutines.launch
import java.time.LocalDate

@Composable
fun MainScreen(
    container: AppContainer,
    onAdd: () -> Unit,
    onAddAtDate: (LocalDate) -> Unit,
    onStatisticsBills: (StatisticsBillFilter) -> Unit,
    onEdit: (Long) -> Unit,
    onSearch: () -> Unit,
    onRules: () -> Unit,
    onCategoryManage: () -> Unit = {},
    onAssetManage: () -> Unit = {},
    onBudget: () -> Unit = {},
    onPeriodic: () -> Unit = {},
    onTemplate: () -> Unit = {},
    onApplyTemplate: (Long) -> Unit = {},
    onSavingsGoal: () -> Unit = {},
    onCalendar: () -> Unit = {},
    onSettings: () -> Unit = {},
    onAppearance: () -> Unit = {},
    onImportExport: () -> Unit = {},
    onBackup: () -> Unit = {},
    initialTab: Int = 0,
) {
    val vm: MainViewModel = viewModel(factory = vmFactory { MainViewModel(container) })
    val bookId by vm.selectedBookId.collectAsState()
    val books by vm.books.collectAsState()
    val labels by vm.navigationLabelsEnabled.collectAsState(initial = false)
    val pager = rememberPagerState(initialPage = initialTab.coerceIn(0, 4)) { MainTab.entries.size }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current.applicationContext
    var showBooks by remember { mutableStateOf(false) }
    LaunchedEffect(initialTab) { pager.scrollToPage(initialTab.coerceIn(0, 4)) }
    Column(Modifier.fillMaxSize()) {
        HorizontalPager(
            state = pager,
            modifier = Modifier.weight(1f).fillMaxWidth(),
        ) { page ->
            when (MainTab.entries[page]) {
                MainTab.HOME -> {
                    val home: HomeViewModel = viewModel(factory = vmFactory {
                        HomeViewModel(context, container.transactionRepository)
                    })
                    LaunchedEffect(bookId) { home.selectBook(bookId) }
                    YimuHomeScreen(
                        vm = home, books = books, selectedBookId = bookId,
                        templateRepo = container.templateRepository,
                        categoryRepo = container.categoryRepository,
                        settings = container.settings,
                        onSelectBook = vm::selectBook, onCreateBook = vm::createBook,
                        onAdd = onAdd, onSearch = onSearch, onEdit = onEdit,
                        onTemplate = onTemplate, onBudget = onBudget,
                        onApplyTemplate = onApplyTemplate,
                    )
                }
                MainTab.CALENDAR -> {
                    val calendar: CalendarViewModel = viewModel(factory = vmFactory {
                        CalendarViewModel(container.reportRepository)
                    })
                    LaunchedEffect(bookId) { calendar.selectBook(bookId) }
                    YimuCalendarScreen(calendar, onEdit = onEdit, onAdd = onAddAtDate)
                }
                MainTab.ASSET -> {
                    val asset: AssetManageViewModel = viewModel(factory = vmFactory {
                        AssetManageViewModel(container.assetRepository)
                    })
                    AssetManageScreen(asset, onBack = {}, embedded = true)
                }
                MainTab.STATISTICS -> {
                    val stats: StatsViewModel = viewModel(factory = vmFactory {
                        StatsViewModel(container.transactionRepository, container.reportRepository)
                    })
                    LaunchedEffect(bookId) { stats.selectBook(bookId) }
                    YimuStatisticsScreen(container, bookId, onBudget, onStatisticsBills)
                }
                MainTab.MY -> MyScreen(
                    container, onSettings, onCategoryManage, onRules, onPeriodic, onSavingsGoal,
                    onTemplate, onBudget, onBook = { showBooks = true },
                    onAppearance = onAppearance, onImportExport = onImportExport, onBackup = onBackup,
                )
            }
        }
        YimuBottomBar(pager.currentPage, labels) { scope.launch { pager.animateScrollToPage(it) } }
    }
    if (showBooks) BookChoiceDialog(books, bookId, vm::selectBook, vm::createBook) { showBooks = false }
}

@Composable
private fun YimuBottomBar(selected: Int, labels: Boolean, onSelect: (Int) -> Unit) {
    val ids = listOf(R.id.tab_main, R.id.tab_calendar, R.id.tab_asset, R.id.tab_statistics, R.id.tab_my)
    val background = MaterialTheme.colorScheme.surface.toArgb()
    val density = LocalDensity.current
    val navigationBottom = WindowInsets.navigationBars.getBottom(density)
    AndroidView(
        modifier = Modifier.fillMaxWidth().height(56.dp + with(density) { navigationBottom.toDp() }),
        factory = { context ->
            BottomNavigationView(context).apply {
                id = R.id.bottom_nav_view
                inflateMenu(R.menu.menu_navigation_selector)
                importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_YES
                ids.forEachIndexed { index, itemId ->
                    menu.findItem(itemId).contentDescription = listOf("首页", "日历", "资产", "统计", "我的")[index]
                    findViewById<View>(itemId)?.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_YES
                }
                itemIconSize = (26 * resources.displayMetrics.density).roundToInt()
                setItemTextAppearanceActive(R.style.YimuBottomTitleActive)
                setItemTextAppearanceInactive(R.style.YimuBottomTitleInactive)
                isItemHorizontalTranslationEnabled = false
                setItemActiveIndicatorEnabled(false)
                itemIconTintList = null
                itemTextColor = ColorStateList(
                    arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf()),
                    intArrayOf(0xFF48AB93.toInt(), 0xFF999999.toInt()),
                )
                ViewCompat.setOnApplyWindowInsetsListener(this) { _, insets -> insets }
                setOnItemSelectedListener { item -> onSelect(ids.indexOf(item.itemId)); true }
            }
        },
        update = { view ->
            view.setPadding(0, 0, 0, navigationBottom)
            view.setBackgroundColor(background)
            view.labelVisibilityMode = if (labels) NavigationBarView.LABEL_VISIBILITY_LABELED
                else NavigationBarView.LABEL_VISIBILITY_UNLABELED
            if (view.selectedItemId != ids[selected]) {
                view.setOnItemSelectedListener(null)
                view.selectedItemId = ids[selected]
                view.setOnItemSelectedListener { item -> onSelect(ids.indexOf(item.itemId)); true }
            }
        },
    )
}
