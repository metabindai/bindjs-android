package ai.metabind.bindjs.composables.chart

// Accumulates each area series onto the running per-x total, returning new y-values where series
// i carries the sum of itself and all earlier series at each shared x. Series keep their original
// order; the caller reverses for back-to-front draw order. Kept free of Android dependencies so
// it can be unit-tested directly. Each entry is (xValues, yValues), aligned by index.
internal fun cumulativeStackedYValues(
    series: List<Pair<List<Double>, List<Double>>>,
): List<List<Double>> {
    val totals = linkedMapOf<Double, Double>()
    return series.map { (xValues, yValues) ->
        xValues.mapIndexed { index, x ->
            val total = totals.getOrDefault(x, 0.0) + yValues[index]
            totals[x] = total
            total
        }
    }
}
