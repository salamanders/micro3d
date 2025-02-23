package info.benjaminhill.micro3d

import java.util.*

class Point3D(x: Double = 0.0, y: Double = 0.0, z: Double = 0.0) {

    private val coordinates: EnumMap<Axis, Double> = EnumMap<Axis, Double>(Axis::class.java)

    init {
        coordinates[Axis.X] = x
        coordinates[Axis.Y] = y
        coordinates[Axis.Z] = z
    }

    operator fun get(axis: Axis): Double = coordinates.getValue(axis)

    operator fun get(axisName: String): Double = get(Axis.valueOf(axisName))

    operator fun set(axis: Axis, value: Double) {
        coordinates[axis] = value
    }

    operator fun set(axisName: String, value: Double) = set(Axis.valueOf(axisName), value)

    /** Number is the offset from the current location, provided to the printer as an absolute location */
    operator fun plusAssign(other: Pair<Axis, Double>) {
        val (axis, offset) = other
        set(axis, get(axis) + offset)
    }
    override fun toString(): String {
        // G1 for a precision move
        return "G1 " + Axis.entries.joinToString(" ") { "%s%.2f".format(it.name, coordinates[it]) }
    }
    companion object {
        enum class Axis { X, Y, Z }
    }
}