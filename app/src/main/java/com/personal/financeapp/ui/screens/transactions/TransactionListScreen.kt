package com.personal.financeapp.ui.screens.transactions

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.personal.financeapp.data.local.dao.TransactionWithDetails
import com.personal.financeapp.ui.components.MonthSelector
import com.personal.financeapp.ui.theme.Forest
import com.personal.financeapp.ui.theme.IncomeGreen
import com.personal.financeapp.ui.theme.Terra
import com.personal.financeapp.util.CurrencyFormatter
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionListScreen(
    onAddTransaction: () -> Unit,
    onEditTransaction: (Long) -> Unit,
    viewModel: TransactionViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var searchVisible by remember { mutableStateOf(false) }
    var selectedIds by remember { mutableStateOf(emptySet<Long>()) }
    val isSelecting = selectedIds.isNotEmpty()

    val groupedTx = remember(state.transactions) {
        state.transactions
            .groupBy { txDateLabel(it.transaction.date) }
            .entries.toList()
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            if (!isSelecting) {
                FloatingActionButton(
                    onClick = onAddTransaction,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    elevation = FloatingActionButtonDefaults.elevation(8.dp)
                ) { Icon(Icons.Default.Add, "Add") }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = 88.dp)
        ) {
            // ── Header ─────────────────────────────────────────────
            item {
                if (isSelecting) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.background)
                            .padding(start = 4.dp, end = 8.dp, top = 4.dp, bottom = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { selectedIds = emptySet() }) {
                            Icon(Icons.Default.Close, "Cancel selection",
                                tint = MaterialTheme.colorScheme.onSurface)
                        }
                        Text(
                            "${selectedIds.size} selected",
                            style = MaterialTheme.typography.headlineMedium,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = {
                            val toDelete = state.transactions.filter { it.transaction.id in selectedIds }
                            toDelete.forEach { viewModel.delete(it.transaction) }
                            selectedIds = emptySet()
                            scope.launch {
                                val result = snackbarHostState.showSnackbar(
                                    message = "${toDelete.size} transaction${if (toDelete.size == 1) "" else "s"} deleted",
                                    actionLabel = "Undo",
                                    duration = SnackbarDuration.Short
                                )
                                if (result == SnackbarResult.ActionPerformed) {
                                    toDelete.forEach { viewModel.insert(it.transaction) }
                                }
                            }
                        }) {
                            Icon(Icons.Default.Delete, "Delete selected",
                                tint = MaterialTheme.colorScheme.error)
                        }
                    }
                } else {
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
                                "LEDGER",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(2.dp))
                            Text("Transactions", style = MaterialTheme.typography.headlineLarge)
                        }
                        IconButton(onClick = { searchVisible = !searchVisible }) {
                            Icon(
                                Icons.Default.Search, "Search",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // ── Month navigator ──────────────────────────────────
            if (!isSelecting) {
                item {
                    Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                        MonthSelector(
                            selectedMonth = state.selectedMonth,
                            selectedYear = state.selectedYear,
                            onMonthChange = { m, y -> viewModel.selectMonth(m, y) }
                        )
                    }
                }
            }

            // ── Search field ───────────────────────────────────────
            item {
                AnimatedVisibility(searchVisible && !isSelecting) {
                    OutlinedTextField(
                        value = state.searchQuery,
                        onValueChange = { viewModel.setSearch(it) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        placeholder = { Text("Search transactions in this month…") },
                        singleLine = true,
                        shape = RoundedCornerShape(100.dp),
                        leadingIcon = { Icon(Icons.Default.Search, null, modifier = Modifier.size(18.dp)) },
                        trailingIcon = {
                            if (state.searchQuery.isNotEmpty()) {
                                IconButton(onClick = { viewModel.setSearch("") }) {
                                    Icon(Icons.Default.Close, "Clear", modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    )
                }
            }

            // ── Filter pills ───────────────────────────────────────
            item {
                AnimatedVisibility(!isSelecting) {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(top = 8.dp, bottom = 8.dp)
                    ) {
                        val filters = listOf(null to "All", "INCOME" to "Income", "EXPENSE" to "Expenses")
                        items(filters) { (type, label) ->
                            val selected = state.filterType == type
                            FilterChip(
                                selected = selected,
                                onClick = { viewModel.setFilter(type) },
                                label = { Text(label) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            )
                        }
                    }
                }
            }

            // ── Net flow summary strip ────────────────────────────────
            if (state.transactions.isNotEmpty() && !isSelecting) {
                item {
                    val income = state.transactions.filter { it.transaction.type == "INCOME" }
                        .sumOf { it.transaction.amount }
                    val expense = state.transactions.filter { it.transaction.type == "EXPENSE" }
                        .sumOf { it.transaction.amount }
                    val net = income - expense
                    val monthLabel = remember(state.selectedMonth, state.selectedYear) {
                        val cal = Calendar.getInstance().apply { set(state.selectedYear, state.selectedMonth, 1) }
                        SimpleDateFormat("MMMM", Locale.getDefault()).format(cal.time)
                    }
                    OutlinedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "NET FLOW · ${monthLabel.uppercase()}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    CurrencyFormatter.formatWithSign(net, net >= 0),
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = if (net >= 0) Forest else MaterialTheme.colorScheme.error
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    "In  ${CurrencyFormatter.format(income)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Forest
                                )
                                Text(
                                    "Out ${CurrencyFormatter.format(expense)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Terra
                                )
                            }
                        }
                    }
                }
            }

            if (state.transactions.isEmpty()) {
                item {
                    val emptyMonthLabel = remember(state.selectedMonth, state.selectedYear) {
                        val cal = Calendar.getInstance().apply { set(state.selectedYear, state.selectedMonth, 1) }
                        SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(cal.time)
                    }
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(top = 64.dp, start = 24.dp, end = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "No transactions in $emptyMonthLabel",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                groupedTx.forEachIndexed { groupIndex, (dateStr, txList) ->
                    item(key = "date_$dateStr") {
                        Column(
                            modifier = Modifier.padding(
                                start = 16.dp, end = 16.dp,
                                top = if (groupIndex == 0) 4.dp else 16.dp,
                                bottom = 6.dp
                            )
                        ) {
                            Text(
                                text = dateStr.uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(start = 2.dp, bottom = 6.dp)
                            )
                            OutlinedCard(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                colors = CardDefaults.outlinedCardColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                )
                            ) {
                                txList.forEachIndexed { i, item ->
                                    if (isSelecting) {
                                        TransactionItem(
                                            item = item,
                                            isSelected = item.transaction.id in selectedIds,
                                            onClick = {
                                                selectedIds = if (item.transaction.id in selectedIds)
                                                    selectedIds - item.transaction.id
                                                else
                                                    selectedIds + item.transaction.id
                                            },
                                            onLongClick = {}
                                        )
                                    } else {
                                        SwipeToDismissTransactionItem(
                                            item = item,
                                            onEdit = { onEditTransaction(item.transaction.id) },
                                            onDelete = {
                                                viewModel.delete(item.transaction)
                                                scope.launch {
                                                    val result = snackbarHostState.showSnackbar(
                                                        message = "Transaction deleted",
                                                        actionLabel = "Undo",
                                                        duration = SnackbarDuration.Short
                                                    )
                                                    if (result == SnackbarResult.ActionPerformed) {
                                                        viewModel.insert(item.transaction)
                                                    }
                                                }
                                            },
                                            onLongClick = { selectedIds = setOf(item.transaction.id) }
                                        )
                                    }
                                    if (i != txList.lastIndex) HorizontalDivider(
                                        color = MaterialTheme.colorScheme.outlineVariant,
                                        thickness = 0.5.dp,
                                        modifier = Modifier.padding(horizontal = 14.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeToDismissTransactionItem(
    item: TransactionWithDetails,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onLongClick: () -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) { onDelete(); true } else false
        }
    )
    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.errorContainer),
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(end = 20.dp)
                )
            }
        }
    ) {
        TransactionItem(item = item, isSelected = false, onClick = onEdit, onLongClick = onLongClick)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TransactionItem(
    item: TransactionWithDetails,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val tx = item.transaction
    val isIncome = tx.type == "INCOME"
    val dateStr = SimpleDateFormat("MMM dd", Locale.getDefault()).format(Date(tx.date))

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                else MaterialTheme.colorScheme.surface
            )
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(horizontal = 14.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .background(
                    when {
                        isSelected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                        isIncome   -> Forest.copy(alpha = 0.12f)
                        else       -> Terra.copy(alpha = 0.12f)
                    },
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isSelected) {
                Icon(Icons.Default.Check, null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp))
            } else {
                Icon(
                    if (isIncome) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                    contentDescription = null,
                    tint = if (isIncome) IncomeGreen else Terra,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                item.category?.name ?: "Uncategorized",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 1
            )
            Text(
                buildString {
                    if (tx.description.isNotBlank()) { append(tx.description); append(" · ") }
                    append(item.account?.name ?: "—")
                    append(" · ")
                    append(dateStr)
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                CurrencyFormatter.formatWithSign(tx.amount, isIncome),
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = if (isIncome) IncomeGreen else MaterialTheme.colorScheme.onSurface
            )
            if (tx.isRecurring) {
                Text(
                    "↻ recurring",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
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
