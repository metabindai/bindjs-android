package ai.metabind.bindjs.composables.chart

import ai.metabind.bindjs.model.chart.ChartValueFormatter
import java.text.DateFormat
import java.text.NumberFormat
import java.util.Currency
import java.util.Date
import java.util.Locale

// Renders an axis tick value through the declarative formatter parsed from `.chartXAxis`/
// `.chartYAxis({ formatter })`, mirroring SwiftUI's `AxisMarks(format:)`. Kept free of Android
// dependencies so it can be unit-tested directly.
internal fun ChartValueFormatter.format(value: Double, locale: Locale = Locale.getDefault()): String =
    when (this) {
        is ChartValueFormatter.NumberFormatter ->
            NumberFormat.getNumberInstance(locale)
                .applyFractionDigits(minimumFractionDigits, maximumFractionDigits)
                .format(value)

        is ChartValueFormatter.PercentFormatter ->
            // Matches Swift's `.percent` style, which scales the fractional value by 100.
            NumberFormat.getPercentInstance(locale)
                .applyFractionDigits(minimumFractionDigits, maximumFractionDigits)
                .format(value)

        is ChartValueFormatter.CurrencyFormatter ->
            NumberFormat.getCurrencyInstance(locale)
                .also { format ->
                    runCatching { format.currency = Currency.getInstance(currency) }
                }
                .applyFractionDigits(minimumFractionDigits, maximumFractionDigits)
                .format(value)

        is ChartValueFormatter.DateFormatter ->
            // Date axis values arrive as epoch milliseconds (JS `Date.getTime()`).
            dateFormat(dateStyle, timeStyle, locale).format(Date(value.toLong()))
    }

// Setting max below the format's default min (or vice versa) makes NumberFormat reconcile the
// other bound automatically, so an explicit `maximumFractionDigits: 0` drops currency cents.
private fun NumberFormat.applyFractionDigits(min: Int?, max: Int?): NumberFormat {
    min?.let { minimumFractionDigits = it }
    max?.let { maximumFractionDigits = it }
    return this
}

private fun dateFormat(dateStyle: String?, timeStyle: String?, locale: Locale): DateFormat {
    val date = dateStyleConstant(dateStyle)
    val time = dateStyleConstant(timeStyle)
    return when {
        date != null && time != null -> DateFormat.getDateTimeInstance(date, time, locale)
        time != null -> DateFormat.getTimeInstance(time, locale)
        date != null -> DateFormat.getDateInstance(date, locale)
        else -> DateFormat.getDateInstance(DateFormat.MEDIUM, locale)
    }
}

private fun dateStyleConstant(style: String?): Int? =
    when (style) {
        "short" -> DateFormat.SHORT
        "medium" -> DateFormat.MEDIUM
        "long" -> DateFormat.LONG
        "full" -> DateFormat.FULL
        "none", null -> null
        else -> DateFormat.MEDIUM
    }
