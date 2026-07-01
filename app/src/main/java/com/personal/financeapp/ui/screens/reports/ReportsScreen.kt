package com.personal.financeapp.ui.screens.reports

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.personal.financeapp.ui.components.BarChart
import com.personal.financeapp.ui.components.DonutChart
import com.personal.financeapp.ui.components.LineChart
import com.personal.financeapp.ui.components.MonthSelector
import com.personal.financeapp.ui.components.MonthlyBarData
import com.personal.financeapp.ui.theme.ExpenseRed
import com.personal.financeapp.ui.theme.IncomeGreen
import com.personal.financeapp.ui.theme.Terra
import com.personal.financeapp.util.CurrencyFormatter
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ReportsScreen(
    viewModel: ReportsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { padding ->
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(padding),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(start = 4.dp, end = 4.dp, top = 4.dp, bottom = 8.dp)
            ) {
                Text(
                    "ANALYTICS",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(2.dp))
                Text("Reports", style = MaterialTheme.typography.headlineLarge)
            }
        }
        item { NetWorthHeroCard(state.netWorthHistory.lastOrNull()?.netWorth ?: 0.0) }
        item {
            MonthSelector(
                selectedMonth = state.selectedMonth,
                selectedYear = state.selectedYear,
                onMonthChange = { m, y -> viewModel.selectMonth(m, y) }
            )
        }
        if (state.categoryBreakdown.isNotEmpty()) {
            item { CategoryBreakdownCard(state) }
        } else {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No expenses in this month",
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
        item { NetWorthHistoryCard(state) }
        item { MonthlyBarChartCard(state.monthlyData) }
    }
    } // Scaffold
}

@Composable
private fun NetWorthHeroCard(netWorth: Double) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                "NET WORTH",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(6.dp))
            Text(
                CurrencyFormatter.format(netWorth),
                style = MaterialTheme.typography.displayMedium,
                color = if (netWorth >= 0) IncomeGreen else Terra
            )
        }
    }
}

@Composable
private fun MonthlyBarChartCard(data: List<MonthlyData>) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Income vs Expenses (12 months)", style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(12.dp))
            if (data.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().height(140.dp), contentAlignment = Alignment.Center) {
                    Text("No data", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                BarChart(
                    data = data.map { MonthlyBarData(it.label, it.income, it.expense) },
                    modifier = Modifier.fillMaxWidth().height(140.dp),
                    incomeColor = IncomeGreen,
                    expenseColor = ExpenseRed
                )
                Spacer(Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                    LegendItem(IncomeGreen, "Income")
                    LegendItem(ExpenseRed, "Expenses")
                }
            }
        }
    }
}

@Composable
private fun CategoryBreakdownCard(state: ReportsUiState) {
    val breakdown = state.categoryBreakdown
    val monthNames = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
    val cal = Calendar.getInstance().apply { set(state.selectedYear, state.selectedMonth, 1) }
    val totalSpent = breakdown.sumOf { it.amount }

    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Expenses — ${monthNames.format(cal.time)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold)
                Text(CurrencyFormatter.format(totalSpent),
                    style = MaterialTheme.typography.titleMedium,
                    color = ExpenseRed, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                DonutChart(
                    data = breakdown.map { b ->
                        runCatching { androidx.compose.ui.graphics.Color(android.graphics.Color.parseColor(b.category.color)) }
                            .getOrElse { androidx.compose.ui.graphics.Color.Gray } to b.amount
                    },
                    modifier = Modifier.size(100.dp)
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    breakdown.take(6).forEach { item ->
                        val catColor = runCatching { androidx.compose.ui.graphics.Color(android.graphics.Color.parseColor(item.category.color)) }
                            .getOrElse { androidx.compose.ui.graphics.Color.Gray }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier.size(10.dp).clip(CircleShape)
                                    .background(catColor)
                            )
                            Text(item.category.name, modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.bodyMedium, maxLines = 1)
                            Text(CurrencyFormatter.format(item.amount),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NetWorthHistoryCard(state: ReportsUiState) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Net Worth Over Time", style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            if (state.netWorthHistory.size < 2) {
                Box(
                    modifier = Modifier.fillMaxWidth().height(120.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Add transactions to see your net worth trend",
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                val chartData = state.netWorthHistory.mapIndexed { i, s -> i.toFloat() to s.netWorth.toFloat() }
                LineChart(
                    data = chartData,
                    lineColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.fillMaxWidth().height(140.dp)
                )
                Spacer(Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    val fmt = SimpleDateFormat("MMM dd", Locale.getDefault())
                    Text(fmt.format(Date(state.netWorthHistory.first().date)),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(fmt.format(Date(state.netWorthHistory.last().date)),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun LegendItem(color: androidx.compose.ui.graphics.Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(color))
        Text(label, style = MaterialTheme.typography.labelMedium)
    }
}
