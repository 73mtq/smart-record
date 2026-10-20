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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import com.nonen.Bookkeeping.ui.screens.AssetManageScreen
import com.nonen.Bookkeeping.ui.screens.AssetManageViewModel
import com.nonen.Bookkeeping.ui.screens.BudgetScreen
import com.nonen.Bookkeeping.ui.screens.BudgetViewModel
import com.nonen.Bookkeeping.ui.screens.CalendarScreen
import com.nonen.Bookkeeping.ui.screens.CalendarViewModel
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
import com.nonen.Bookkeeping.ui.screens.SettingsPage
import com.nonen.Bookkeeping.ui.screens.StatisticsBillFilter
import com.nonen.Bookkeeping.ui.screens.StatisticsBillListScreen
import com.nonen.Bookkeeping.ui.screens.StatisticsBillListViewModel
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
    const val ASSET_MANAGE = "asset_manage"
    const val BUDGET = "budget"
    const val PERIODIC = "periodic"
    const val TEMPLATE = "template"
    const val SAVINGS_GOAL = "savings_goal"
    const val CALENDAR = "calendar"
    const val SETTINGS = "settings"
    const val APPEARANCE = "appearance"
    const val IMPORT_EXPORT = "import_export"
    const val BACKUP = "backup"
    const val MAIN_PATTERN = "main?tab={tab}"

    fun mainTab(index: Int) = "main?tab=$index"

    fun add(txId: Long, date: LocalDate? = null) = "add/$txId" + (date?.let { "?date=$it" } ?: "")
    fun fromTemplate(templateId: Long) = "add/0?templateId=$templateId"
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
    LaunchedEffect(startDestination) {
        if (nav.currentDestination != null) nav.navigate(startDestination) { launchSingleTop = true }
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

        listOf(Routes.SETTINGS to SettingsPage.ROOT, Routes.APPEARANCE to SettingsPage.APPEARANCE,
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
            CategoryManageScreen(vm = vm, onBack = { nav.popBackStack() })
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
    }
}


/** 相同目的地不重复入栈，避免反复点击搜索/规则堆出多层页面 */
private fun NavHostController.navigateSingleTop(route: String) {
    navigate(route) { launchSingleTop = true }
}
