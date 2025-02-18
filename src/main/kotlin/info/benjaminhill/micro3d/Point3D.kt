package info.benjaminhill.micro3d

import java.util.*


class Point3D() {

    private val coordinates: EnumMap<Axis, Double> = EnumMap<Axis, Double>(Axis::class.java).apply {
        Axis.entries.forEach { put(it, 0.0) }
    }

    operator fun get(axis: Axis): Double = coordinates[axis]!!

    operator fun get(axisName: String): Double = get(Axis.valueOf(axisName))

    operator fun set(axis: Axis, value: Double) {
        coordinates[axis] = value
    }

    operator fun set(axisName: String, value: Double) = set(Axis.valueOf(axisName), value)

    operator fun plusAssign(other: Pair<String, Double>) {
        val (axisName, offset) = other
        val axis = Axis.valueOf(axisName)
        set(axis, get(axis) + offset)
    }

    override fun toString(): String =
        "Point3D(x=${coordinates[Axis.X]}, y=${coordinates[Axis.Y]}, z=${coordinates[Axis.Z]})"

    companion object {
        enum class Axis { X, Y, Z }
    }
}