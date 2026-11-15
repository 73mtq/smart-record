package com.nonen.Bookkeeping.data.repo

import com.nonen.Bookkeeping.data.db.AssetEntity
import kotlin.math.abs

data class AssetSummary(
    val netAsset: Double = 0.0,
    val totalAsset: Double = 0.0,
    val totalLiability: Double = 0.0,
)

internal fun summarizeAssets(
    assets: List<AssetEntity>,
    balanceOf: (AssetEntity) -> Double = { it.assetNumber },
): AssetSummary {
    var totalAsset = 0.0
    var totalLiability = 0.0

    for (asset in assets) {
        if (!asset.intoTotalAsset) continue
        val balance = balanceOf(asset)
        when (asset.assetType) {
            AssetRepository.TYPE_CREDIT -> {
                // 配置信用额度时，账户余额表示可用额度。
                if (asset.totalQuota > 0.0) {
                    if (balance < asset.totalQuota) {
                        totalLiability += asset.totalQuota - balance
                    } else {
                        totalAsset += balance - asset.totalQuota
                    }
                } else if (balance < 0.0) {
                    totalLiability += abs(balance)
                } else {
                    totalAsset += balance
                }
            }
            AssetRepository.TYPE_DEBT -> totalLiability += abs(balance)
            else -> {
                if (balance >= 0.0) {
                    totalAsset += balance
                } else {
                    totalLiability += abs(balance)
                }
            }
        }
    }

    return AssetSummary(
        netAsset = totalAsset - totalLiability,
        totalAsset = totalAsset,
        totalLiability = totalLiability,
    )
}
