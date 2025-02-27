package info.benjaminhill.micro3d

import info.benjaminhill.micro3d.Paths.toUnitXY

const val SMALLEST_XY: Double = 0.1
const val SMALLEST_Z: Double = 0.04

suspend fun main() {
    EasyCamera().use { cam ->
        cam.capture("test")
    }

    EasyPort.connect().use { port ->
        val posStr = port.writeAndWait("M114").first { it.contains("X:") }
        val startingPoint = Point3D.fromPosition(posStr)

        Paths.mooreCurve().toUnitXY()
            .map { (x, y) ->
                Point3D(
                    x = x.toDouble() + startingPoint.x,
                    y = y.toDouble() + startingPoint.y,
                    z = startingPoint.z
                )
            }

            .forEach { point ->
                port.writeAndWait(point.toString())
            }

    }
}
