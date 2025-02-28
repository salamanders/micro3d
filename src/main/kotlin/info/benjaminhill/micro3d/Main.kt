package info.benjaminhill.micro3d

import info.benjaminhill.micro3d.Paths.toUnitXY

const val SMALLEST_XY: Double = 0.1
const val SMALLEST_Z: Double = 0.04

suspend fun main() {
    EasyCamera().use { cam ->
        EasyPort.connect().use { port ->
            val startingPoint: Point3D = GCodeCommand.POS.result(port)
            println("Currently at point $startingPoint")

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
                    cam.capture("test_${point.x}_${point.y}")
                }
        }

    }
}
