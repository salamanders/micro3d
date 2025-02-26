package info.benjaminhill.micro3d

import kotlin.math.abs
import kotlin.math.pow

data class Point3D(val x:Double, val y:Double, val z:Double) {
    override fun toString(): String {
        return "G1 X%.2f Y%.2f Z%.2f".format(x.roundToZeroIfClose(), y.roundToZeroIfClose(), z.roundToZeroIfClose())
    }

    fun moveXPos(distance: Double = SMALLEST_XY) = this.copy(x = this.x + distance)
    fun moveXNeg(distance: Double = SMALLEST_XY) = this.copy(x = this.x - distance)
    fun moveYPos(distance: Double = SMALLEST_XY) = this.copy(y = this.y + distance)
    fun moveYNeg(distance: Double = SMALLEST_XY) = this.copy(y = this.y - distance)

    fun moveZPos(distance: Double = SMALLEST_Z) = this.copy(z = this.z + distance)
    fun moveZNeg(distance: Double = SMALLEST_Z) = this.copy(z = this.z - distance)

    // Rotate counter-clockwise around the origin
    fun rotateCCW() = Point3D(this.y, -this.x, this.z)  // Correct: Swaps x and y, negates new x

    // Reflect and Translate for Q3, combines a reflection and a translation.
    fun reflectAndTranslate(size: Double): Point3D {
        return this.copy(x = size - SMALLEST_XY - this.y, y = 2 * size - SMALLEST_XY - this.x)
    }

    fun transformForQuadrant(quadrant: Int, size: Double): Point3D {
        return when (quadrant) {
            0 -> this.rotateCCW() // Q1: Rotate 90 degrees counter-clockwise.
            1 -> this.moveXPos(size) // Q2: Translate right by 'size'.
            2 -> this.moveXPos(size).moveYPos(size) // Q3: Translate right and up by 'size'.
            3 -> this.reflectAndTranslate(size) // Q4: Reflect and translate.
            else -> throw IllegalArgumentException("Invalid quadrant: $quadrant")
        }
    }

    fun generateHilbertCurve(order: Int): List<Point3D> {
        if (order <= 0) {
            return listOf(this) // Base case: Order 0 is just the starting point.
        }

        val size = 2.0.pow((order - 1).toDouble()) * SMALLEST_XY // Size of the sub-quadrant.
        val subCurve = this.generateHilbertCurve(order - 1) // Recursive call.

        // Map each point in the sub-curve to its corresponding position in each quadrant.
        val q1 = subCurve.map { it.transformForQuadrant(0, size) }
        val q2 = subCurve.map { it.transformForQuadrant(1, size) }
        val q3 = subCurve.map { it.transformForQuadrant(2, size) }
        val q4 = subCurve.map { it.transformForQuadrant(3, size) }

        return q1 + q2 + q3 + q4 // Concatenate the quadrants in the correct order.
    }



    companion object {
        const val SMALLEST_XY: Double = 0.1
        const val SMALLEST_Z: Double = 0.04

            const val EPSILON = 1e-6 // Define a small tolerance for near-zero values

        private fun Double.roundToZeroIfClose(): Double {
            return if (abs(this) < EPSILON) 0.0 else this
        }

        fun fromPosition(positionString:String) :Point3D {
            val positionRe = """X:([0-9.]+) Y:([0-9.]+) Z:([0-9.]+)""".toRegex()
            val (x, y, z) = positionRe.find(positionString)!!.groupValues.drop(1).map(String::toDouble)
            return Point3D(x, y, z)
        }
    }

}