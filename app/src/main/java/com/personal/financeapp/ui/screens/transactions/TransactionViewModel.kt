package com.personal.financeapp.ui.screens.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.personal.financeapp.data.local.dao.TransactionWithDetails
import com.personal.financeapp.data.local.entity.AccountEntity
import com.personal.financeapp.data.local.entity.CategoryEntity
import com.personal.financeapp.data.local.entity.TransactionEntity
import com.personal.financeapp.data.repository.AccountRepository
import com.personal.financeapp.data.repository.CategoryRepository
import com.personal.financeapp.data.repository.NetWorthService
import com.personal.financeapp.data.repository.TransactionRepository
import com.personal.financeapp.util.MonthRange
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TransactionUiState(
    val transactions: List<TransactionWithDetails> = emptyList(),
    val categories: List<CategoryEntity> = emptyList(),
    val accounts: List<AccountEntity> = emptyList(),
    val filterType: String? = null,
    val searchQuery: String = "",
    val selectedMonth: Int = MonthRange.currentMonth(),
    val selectedYear: Int = MonthRange.currentYear()
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class TransactionViewModel @Inject constructor(
    private val transactionRepo: TransactionRepository,
    private val categoryRepo: CategoryRepository,
    private val accountRepo: AccountRepository,
    private val netWorthService: NetWorthService
) : ViewModel() {

    private val filterType = MutableStateFlow<String?>(null)
    private val searchQuery = MutableStateFlow("")
    private val selectedMonth = MutableStateFlow(MonthRange.currentMonth())
    private val selectedYear = MutableStateFlow(MonthRange.currentYear())

    fun selectMonth(month: Int, year: Int) {
        selectedMonth.value = month
        selectedYear.value = year
    }

    private data class MonthSel(val month: Int, val year: Int)
    private data class Filters(val type: String?, val query: String, val month: Int, val year: Int)

    private val monthSelFlow: Flow<MonthSel> =
        combine(selectedMonth, selectedYear) { m, y -> MonthSel(m, y) }

    private val monthTx: Flow<List<TransactionWithDetails>> = monthSelFlow.flatMapLatest { sel ->
        val (from, to) = MonthRange.monthRange(sel.month, sel.year)
        transactionRepo.getByDateRange(from, to)
    }

    private val filters: Flow<Filters> =
        combine(filterType, searchQuery, monthSelFlow) { t, q, sel ->
            Filters(t, q, sel.month, sel.year)
        }

    val uiState: StateFlow<TransactionUiState> = combine(
        monthTx,
        categoryRepo.getAll(),
        accountRepo.getAll(),
        filters
    ) { transactions, categories, accounts, f ->
        val filtered = transactions
            .filter { f.type == null || it.transaction.type == f.type }
            .filter { f.query.isBlank() || it.transaction.description.contains(f.query, ignoreCase = true) ||
                    it.category?.name?.contains(f.query, ignoreCase = true) == true }
        TransactionUiState(filtered, categories, accounts, f.type, f.query, f.month, f.year)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TransactionUiState())

    fun setFilter(type: String?) { filterType.value = type }
    fun setSearch(query: String) { searchQuery.value = query }

    fun insert(transaction: TransactionEntity) = viewModelScope.launch {
        transactionRepo.insert(transaction)
        netWorthService.refreshSnapshot()
    }

    fun update(transaction: TransactionEntity) = viewModelScope.launch {
        transactionRepo.update(transaction)
        netWorthService.refreshSnapshot()
    }

    fun delete(transaction: TransactionEntity) = viewModelScope.launch {
        transactionRepo.delete(transaction)
        netWorthService.refreshSnapshot()
    }

    fun insertCategory(name: String, type: String) = viewModelScope.launch {
        val colors = listOf("#F44336","#2196F3","#FF9800","#4CAF50","#9C27B0","#00BCD4","#FF5722","#3F51B5")
        categoryRepo.insert(CategoryEntity(name = name, type = type, color = colors.random(), icon = "label", monthlyBudget = null))
    }

    fun insertAccount(name: String, type: String) = viewModelScope.launch {
        val color = when (type) {
            "CREDIT_CARD" -> "#5C6BC0"
            "SAVINGS"     -> "#009688"
            "CASH"        -> "#4CAF50"
            else          -> "#2D5A3F"
        }
        accountRepo.insert(AccountEntity(name = name, type = type, initialBalance = 0.0, color = color, icon = "account_balance"))
    }
}
