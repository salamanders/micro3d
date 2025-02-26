package info.benjaminhill.micro3d

import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.abs
import kotlin.math.pow

data class Point3D(val x: Double, val y: Double, val z: Double) {
    override fun toString(): String {
        return "G1 X%.2f Y%.2f Z%.2f".format(x.roundToZeroIfClose(), y.roundToZeroIfClose(), z.roundToZeroIfClose())
    }

    fun moveXPos(distance: Double = SMALLEST_XY) = this.copy(x = this.x + distance)
    fun moveXNeg(distance: Double = SMALLEST_XY) = this.copy(x = this.x - distance)
    fun moveYPos(distance: Double = SMALLEST_XY) = this.copy(y = this.y + distance)
    fun moveYNeg(distance: Double = SMALLEST_XY) = this.copy(y = this.y - distance)
    fun moveZPos(distance: Double = SMALLEST_Z) = this.copy(z = this.z + distance)
    fun moveZNeg(distance: Double = SMALLEST_Z) = this.copy(z = this.z - distance)

    companion object {
        const val SMALLEST_XY: Double = 0.1
        const val SMALLEST_Z: Double = 0.04
        const val EPSILON = 1e-6 // Define a small tolerance for near-zero values

        private fun Double.roundToZeroIfClose(): Double {
            return if (abs(this) < EPSILON) 0.0 else (this*100)
        }

        fun fromPosition(positionString: String): Point3D {
            val positionRe = """X:([0-9.]+) Y:([0-9.]+) Z:([0-9.]+)""".toRegex()
            val (x, y, z) = positionRe.find(positionString)!!.groupValues.drop(1).map(String::toDouble)
            return Point3D(x, y, z)
        }

        fun Double.round(decimals: Int=2): Double =
            BigDecimal(this).setScale(decimals, RoundingMode.HALF_EVEN).toDouble()
    }

}