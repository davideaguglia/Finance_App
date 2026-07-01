package com.personal.financeapp.util

import java.util.Calendar

/** Helpers for month boundaries used across Dashboard, History and Reports. */
object MonthRange {

    /** Epoch-millis range `[start-of-month, end-of-month]` for the given month/year. */
    fun monthRange(month: Int, year: Int): Pair<Long, Long> {
        val cal = Calendar.getInstance()
        cal.clear()
        cal.set(year, month, 1, 0, 0, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val from = cal.timeInMillis
        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        cal.set(Calendar.MILLISECOND, 999)
        return from to cal.timeInMillis
    }

    fun currentMonth(): Int = Calendar.getInstance().get(Calendar.MONTH)
    fun currentYear(): Int = Calendar.getInstance().get(Calendar.YEAR)

    /** True when the given (month, year) is the current calendar month or later. */
    fun isCurrentOrFuture(month: Int, year: Int): Boolean {
        val now = Calendar.getInstance()
        val nowMonth = now.get(Calendar.MONTH)
        val nowYear = now.get(Calendar.YEAR)
        return year > nowYear || (year == nowYear && month >= nowMonth)
    }
}
