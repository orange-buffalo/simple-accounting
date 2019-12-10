package io.orangebuffalo.simpleaccounting.services.business

import java.math.RoundingMode

private val hundredPercent = 100.toBigDecimal()

fun Long.percentPart(percent: Int): Long =
    this.toBigDecimal()
        .multiply(percent.toBigDecimal())
        .divide(hundredPercent, 0, RoundingMode.HALF_UP)
        .longValueExact()

private val hundredPercentInBps = 100_00.toBigDecimal()

fun Long.bpsPart(bps: Int): Long =
    this.toBigDecimal()
        .multiply(bps.toBigDecimal())
        .divide(hundredPercentInBps, 0, RoundingMode.HALF_UP)
        .longValueExact()

fun Long.bpsBasePart(bps: Int): Long =
    this.toBigDecimal()
        .multiply(hundredPercentInBps)
        .divide(hundredPercentInBps + bps.toBigDecimal(), 0, RoundingMode.HALF_UP)
        .longValueExact()
