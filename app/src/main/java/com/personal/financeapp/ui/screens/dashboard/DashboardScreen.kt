package com.personal.financeapp.ui.screens.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.personal.financeapp.data.local.dao.TransactionWithDetails
import com.personal.financeapp.ui.components.DonutChart
import com.personal.financeapp.ui.components.MonthSelector
import com.personal.financeapp.ui.theme.Forest
import com.personal.financeapp.ui.theme.IncomeGreen
import com.personal.financeapp.ui.theme.Terra
import com.personal.financeapp.util.CurrencyFormatter
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun DashboardScreen(
    onAddTransaction: () -> Unit,
    isDarkTheme: Boolean = false,
    onToggleTheme: () -> Unit = {},
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val balance = state.monthlyIncome - state.monthlyExpense

    val dateLabel = remember { SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()).format(Date()) }
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    val greeting = when { hour < 12 -> "Good morning"; hour < 17 -> "Good afternoon"; else -> "Good evening" }

    val groupedTx = remember(state.recentTransactions) {
        state.recentTransactions.groupBy { txDateLabel(it.transaction.date) }.entries.toList()
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddTransaction,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 8.dp)
            ) { Icon(Icons.Default.Add, "Add transaction") }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = 88.dp),
        ) {
            // ── Editorial header ──────────────────────────────────
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.background)
                        .padding(start = 20.dp, end = 8.dp, top = 4.dp, bottom = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Column {
                        Text(
                            dateLabel.uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(greeting, style = MaterialTheme.typography.headlineLarge)
                    }
                    IconButton(onClick = onToggleTheme) {
                        Icon(
                            if (isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                            "Toggle theme",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // ── Month navigator ──────────────────────────────────
            item {
                Box(modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 10.dp)) {
                    MonthSelector(
                        selectedMonth = state.selectedMonth,
                        selectedYear = state.selectedYear,
                        onMonthChange = { m, y -> viewModel.selectMonth(m, y) }
                    )
                }
            }

            // ── Monthly balance hero ───────────────────────────────
            item {
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    AtelierCard {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text(
                                "NET FLOW",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                CurrencyFormatter.format(balance),
                                style = MaterialTheme.typography.displayMedium,
                                color = if (balance >= 0) IncomeGreen else Terra
                            )
                        }
                    }
                }
            }

            // ── Earned / Spent side-by-side ──────────────────────────
            item {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedCard(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(Modifier.size(6.dp).background(IncomeGreen, CircleShape))
                                Text("EARNED", style = MaterialTheme.typography.labelSmall, color = IncomeGreen)
                            }
                            Spacer(Modifier.height(6.dp))
                            Text(
                                CurrencyFormatter.format(state.monthlyIncome),
                                style = MaterialTheme.typography.headlineSmall
                            )
                        }
                    }
                    OutlinedCard(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(Modifier.size(6.dp).background(Terra, CircleShape))
                                Text("SPENT", style = MaterialTheme.typography.labelSmall, color = Terra)
                            }
                            Spacer(Modifier.height(6.dp))
                            Text(
                                CurrencyFormatter.format(state.monthlyExpense),
                                style = MaterialTheme.typography.headlineSmall
                            )
                        }
                    }
                }
            }

            // ── Spending breakdown ───────────────────────────────────
            if (state.categoryExpenses.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(12.dp))
                    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                        SpendingCard(state.categoryExpenses)
                    }
                }
            }

            // ── Recent activity ─────────────────────────────────────
            if (state.recentTransactions.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Recent activity", style = MaterialTheme.typography.headlineSmall)
                    }
                }

                groupedTx.forEach { (dateStr, txList) ->
                    item(key = "tx_group_$dateStr") {
                        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                            Text(
                                dateStr.uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(start = 2.dp, bottom = 6.dp, top = if (dateStr != groupedTx.first().key) 12.dp else 0.dp)
                            )
                            AtelierCard {
                                txList.forEachIndexed { i, item ->
                                    TxRow(item, last = i == txList.lastIndex)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SpendingCard(expenses: List<CategoryExpense>) {
    val total = expenses.sumOf { it.amount }
    AtelierCard {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                "SPENDING BY CATEGORY",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(4.dp))
            Text(CurrencyFormatter.format(total), style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                DonutChart(
                    data = expenses.map { e ->
                        runCatching { Color(android.graphics.Color.parseColor(e.category.color)) }
                            .getOrElse { Color.Gray } to e.amount
                    },
                    modifier = Modifier.size(110.dp)
                )
                Column(
                    modifier = Modifier.weight(1f).align(Alignment.CenterVertically),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    expenses.take(5).forEach { exp ->
                        val catColor = runCatching { Color(android.graphics.Color.parseColor(exp.category.color)) }
                            .getOrElse { Color.Gray }
                        val pct = if (total > 0) exp.amount / total * 100 else 0.0
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(Modifier.size(8.dp).background(catColor, CircleShape))
                            Text(exp.category.name, Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, maxLines = 1)
                            Text(
                                "${String.format("%.0f", pct)}%",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TxRow(item: TransactionWithDetails, last: Boolean) {
    val tx = item.transaction
    val isIncome = tx.type == "INCOME"
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(
                        if (isIncome) Forest.copy(alpha = 0.12f) else Terra.copy(alpha = 0.12f),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (isIncome) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                    contentDescription = null,
                    tint = if (isIncome) IncomeGreen else Terra,
                    modifier = Modifier.size(16.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    item.category?.name ?: "Uncategorized",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1
                )
                if (tx.description.isNotBlank()) {
                    Text(
                        tx.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }
            Text(
                CurrencyFormatter.formatWithSign(tx.amount, isIncome),
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = if (isIncome) IncomeGreen else MaterialTheme.colorScheme.onSurface
            )
        }
        if (!last) HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant,
            thickness = 0.5.dp,
            modifier = Modifier.padding(horizontal = 14.dp)
        )
    }
}

private fun txDateLabel(dateMs: Long): String {
    val today = Calendar.getInstance()
    val txDay = Calendar.getInstance().apply { timeInMillis = dateMs }
    return when {
        today.get(Calendar.YEAR) == txDay.get(Calendar.YEAR) &&
        today.get(Calendar.DAY_OF_YEAR) == txDay.get(Calendar.DAY_OF_YEAR) -> "Today"
        today.get(Calendar.YEAR) == txDay.get(Calendar.YEAR) &&
        today.get(Calendar.DAY_OF_YEAR) - txDay.get(Calendar.DAY_OF_YEAR) == 1 -> "Yesterday"
        else -> SimpleDateFormat("MMM dd", Locale.getDefault()).format(Date(dateMs))
    }
}

@Composable
fun AtelierCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    OutlinedCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        content = content
    )
}
