package com.personal.financeapp.ui.screens.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.personal.financeapp.data.local.dao.TransactionWithDetails
import com.personal.financeapp.data.local.entity.CategoryEntity
import com.personal.financeapp.data.repository.*
import com.personal.financeapp.util.MonthRange
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import javax.inject.Inject

data class CategoryExpense(val category: CategoryEntity, val amount: Double)

data class DashboardUiState(
    val monthlyIncome: Double = 0.0,
    val monthlyExpense: Double = 0.0,
    val netWorth: Double = 0.0,
    val recentTransactions: List<TransactionWithDetails> = emptyList(),
    val categoryExpenses: List<CategoryExpense> = emptyList(),
    val selectedMonth: Int = MonthRange.currentMonth(),
    val selectedYear: Int = MonthRange.currentYear()
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val transactionRepo: TransactionRepository,
    private val accountRepo: AccountRepository,
    private val investmentRepo: InvestmentRepository,
    private val categoryRepo: CategoryRepository
) : ViewModel() {

    private val selectedMonth = MutableStateFlow(MonthRange.currentMonth())
    private val selectedYear = MutableStateFlow(MonthRange.currentYear())

    fun selectMonth(month: Int, year: Int) {
        selectedMonth.value = month
        selectedYear.value = year
    }

    private data class MonthSel(val month: Int, val year: Int)

    val uiState: StateFlow<DashboardUiState> =
        combine(selectedMonth, selectedYear) { m, y -> MonthSel(m, y) }
            .flatMapLatest { sel ->
                val (from, to) = MonthRange.monthRange(sel.month, sel.year)
                combine(
                    transactionRepo.getSumByType("INCOME", from, to),
                    transactionRepo.getSumByType("EXPENSE", from, to),
                    transactionRepo.getByDateRange(from, to),
                    transactionRepo.getCategoryTotals(from, to),
                    categoryRepo.getAll()
                ) { income, expense, monthTx, catTotals, categories ->
                    val catMap = categories.associateBy { it.id }
                    val catExpenses = catTotals.mapNotNull { ct ->
                        catMap[ct.categoryId]?.let { CategoryExpense(it, ct.total) }
                    }.sortedByDescending { it.amount }
                    DashboardUiState(
                        monthlyIncome = income,
                        monthlyExpense = expense,
                        netWorth = 0.0,
                        recentTransactions = monthTx.take(5),
                        categoryExpenses = catExpenses,
                        selectedMonth = sel.month,
                        selectedYear = sel.year
                    )
                }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardUiState())

    // Net worth is derived separately to avoid the combine limit
    val netWorth: StateFlow<Double> = combine(
        accountRepo.getAll(),
        investmentRepo.getTotalPortfolioValue()
    ) { accounts, portfolioValue ->
        portfolioValue // accounts balance added in the screen via repository
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)
}
