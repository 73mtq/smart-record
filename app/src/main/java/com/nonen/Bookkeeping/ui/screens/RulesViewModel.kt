package com.nonen.Bookkeeping.ui.screens

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nonen.Bookkeeping.core.RuleEngine
import com.nonen.Bookkeeping.data.repo.RuleRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class RulesViewModel(
    private val ruleRepo: RuleRepository,
    private val ruleEngine: RuleEngine? = null,
) : ViewModel() {
    val rules = ruleRepo.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val merchantCount = ruleRepo.observeMerchantCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    var testInput by mutableStateOf("")
    var testIsIncome by mutableStateOf(false)
    var testResult by mutableStateOf<RuleEngine.MatchDetail?>(null)
        private set

    fun setTestText(text: String) {
        testInput = text
        triggerTest()
    }

    fun setTestIncome(income: Boolean) {
        testIsIncome = income
        triggerTest()
    }

    fun triggerTest() {
        val text = testInput.trim()
        if (text.isEmpty()) {
            testResult = null
            return
        }
        viewModelScope.launch {
            val rulesSnapshot = ruleEngine?.loadRules()
            if (rulesSnapshot != null) {
                testResult = RuleEngine.classifyDetail(
                    text = text,
                    isIncome = testIsIncome,
                    rules = rulesSnapshot,
                    merchant = text,
                )
            }
        }
    }

    fun add(keyword: String, category: String, isIncome: Boolean, onResult: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val added = ruleRepo.add(keyword, category, isIncome)
            onResult(added)
            if (added && testInput.isNotBlank()) triggerTest()
        }
    }

    fun delete(id: Long) {
        viewModelScope.launch {
            ruleRepo.delete(id)
            if (testInput.isNotBlank()) triggerTest()
        }
    }

    fun clearMerchantMemory() {
        viewModelScope.launch {
            ruleRepo.clearMerchantMemory()
            if (testInput.isNotBlank()) triggerTest()
        }
    }
}
