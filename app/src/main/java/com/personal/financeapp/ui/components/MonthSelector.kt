package com.personal.financeapp.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.personal.financeapp.util.MonthRange
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/** Chevron-left / month-label / chevron-right selector. Disables next when at current month. */
@Composable
fun MonthSelector(
    selectedMonth: Int,
    selectedYear: Int,
    onMonthChange: (Int, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val monthFmt = remember { SimpleDateFormat("MMMM yyyy", Locale.getDefault()) }
    val label = remember(selectedMonth, selectedYear) {
        val cal = Calendar.getInstance().apply { set(selectedYear, selectedMonth, 1) }
        monthFmt.format(cal.time)
    }
    val isCurrentOrFuture = MonthRange.isCurrentOrFuture(selectedMonth, selectedYear)

    OutlinedCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = {
                val prev = Calendar.getInstance().apply { set(selectedYear, selectedMonth, 1) }
                prev.add(Calendar.MONTH, -1)
                onMonthChange(prev.get(Calendar.MONTH), prev.get(Calendar.YEAR))
            }) {
                Icon(Icons.Default.ChevronLeft, contentDescription = "Previous month")
            }
            Text(
                label,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            IconButton(
                onClick = {
                    val next = Calendar.getInstance().apply { set(selectedYear, selectedMonth, 1) }
                    next.add(Calendar.MONTH, 1)
                    onMonthChange(next.get(Calendar.MONTH), next.get(Calendar.YEAR))
                },
                enabled = !isCurrentOrFuture
            ) {
                Icon(Icons.Default.ChevronRight, contentDescription = "Next month")
            }
        }
    }
}
