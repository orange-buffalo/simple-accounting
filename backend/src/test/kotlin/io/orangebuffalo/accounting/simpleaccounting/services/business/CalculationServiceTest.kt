package io.orangebuffalo.accounting.simpleaccounting.services.business

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

internal class CalculationServiceTest {

    @ParameterizedTest
    @MethodSource("percentPartTestData")
    fun testPercentPartCalculation(testData: PercentPartData) {
        assertThat(testData.input.percentPart(testData.percent)).isEqualTo(testData.expectedOutput)
    }

    @ParameterizedTest
    @MethodSource("bpsPartTestData")
    fun testBpsPartCalculation(testData: BpsPartData) {
        assertThat(testData.input.bpsPart(testData.bps)).isEqualTo(testData.expectedOutput)
    }

    @ParameterizedTest
    @MethodSource("bpsBasePartTestData")
    fun testBpsBasePartCalculation(testData: BpsPartData) {
        assertThat(testData.input.bpsBasePart(testData.bps)).isEqualTo(testData.expectedOutput)
    }

    @Suppress("unused")
    companion object {
        @JvmStatic
        fun percentPartTestData(): Stream<PercentPartData> = Stream.of(
            PercentPartData(34132, 100, 34132),
            PercentPartData(34132, 0, 0),
            PercentPartData(34132, 1, 341),
            PercentPartData(45, 10, 5),
            PercentPartData(34132, 50, 17066),
            // 2389.24 -> round down
            PercentPartData(34132, 7, 2389),
            // 2730.56 -> round up
            PercentPartData(34132, 8, 2731)
        )

        @JvmStatic
        fun bpsPartTestData(): Stream<BpsPartData> = Stream.of(
            BpsPartData(34132, 100_00, 34132),
            BpsPartData(34132, 0, 0),
            BpsPartData(34132, 1, 3),
            BpsPartData(45, 10_00, 5),
            BpsPartData(34132, 50_00, 17066),
            // 2389.24 -> round down
            BpsPartData(34132, 7_00, 2389),
            // 2730.56 -> round up
            BpsPartData(34132, 8_00, 2731),
            // 2952.418 -> round down
            BpsPartData(34132, 8_65, 2952),
            // 1938.6976 -> round up
            BpsPartData(34132, 5_68, 1939)
        )

        @JvmStatic
        fun bpsBasePartTestData(): Stream<BpsPartData> = Stream.of(
            BpsPartData(0, 100_00, 0),
            BpsPartData(0, 0, 0),
            // 6818.181818182 -> round down
            BpsPartData(7500, 10_00, 6818),
            BpsPartData(7500, 50_00, 5000),
            // 7498.50029994 -> round up
            BpsPartData(7500, 2, 7499)
        )
    }
}

internal data class PercentPartData(
    val input: Long,
    val percent: Int,
    val expectedOutput: Long
)

internal data class BpsPartData(
    val input: Long,
    val bps: Int,
    val expectedOutput: Long
)