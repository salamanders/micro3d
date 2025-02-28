package info.benjaminhill.micro3d

import kotlin.math.abs

/**
 * Handles GCodes (to and from)
 */
sealed class GCodeCommand<out T : Any>(val gcode: String) {
    open suspend fun result(port: EasyPort): T? {
        port.writeAndWait(gcode)
        return null
    }

    object FAN0 : GCodeCommand<Unit>(gcode = "M107")

    object FAN1 : GCodeCommand<Unit>(gcode = "M107 P1")

    object HOME : GCodeCommand<Unit>(gcode = "G28")

    object MOVE : GCodeCommand<Unit>(gcode = "")

    object POS : GCodeCommand<Point3D>(gcode = "M114") {
        override suspend fun result(port: EasyPort): Point3D {
            val positionString = port.writeAndWait("M114").first { it.contains("X:") }
            val positionRe = """X:([0-9.]+) Y:([0-9.]+) Z:([0-9.]+)""".toRegex()
            val (x, y, z) = positionRe.find(positionString)!!.groupValues.drop(1).map(String::toDouble)
            return Point3D(x, y, z)
        }
    }

    companion object {
        /**
         * Returns a string representation of the point in G-code format.
         *
         * @return A string in the format "G1 X%.2f Y%.2f Z%.2f".
         */
        fun Point3D.toGCode(): String {
            return "G1 X%.2f Y%.2f Z%.2f".format(x.roundToZeroIfClose(), y.roundToZeroIfClose(), z.roundToZeroIfClose())
        }

        const val EPSILON = 1e-4 // Define a small tolerance for near-zero values

        private fun Double.roundToZeroIfClose(): Double {
            return if (abs(this) < EPSILON) 0.0 else this
        }
    }
}
