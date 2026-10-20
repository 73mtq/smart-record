package com.nonen.Bookkeeping.data.prefs

import android.content.Context
import androidx.annotation.StringRes
import com.nonen.Bookkeeping.R
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore by preferencesDataStore(name = "settings")

object Packages {
    const val WECHAT = "com.tencent.mm"
    const val ALIPAY = "com.eg.android.AlipayGphone"
}

/** 监听范围：labelRes 指向 UI 文案，领域层不持有本地化字符串 */
enum class ListenScope(@param:StringRes val labelRes: Int, val packages: Set<String>) {
    ALL(R.string.scope_all, setOf(Packages.WECHAT, Packages.ALIPAY)),
    WECHAT_ONLY(R.string.scope_wechat_only, setOf(Packages.WECHAT)),
    ALIPAY_ONLY(R.string.scope_alipay_only, setOf(Packages.ALIPAY));

    companion object {
        fun from(name: String?): ListenScope = entries.firstOrNull { it.name == name } ?: ALL
    }
}

enum class ThemeMode(@param:StringRes val labelRes: Int) {
    SYSTEM(R.string.theme_system), LIGHT(R.string.theme_light), DARK(R.string.theme_dark);

    companion object {
        fun from(name: String?): ThemeMode = entries.firstOrNull { it.name == name } ?: SYSTEM
    }
}

data class SettingsSnapshot(
    val autoRecordEnabled: Boolean,
    val listenScope: ListenScope,
    val notifyOnRecord: Boolean,
    val learnOnEdit: Boolean,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColor: Boolean = true,
    val hideAmount: Boolean = false,
    val appLockEnabled: Boolean = false,
    val fontOption: String = "DEFAULT",
    val navigationLabelsEnabled: Boolean = false,
    val selectedBookId: Long = 1L,
    val noteFirst: Boolean = false,
    val simpleCategory: Boolean = false,
    val showTransactionTime: Boolean = false,
    val showAssetIcon: Boolean = false,
    val templateTipHidden: Boolean = false,
)

class SettingsStore(context: Context, private val store: DataStore<Preferences> = context.settingsDataStore) {

    private val KEY_AUTO_RECORD = booleanPreferencesKey("auto_record_enabled")
    private val KEY_LISTEN_SCOPE = stringPreferencesKey("listen_scope")
    private val KEY_NOTIFY = booleanPreferencesKey("notify_on_record")
    private val KEY_LEARN = booleanPreferencesKey("learn_on_edit")
    private val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
    private val KEY_DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
    private val KEY_HIDE_AMOUNT = booleanPreferencesKey("hide_amount")
    private val KEY_APP_LOCK = booleanPreferencesKey("app_lock_enabled")
    private val KEY_FONT_OPTION = stringPreferencesKey("font_option")
    private val KEY_NAVIGATION_LABELS = booleanPreferencesKey("navigation_labels_enabled")
    private val KEY_SELECTED_BOOK = longPreferencesKey("selected_book_id")
    private val KEY_NOTE_FIRST = booleanPreferencesKey("note_first")
    private val KEY_SIMPLE_CATEGORY = booleanPreferencesKey("simple_category")
    private val KEY_SHOW_TIME = booleanPreferencesKey("show_transaction_time")
    private val KEY_SHOW_ASSET_ICON = booleanPreferencesKey("show_asset_icon")
    private val KEY_TEMPLATE_TIP_HIDDEN = booleanPreferencesKey("template_tip_hidden")

    val autoRecordEnabled: Flow<Boolean> = store.data.map { it[KEY_AUTO_RECORD] ?: true }
    val listenScope: Flow<ListenScope> = store.data.map { ListenScope.from(it[KEY_LISTEN_SCOPE]) }
    val notifyOnRecord: Flow<Boolean> = store.data.map { it[KEY_NOTIFY] ?: true }
    val learnOnEdit: Flow<Boolean> = store.data.map { it[KEY_LEARN] ?: true }
    val themeMode: Flow<ThemeMode> = store.data.map { ThemeMode.from(it[KEY_THEME_MODE]) }
    val dynamicColor: Flow<Boolean> = store.data.map { it[KEY_DYNAMIC_COLOR] ?: true }
    val hideAmount: Flow<Boolean> = store.data.map { it[KEY_HIDE_AMOUNT] ?: false }
    val appLockEnabled: Flow<Boolean> = store.data.map { it[KEY_APP_LOCK] ?: false }
    val fontOption: Flow<String> = store.data.map { it[KEY_FONT_OPTION] ?: "DEFAULT" }
    val navigationLabelsEnabled: Flow<Boolean> = store.data.map { it[KEY_NAVIGATION_LABELS] ?: false }
    val selectedBookId: Flow<Long> = store.data.map { it[KEY_SELECTED_BOOK] ?: 1L }
    val noteFirst: Flow<Boolean> = store.data.map { it[KEY_NOTE_FIRST] ?: false }
    val simpleCategory: Flow<Boolean> = store.data.map { it[KEY_SIMPLE_CATEGORY] ?: false }
    val showTransactionTime: Flow<Boolean> = store.data.map { it[KEY_SHOW_TIME] ?: false }
    val showAssetIcon: Flow<Boolean> = store.data.map { it[KEY_SHOW_ASSET_ICON] ?: false }
    val templateTipHidden: Flow<Boolean> = store.data.map { it[KEY_TEMPLATE_TIP_HIDDEN] ?: false }

    suspend fun snapshot(): SettingsSnapshot = SettingsSnapshot(
        autoRecordEnabled = autoRecordEnabled.first(),
        listenScope = listenScope.first(),
        notifyOnRecord = notifyOnRecord.first(),
        learnOnEdit = learnOnEdit.first(),
        themeMode = themeMode.first(),
        dynamicColor = dynamicColor.first(),
        hideAmount = hideAmount.first(),
        appLockEnabled = appLockEnabled.first(),
        fontOption = fontOption.first(),
        navigationLabelsEnabled = navigationLabelsEnabled.first(),
        selectedBookId = selectedBookId.first(),
        noteFirst = noteFirst.first(), simpleCategory = simpleCategory.first(),
        showTransactionTime = showTransactionTime.first(), showAssetIcon = showAssetIcon.first(),
        templateTipHidden = templateTipHidden.first(),
    )

    suspend fun setAutoRecordEnabled(value: Boolean) = store.edit { it[KEY_AUTO_RECORD] = value }
    suspend fun setListenScope(value: ListenScope) = store.edit { it[KEY_LISTEN_SCOPE] = value.name }
    suspend fun setNotifyOnRecord(value: Boolean) = store.edit { it[KEY_NOTIFY] = value }
    suspend fun setLearnOnEdit(value: Boolean) = store.edit { it[KEY_LEARN] = value }
    suspend fun setThemeMode(value: ThemeMode) = store.edit { it[KEY_THEME_MODE] = value.name }
    suspend fun setDynamicColor(value: Boolean) = store.edit { it[KEY_DYNAMIC_COLOR] = value }
    suspend fun setHideAmount(value: Boolean) = store.edit { it[KEY_HIDE_AMOUNT] = value }
    suspend fun setAppLockEnabled(value: Boolean) = store.edit { it[KEY_APP_LOCK] = value }
    suspend fun setFontOption(value: String) = store.edit { it[KEY_FONT_OPTION] = value }
    suspend fun setNavigationLabelsEnabled(value: Boolean) = store.edit { it[KEY_NAVIGATION_LABELS] = value }
    suspend fun setSelectedBookId(value: Long) = store.edit { it[KEY_SELECTED_BOOK] = value }
    suspend fun setNoteFirst(value: Boolean) = store.edit { it[KEY_NOTE_FIRST] = value }
    suspend fun setSimpleCategory(value: Boolean) = store.edit { it[KEY_SIMPLE_CATEGORY] = value }
    suspend fun setShowTransactionTime(value: Boolean) = store.edit { it[KEY_SHOW_TIME] = value }
    suspend fun setShowAssetIcon(value: Boolean) = store.edit { it[KEY_SHOW_ASSET_ICON] = value }
    suspend fun setTemplateTipHidden(value: Boolean) = store.edit { it[KEY_TEMPLATE_TIP_HIDDEN] = value }
}
