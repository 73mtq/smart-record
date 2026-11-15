package com.nonen.Bookkeeping.ui

import androidx.compose.animation.core.tween
import android.net.Uri
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.nonen.Bookkeeping.BookkeepingApp
import com.nonen.Bookkeeping.ui.motion.motionSpring
import com.nonen.Bookkeeping.ui.motion.rememberReducedMotion
import com.nonen.Bookkeeping.ui.screens.AddEditScreen
import com.nonen.Bookkeeping.ui.screens.AddEditViewModel
import com.nonen.Bookkeeping.ui.screens.AboutScreen
import com.nonen.Bookkeeping.ui.screens.AssetManageScreen
import com.nonen.Bookkeeping.ui.screens.AssetManageViewModel
import com.nonen.Bookkeeping.ui.screens.GuideScreen
import com.nonen.Bookkeeping.ui.screens.BudgetScreen
import com.nonen.Bookkeeping.ui.screens.BudgetViewModel
import com.nonen.Bookkeeping.ui.screens.CalendarScreen
import com.nonen.Bookkeeping.ui.screens.CalendarViewModel
import com.nonen.Bookkeeping.ui.screens.CategoryEditScreen
import com.nonen.Bookkeeping.ui.screens.CategoryManageScreen
import com.nonen.Bookkeeping.ui.screens.CategoryManageViewModel
import com.nonen.Bookkeeping.ui.screens.MainScreen
import com.nonen.Bookkeeping.ui.screens.PeriodicScreen
import com.nonen.Bookkeeping.ui.screens.PeriodicViewModel
import com.nonen.Bookkeeping.ui.screens.RulesScreen
import com.nonen.Bookkeeping.ui.screens.RulesViewModel
import com.nonen.Bookkeeping.ui.screens.SavingsGoalScreen
import com.nonen.Bookkeeping.ui.screens.SavingsGoalViewModel
import com.nonen.Bookkeeping.ui.screens.SearchScreen
import com.nonen.Bookkeeping.ui.screens.SearchViewModel
import com.nonen.Bookkeeping.ui.screens.TemplateScreen
import com.nonen.Bookkeeping.ui.screens.TemplateViewModel
import com.nonen.Bookkeeping.ui.screens.SettingsScreen
import com.nonen.Bookkeeping.ui.screens.SettingsViewModel
import com.nonen.Bookkeeping.ui.screens.YimuAddEditScreen
import com.nonen.Bookkeeping.ui.screens.YimuThemeScreen
import com.nonen.Bookkeeping.ui.screens.SettingsPage
import com.nonen.Bookkeeping.ui.screens.StatisticsBillFilter
import com.nonen.Bookkeeping.ui.screens.StatisticsBillListScreen
import com.nonen.Bookkeeping.ui.screens.StatisticsBillListViewModel
import com.nonen.Bookkeeping.ui.screens.TagEditScreen
import com.nonen.Bookkeeping.ui.screens.TagManageScreen
import java.time.LocalDate
import kotlinx.coroutines.launch
import com.nonen.Bookkeeping.data.db.BookEntity

object Routes {
    const val MAIN = "main"
    const val ADD_EDIT = "add/{txId}?templateId={templateId}&date={date}&editTemplate={editTemplate}&templateType={templateType}"
    const val SEARCH = "search"
    const val STATISTICS_BILLS = "statistics_bills/{bookId}/{start}/{end}/{income}" +
        "?category={category}&subCategory={subCategory}&tag={tag}"
    const val RULES = "rules"
    const val CATEGORY_MANAGE = "category_manage"
    const val CATEGORY_EDIT = "category_edit?parentId={parentId}&categoryId={categoryId}&type={type}"
    const val ASSET_MANAGE = "asset_manage"
    const val BUDGET = "budget"
    const val PERIODIC = "periodic"
    const val TEMPLATE = "template"
    const val SAVINGS_GOAL = "savings_goal"
    const val CALENDAR = "calendar"
    const val TAG_MANAGE = "tag_manage"
    const val ABOUT = "about"
    const val GUIDE = "guide/{document}?title={title}"
    const val TAG_EDIT = "tag_edit/{tagId}?parentTagId={parentTagId}"
    const val SETTINGS = "settings"
    const val APPEARANCE = "appearance"
    const val IMPORT_EXPORT = "import_export"
    const val BACKUP = "backup"
    const val MAIN_PATTERN = "main?tab={tab}"

    fun mainTab(index: Int) = "main?tab=$index"

    fun add(txId: Long, date: LocalDate? = null) = "add/$txId" + (date?.let { "?date=$it" } ?: "")
    fun fromTemplate(templateId: Long) = "add/0?templateId=$templateId"
    fun tagEdit(tagId: Long, parentTagId: Long = 0L) = "tag_edit/$tagId?parentTagId=$parentTagId"
    fun guide(document: String, title: String) = "guide/$document?title=${Uri.encode(title)}"
    fun categoryEdit(parentId: Int, categoryId: Int = 0, type: Int = 0) =
        "category_edit?parentId=$parentId&categoryId=$categoryId&type=$type"
    fun editTemplate(templateId: Long, moduleType: Int = 0): String {
        require(moduleType in 0..1)
        return "add/0?templateId=$templateId&editTemplate=true&templateType=$moduleType"
    }
    fun statisticsBills(filter: StatisticsBillFilter): String =
        "statistics_bills/${filter.bookId}/${filter.start}/${filter.end}/${filter.income}" +
            listOfNotNull(
                filter.category?.let { "category=${Uri.encode(it)}" },
                filter.subCategory?.let { "subCategory=${Uri.encode(it)}" },
                filter.tag?.let { "tag=${Uri.encode(it)}" },
            ).joinToString("&").let { if (it.isEmpty()) "" else "?$it" }
}


inline fun <reified VM : ViewModel> vmFactory(crossinline create: () -> VM): ViewModelProvider.Factory =
    viewModelFactory {
        initializer { create() }
    }

/** iOS 风格推入/推出过渡：新页整宽从右滑入，旧页退到 -1/3 宽；弹出相反（参照墨麒麟） */
@Composable
fun AppNavHost(
    startDestination: String = Routes.MAIN,
    navController: NavHostController = rememberNavController(),
) {
    val container = (LocalContext.current.applicationContext as BookkeepingApp).container
    val nav = navController
    val scope = rememberCoroutineScope()
    val reduced = rememberReducedMotion()
    val selectedBookId by container.settings.selectedBookId.collectAsState(initial = 1L)
    val books by remember(container) { container.bookDao.getAllFlow() }.collectAsState(initial = emptyList())
    // 记录首次 startDestination：主题切换会触发 Activity 重建，NavController 恢复原返回栈后
    // 不能再把 startDestination 压入一次，否则会从主题页跳回首页；只有新 intent 改变入口时才导航。
    val entryRoute = rememberSaveable { mutableStateOf(startDestination) }
    LaunchedEffect(startDestination) {
        if (entryRoute.value != startDestination) {
            nav.navigate(startDestination) { launchSingleTop = true }
            entryRoute.value = startDestination
        }
    }

    NavHost(
        navController = nav,
        startDestination = startDestination,
        // 滑动页面保持完全不透明（iOS 推入式）：任何时刻进出两页都铺满整屏，
        // 半透明淡入/淡出会让底层窗口背景透出造成白闪
        enterTransition = {
            if (reduced) fadeIn(tween(150))
            else slideInHorizontally(motionSpring()) { it }
        },
        exitTransition = {
            if (reduced) fadeOut(tween(150))
            else slideOutHorizontally(motionSpring()) { -it / 3 }
        },
        popEnterTransition = {
            if (reduced) fadeIn(tween(150))
            else slideInHorizontally(motionSpring()) { -it / 3 }
        },
        popExitTransition = {
            if (reduced) fadeOut(tween(150))
            else slideOutHorizontally(motionSpring()) { it }
        },
    ) {
        composable(Routes.MAIN_PATTERN, arguments = listOf(navArgument("tab") { type = NavType.IntType; defaultValue = 0 })) { entry ->
            MainScreen(
                container = container,
                onAdd = { nav.navigate(Routes.add(0L)) },
                onAddAtDate = { date -> nav.navigate(Routes.add(0L, date)) },
                onStatisticsBills = { nav.navigate(Routes.statisticsBills(it)) },
                onEdit = { txId -> nav.navigate(Routes.add(txId)) },
                onSearch = { nav.navigateSingleTop(Routes.SEARCH) },
                onRules = { nav.navigateSingleTop(Routes.RULES) },
                onCategoryManage = { nav.navigateSingleTop(Routes.CATEGORY_MANAGE) },
                onAssetManage = { nav.navigateSingleTop(Routes.ASSET_MANAGE) },
                onBudget = { nav.navigateSingleTop(Routes.BUDGET) },
                onPeriodic = { nav.navigateSingleTop(Routes.PERIODIC) },
                onTemplate = { nav.navigateSingleTop(Routes.TEMPLATE) },
                onApplyTemplate = { nav.navigate(Routes.fromTemplate(it)) },
                onSavingsGoal = { nav.navigateSingleTop(Routes.SAVINGS_GOAL) },
                onCalendar = { nav.navigateSingleTop(Routes.CALENDAR) },
                onSettings = { nav.navigateSingleTop(Routes.SETTINGS) },
                onAppearance = { nav.navigateSingleTop(Routes.APPEARANCE) },
                onImportExport = { nav.navigateSingleTop(Routes.IMPORT_EXPORT) },
                onBackup = { nav.navigateSingleTop(Routes.BACKUP) },
                onTagManage = { nav.navigateSingleTop(Routes.TAG_MANAGE) },
                onAbout = { nav.navigateSingleTop(Routes.ABOUT) },
                onGuide = { nav.navigateSingleTop(Routes.guide("guide.html", "使用指南")) },
                initialTab = entry.arguments?.getInt("tab") ?: 0,
            )
        }


        composable(
            Routes.ADD_EDIT,
            arguments = listOf(navArgument("txId") { type = NavType.LongType },
                navArgument("templateId") { type = NavType.LongType; defaultValue = 0L },
                navArgument("date") { type = NavType.StringType; defaultValue = "" },
                navArgument("editTemplate") { type = NavType.BoolType; defaultValue = false },
                navArgument("templateType") { type = NavType.IntType; defaultValue = 0 }),
        ) { entry ->
            val txId = entry.arguments?.getLong("txId") ?: 0L
            val templateId = entry.arguments?.getLong("templateId") ?: 0L
            val initialDate = entry.arguments?.getString("date")?.takeIf { it.isNotBlank() }?.let { LocalDate.parse(it) }
            val editingTemplate = entry.arguments?.getBoolean("editTemplate") ?: false
            val templateType = entry.arguments?.getInt("templateType") ?: 0
            require(templateType in 0..1)
            val vm: AddEditViewModel = viewModel(
                key = "add_edit_${txId}_${templateId}_${initialDate}_${editingTemplate}_$templateType",
                factory = vmFactory {
                    AddEditViewModel(
                        repo = container.transactionRepository,
                        ruleEngine = container.ruleEngine,
                        ruleRepo = container.ruleRepository,
                        categoryRepo = container.categoryRepository,
                        assetRepo = container.assetRepository,
                        templateRepo = container.templateRepository,
                        tagRepo = container.tagRepository,
                        txId = txId,
                        initialBookId = selectedBookId,
                        initialTemplateId = templateId,
                        initialDate = initialDate,
                        editingTemplate = editingTemplate,
                        initialTransactionType = if (templateType == 1) "TRANSFER" else "EXPENSE",
                    )
                },
            )
            YimuAddEditScreen(
                vm = vm,
                books = books,
                onCreateBook = { name -> scope.launch {
                    vm.selectBook(container.bookDao.insert(BookEntity(bookName = name.trim())))
                } },
                onBack = { nav.popBackStack() },
                onManageCategories = { nav.navigateSingleTop(Routes.CATEGORY_MANAGE) },
                onManageAssets = { nav.navigateSingleTop(Routes.ASSET_MANAGE) },
            )
        }

        composable(Routes.SEARCH) {
            val vm: SearchViewModel = viewModel(factory = vmFactory { SearchViewModel(container.transactionRepository) })
            SearchScreen(vm = vm, onBack = { nav.popBackStack() }, onEdit = { nav.navigate(Routes.add(it)) })
        }

        composable(
            Routes.STATISTICS_BILLS,
            arguments = listOf(
                navArgument("bookId") { type = NavType.LongType },
                navArgument("start") { type = NavType.StringType },
                navArgument("end") { type = NavType.StringType },
                navArgument("income") { type = NavType.BoolType },
                navArgument("category") { type = NavType.StringType; nullable = true; defaultValue = null },
                navArgument("subCategory") { type = NavType.StringType; nullable = true; defaultValue = null },
                navArgument("tag") { type = NavType.StringType; nullable = true; defaultValue = null },
            ),
        ) { entry ->
            val arguments = requireNotNull(entry.arguments)
            val filter = StatisticsBillFilter(
                bookId = arguments.getLong("bookId"),
                start = LocalDate.parse(requireNotNull(arguments.getString("start"))),
                end = LocalDate.parse(requireNotNull(arguments.getString("end"))),
                income = arguments.getBoolean("income"),
                category = arguments.getString("category"),
                subCategory = arguments.getString("subCategory"),
                tag = arguments.getString("tag"),
            )
            val vm: StatisticsBillListViewModel = viewModel(factory = vmFactory {
                StatisticsBillListViewModel(container.transactionRepository, filter)
            })
            StatisticsBillListScreen(
                vm = vm, onBack = { nav.popBackStack() },
                onEdit = { nav.navigate(Routes.add(it)) },
                onSubCategory = { nav.navigate(Routes.statisticsBills(it)) },
            )
        }

        composable(Routes.APPEARANCE) {
            YimuThemeScreen(
                container = container,
                onBack = { nav.popBackStack() },
                onThemeMode = { mode -> scope.launch { container.settings.setThemeMode(mode) } },
            )
        }

        listOf(Routes.SETTINGS to SettingsPage.ROOT,
            Routes.IMPORT_EXPORT to SettingsPage.IMPORT_EXPORT, Routes.BACKUP to SettingsPage.BACKUP).forEach { (route, page) ->
        composable(route) {
            val vm: SettingsViewModel = viewModel(factory = vmFactory { SettingsViewModel(container) })
            SettingsScreen(
                vm, onRules = { nav.navigateSingleTop(Routes.RULES) },
                onCategoryManage = { nav.navigateSingleTop(Routes.CATEGORY_MANAGE) },
                onAssetManage = { nav.navigateSingleTop(Routes.ASSET_MANAGE) },
                onBudget = { nav.navigateSingleTop(Routes.BUDGET) },
                onPeriodic = { nav.navigateSingleTop(Routes.PERIODIC) },
                onTemplate = { nav.navigateSingleTop(Routes.TEMPLATE) },
                onSavingsGoal = { nav.navigateSingleTop(Routes.SAVINGS_GOAL) },
                onBack = { nav.popBackStack() },
                page = page,
                onAppearance = { nav.navigateSingleTop(Routes.APPEARANCE) },
                onImportExport = { nav.navigateSingleTop(Routes.IMPORT_EXPORT) },
                onBackup = { nav.navigateSingleTop(Routes.BACKUP) },
            )
        }
        }

        composable(Routes.RULES) {
            val vm: RulesViewModel = viewModel(
                factory = vmFactory {
                    RulesViewModel(
                        ruleRepo = container.ruleRepository,
                        ruleEngine = container.ruleEngine,
                    )
                },
            )
            RulesScreen(vm = vm, onBack = { nav.popBackStack() })
        }

        composable(Routes.CATEGORY_MANAGE) {
            val vm: CategoryManageViewModel = viewModel(
                factory = vmFactory {
                    CategoryManageViewModel(container.categoryRepository)
                },
            )
            CategoryManageScreen(
                vm = vm,
                onBack = { nav.popBackStack() },
                onOpenEdit = { parentId, categoryId, categoryType ->
                    nav.navigate(Routes.categoryEdit(parentId, categoryId, categoryType))
                },
            )
        }

        composable(
            Routes.CATEGORY_EDIT,
            arguments = listOf(
                navArgument("parentId") { type = NavType.IntType; defaultValue = 0 },
                navArgument("categoryId") { type = NavType.IntType; defaultValue = 0 },
                navArgument("type") { type = NavType.IntType; defaultValue = 0 },
            ),
        ) { entry ->
            CategoryEditScreen(
                container = container,
                parentId = entry.arguments?.getInt("parentId") ?: 0,
                categoryId = entry.arguments?.getInt("categoryId") ?: 0,
                categoryType = entry.arguments?.getInt("type") ?: 0,
                onBack = { nav.popBackStack() },
            )
        }

        composable(Routes.ASSET_MANAGE) {
            val vm: AssetManageViewModel = viewModel(
                factory = vmFactory {
                    AssetManageViewModel(container.assetRepository)
                },
            )
            AssetManageScreen(vm = vm, onBack = { nav.popBackStack() })
        }

        composable(Routes.BUDGET) {
            val vm: BudgetViewModel = viewModel(
                factory = vmFactory {
                    BudgetViewModel(container.budgetRepository)
                },
            )
            BudgetScreen(viewModel = vm, onBack = { nav.popBackStack() })
        }

        composable(Routes.PERIODIC) {
            val vm: PeriodicViewModel = viewModel(
                factory = vmFactory {
                    PeriodicViewModel(container.periodicRepository, container.periodicTransactionExecutor)
                },
            )
            PeriodicScreen(viewModel = vm, onBack = { nav.popBackStack() })
        }

        composable(Routes.TEMPLATE) {
            val vm: TemplateViewModel = viewModel(
                factory = vmFactory {
                    TemplateViewModel(container.templateRepository, container.settings.selectedBookId)
                },
            )
            TemplateScreen(
                viewModel = vm,
                categoryRepository = container.categoryRepository,
                assetRepository = container.assetRepository,
                templateRepository = container.templateRepository,
                settings = container.settings,
                onBack = { nav.popBackStack() },
                onCreate = { moduleType -> nav.navigate(Routes.editTemplate(0L, moduleType)) },
                onEdit = { moduleId -> nav.navigate(Routes.editTemplate(moduleId, vm.templates.value.firstOrNull { it.moduleBillId == moduleId }?.moduleType ?: 0)) },
                onApply = { nav.navigate(Routes.fromTemplate(it)) },
            )
        }

        composable(Routes.SAVINGS_GOAL) {
            val vm: SavingsGoalViewModel = viewModel(
                factory = vmFactory {
                    SavingsGoalViewModel(container.savingsGoalRepository)
                },
            )
            SavingsGoalScreen(viewModel = vm, onBack = { nav.popBackStack() })
        }

        composable(Routes.CALENDAR) {
            val vm: CalendarViewModel = viewModel(
                factory = vmFactory {
                    CalendarViewModel(container.reportRepository)
                },
            )
            CalendarScreen(
                vm = vm,
                onBack = { nav.popBackStack() },
                onEdit = { txId -> nav.navigate(Routes.add(txId)) },
                onAdd = { nav.navigate(Routes.add(0L, vm.selectedDate.value)) },
            )
        }

        composable(Routes.TAG_MANAGE) {
            TagManageScreen(
                container = container,
                onBack = { nav.popBackStack() },
                onAddTag = { parentTagId -> nav.navigate(Routes.tagEdit(0L, parentTagId)) },
                onEditTag = { tagId -> nav.navigate(Routes.tagEdit(tagId)) },
            )
        }

        composable(
            Routes.TAG_EDIT,
            arguments = listOf(
                navArgument("tagId") { type = NavType.LongType },
                navArgument("parentTagId") { type = NavType.LongType; defaultValue = 0L },
            ),
        ) { entry ->
            TagEditScreen(
                container = container,
                tagId = entry.arguments?.getLong("tagId") ?: 0L,
                parentTagId = entry.arguments?.getLong("parentTagId") ?: 0L,
                onBack = { nav.popBackStack() },
            )
        }

        composable(Routes.ABOUT) {
            AboutScreen(
                onBack = { nav.popBackStack() },
                onOpenDocument = { document, title -> nav.navigate(Routes.guide(document, title)) },
            )
        }

        composable(
            Routes.GUIDE,
            arguments = listOf(
                navArgument("document") { type = NavType.StringType },
                navArgument("title") { type = NavType.StringType },
            ),
        ) { entry ->
            GuideScreen(
                document = entry.arguments?.getString("document") ?: "guide.html",
                title = entry.arguments?.getString("title") ?: "使用指南",
                onBack = { nav.popBackStack() },
            )
        }
    }
}


/** 相同目的地不重复入栈，避免反复点击搜索/规则堆出多层页面 */
private fun NavHostController.navigateSingleTop(route: String) {
    navigate(route) { launchSingleTop = true }
}
