package com.example.ui.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object DateTimeUtils {

    private val fullDateFormat = SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault())
    private val monthYearFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
    private val shortDateFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())

    fun formatTimelineHeader(epochMillis: Long): String {
        val itemCal = Calendar.getInstance().apply { timeInMillis = epochMillis }
        val nowCal = Calendar.getInstance()

        val isSameDay = itemCal.get(Calendar.YEAR) == nowCal.get(Calendar.YEAR) &&
                itemCal.get(Calendar.DAY_OF_YEAR) == nowCal.get(Calendar.DAY_OF_YEAR)

        if (isSameDay) return "Today"

        val yesterdayCal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
        val isYesterday = itemCal.get(Calendar.YEAR) == yesterdayCal.get(Calendar.YEAR) &&
                itemCal.get(Calendar.DAY_OF_YEAR) == yesterdayCal.get(Calendar.DAY_OF_YEAR)

        if (isYesterday) return "Yesterday"

        // If in same year and month
        return monthYearFormat.format(Date(epochMillis))
    }

    fun formatDateGroupKey(epochMillis: Long): String {
        return monthYearFormat.format(Date(epochMillis))
    }

    fun formatFullDateTime(epochMillis: Long): String {
        val date = Date(epochMillis)
        return "${fullDateFormat.format(date)} at ${timeFormat.format(date)}"
    }

    fun formatShortDate(epochMillis: Long): String {
        return shortDateFormat.format(Date(epochMillis))
    }

    fun formatVideoDuration(seconds: Int): String {
        val minutes = seconds / 60
        val remainingSeconds = seconds % 60
        return String.format(Locale.getDefault(), "%d:%02d", minutes, remainingSeconds)
    }
}
