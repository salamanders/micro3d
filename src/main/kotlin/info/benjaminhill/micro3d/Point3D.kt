package info.benjaminhill.micro3d

import kotlin.math.abs

/**
 * Represents a point in 3D space with x, y, and z coordinates.
 *
 * @property x The x-coordinate of the point.
 * @property y The y-coordinate of the point.
 * @property z The z-coordinate of the point.
 */
data class Point3D(val x: Double, val y: Double, val z: Double) {

    /**
     * Returns a string representation of the point in G-code format.
     *
     * @return A string in the format "G1 X%.2f Y%.2f Z%.2f".
     */
    override fun toString(): String {
        return "G1 X%.2f Y%.2f Z%.2f".format(x.roundToZeroIfClose(), y.roundToZeroIfClose(), z.roundToZeroIfClose())
    }

    companion object {
        const val EPSILON = 1e-4 // Define a small tolerance for near-zero values

        private fun Double.roundToZeroIfClose(): Double {
            return if (abs(this) < EPSILON) 0.0 else this
        }
    }

}