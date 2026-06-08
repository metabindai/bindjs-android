package ai.metabind.bindjs.composables.chart

import ai.metabind.bindjs.model.chart.ChartValueFormatter
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale

class ChartValueFormatterRenderTest {
    private val us = Locale.US

    @Test
    fun currencyFormatterDropsFractionDigits() {
        val formatter = ChartValueFormatter.CurrencyFormatter(currency = "USD", maximumFractionDigits = 0)
        assertEquals("$1,200", formatter.format(1200.0, us))
        assertEquals("$1,800", formatter.format(1800.0, us))
    }

    @Test
    fun currencyFormatterKeepsDefaultCentsWhenUnspecified() {
        val formatter = ChartValueFormatter.CurrencyFormatter(currency = "USD")
        assertEquals("$1,200.00", formatter.format(1200.0, us))
    }

    @Test
    fun numberFormatterRespectsFractionDigits() {
        val formatter = ChartValueFormatter.NumberFormatter(maximumFractionDigits = 0)
        assertEquals("1,235", formatter.format(1234.6, us))
    }

    @Test
    fun percentFormatterScalesByHundred() {
        val formatter = ChartValueFormatter.PercentFormatter(maximumFractionDigits = 0)
        assertEquals("50%", formatter.format(0.5, us))
    }

    @Test
    fun dateFormatterUsesEpochMilliseconds() {
        // 2021-01-01T00:00:00Z = 1_609_459_200_000 ms. Assert via the same DateFormat to stay
        // timezone-independent rather than hard-coding a rendered string.
        val formatter = ChartValueFormatter.DateFormatter(dateStyle = "medium")
        val expected = java.text.DateFormat
            .getDateInstance(java.text.DateFormat.MEDIUM, us)
            .format(java.util.Date(1_609_459_200_000L))
        assertEquals(expected, formatter.format(1_609_459_200_000.0, us))
    }
}
