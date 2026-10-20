package com.nonen.Bookkeeping.ui.screens

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nonen.Bookkeeping.data.db.AssetEntity
import com.nonen.Bookkeeping.data.repo.AssetRepository
import com.nonen.Bookkeeping.data.repo.AssetSummary
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * 资产与账户管理视图模型。
 */
class AssetManageViewModel(
    private val assetRepo: AssetRepository,
    private val externalScope: CoroutineScope? = null,
) : ViewModel() {

    private val scope: CoroutineScope get() = externalScope ?: viewModelScope

    val showHidden = MutableStateFlow(false)

    private val allAssets: StateFlow<List<AssetEntity>> = assetRepo.observeAll()
        .stateIn(scope, SharingStarted.Eagerly, emptyList())

    /**
     * 当前界面显示的资产列表（受 showHidden 控制）。
     */
    val displayAssets: StateFlow<List<AssetEntity>> = combine(allAssets, showHidden) { list, hidden ->
        if (hidden) list else list.filter { !it.hide }
    }.stateIn(scope, SharingStarted.Eagerly, emptyList())

    /**
     * 实时统计数据（依据全量资产中 intoTotalAsset == true 计算）。
     */
    val summary: StateFlow<AssetSummary> = allAssets
        .map { assetRepo.calculateSummary(it) }
        .stateIn(scope, SharingStarted.Eagerly, AssetSummary())

    // 弹窗状态管理
    var showAddEditDialog by mutableStateOf(false)
        private set
    var editingAsset by mutableStateOf<AssetEntity?>(null)
        private set

    var showAdjustDialog by mutableStateOf(false)
        private set
    var adjustingAsset by mutableStateOf<AssetEntity?>(null)
        private set

    var showTransferDialog by mutableStateOf(false)
        private set

    var showDeleteDialog by mutableStateOf(false)
        private set
    var deletingAsset by mutableStateOf<AssetEntity?>(null)
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    fun setShowHidden(value: Boolean) {
        showHidden.value = value
    }

    fun openAddDialog() {
        editingAsset = null
        showAddEditDialog = true
    }

    fun openEditDialog(asset: AssetEntity) {
        editingAsset = asset
        showAddEditDialog = true
    }

    fun closeAddEditDialog() {
        showAddEditDialog = false
        editingAsset = null
    }

    fun openAdjustDialog(asset: AssetEntity) {
        adjustingAsset = asset
        showAdjustDialog = true
    }

    fun closeAdjustDialog() {
        showAdjustDialog = false
        adjustingAsset = null
    }

    fun openTransferDialog() {
        showTransferDialog = true
    }

    fun closeTransferDialog() {
        showTransferDialog = false
    }

    fun transfer(
        fromAssetId: Long,
        toAssetId: Long,
        amount: Double,
        serviceCharge: Double = 0.0,
        remark: String? = null,
        onDone: () -> Unit = {},
    ) {
        if (fromAssetId == toAssetId) {
            errorMessage = "转出账户与转入账户不能相同"
            return
        }
        if (amount <= 0.0) {
            errorMessage = "转账金额必须大于 0"
            return
        }
        scope.launch {
            val success = assetRepo.executeTransfer(fromAssetId, toAssetId, amount, serviceCharge, remark)
            if (success) {
                closeTransferDialog()
                onDone()
            } else {
                errorMessage = "转账执行失败"
            }
        }
    }

    fun openDeleteDialog(asset: AssetEntity) {
        if (asset.assetId == AssetRepository.DEFAULT_ASSET_ID) {
            errorMessage = "默认现金账户不可删除"
            return
        }
        deletingAsset = asset
        showDeleteDialog = true
    }

    fun closeDeleteDialog() {
        showDeleteDialog = false
        deletingAsset = null
    }

    fun clearError() {
        errorMessage = null
    }

    /**
     * 保存资产账户（新增或修改）。
     */
    fun saveAsset(
        name: String,
        balance: Double,
        quota: Double,
        type: Int,
        groupName: String?,
        intoTotalAsset: Boolean,
    ) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) {
            errorMessage = "账户名称不可为空"
            return
        }

        scope.launch {
            val current = editingAsset
            if (current != null) {
                val updated = current.copy(
                    assetName = trimmed,
                    assetNumber = balance,
                    totalQuota = quota,
                    assetType = type,
                    groupName = groupName?.trim()?.ifEmpty { null },
                    intoTotalAsset = intoTotalAsset,
                )
                assetRepo.updateAsset(updated)
            } else {
                assetRepo.createAsset(
                    name = trimmed,
                    initialBalance = balance,
                    quota = quota,
                    type = type,
                    groupName = groupName,
                    intoTotalAsset = intoTotalAsset,
                )
            }
            closeAddEditDialog()
        }
    }

    /**
     * 调平资产余额。
     */
    fun adjustBalance(
        assetId: Long,
        targetBalance: Double,
        generateBill: Boolean,
        note: String?,
    ) {
        scope.launch {
            assetRepo.adjustBalance(assetId, targetBalance, generateBill, note)
            closeAdjustDialog()
        }
    }

    /**
     * 切换账户隐藏。
     */
    fun toggleHide(asset: AssetEntity) {
        scope.launch {
            assetRepo.toggleHide(asset.assetId, !asset.hide)
        }
    }

    /**
     * 删除指定资产。
     */
    fun deleteAsset() {
        val target = deletingAsset ?: return
        scope.launch {
            val success = assetRepo.deleteAsset(target.assetId)
            if (!success) {
                errorMessage = "默认账户不可删除"
            }
            closeDeleteDialog()
        }
    }

    /**
     * 上移资产。
     */
    fun moveUp(assetId: Long) {
        val currentList = allAssets.value
        val index = currentList.indexOfFirst { it.assetId == assetId }
        if (index > 0) {
            val target = currentList.toMutableList()
            val item = target.removeAt(index)
            target.add(index - 1, item)
            scope.launch {
                assetRepo.reorderAssets(target.map { it.assetId })
            }
        }
    }

    /**
     * 下移资产。
     */
    fun moveDown(assetId: Long) {
        val currentList = allAssets.value
        val index = currentList.indexOfFirst { it.assetId == assetId }
        if (index >= 0 && index < currentList.size - 1) {
            val target = currentList.toMutableList()
            val item = target.removeAt(index)
            target.add(index + 1, item)
            scope.launch {
                assetRepo.reorderAssets(target.map { it.assetId })
            }
        }
    }
}
