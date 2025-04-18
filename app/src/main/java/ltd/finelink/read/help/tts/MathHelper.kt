package ltd.finelink.read.help.tts

import androidx.annotation.Keep
import java.math.BigDecimal
import java.math.RoundingMode

@Keep
internal object MathHelper {
    fun LeastCommonMultiple(a: Int, b: Int): Int {
        return a / GreatestCommonDivisor(a, b) * b
    }

    fun GreatestCommonDivisor(a: Int, b: Int): Int {
        var a = a
        var b = b
        while (b != 0) {
            val temp = b
            b = a % b
            a = temp
        }
        return a
    }

    fun InterpolateLinear(a: Int, b: Int, distance: Double): Int {
        if (distance <= 0) {
            return a
        } else if (distance >= 1) {
            return b
        }
        return Math.round(a + (b - a) * distance).toInt()
    }

    fun Round(value: Double, decimalPlaces: Int): Double {
        require(!(decimalPlaces < 0))
        var bdValue = BigDecimal(value)
        bdValue = bdValue.setScale(decimalPlaces, RoundingMode.HALF_UP)
        return bdValue.toDouble()
    }
}
