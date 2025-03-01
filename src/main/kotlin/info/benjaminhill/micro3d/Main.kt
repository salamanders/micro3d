package info.benjaminhill.micro3d

import info.benjaminhill.micro3d.GCodeCommand.Companion.toGCode
import info.benjaminhill.micro3d.Paths.toUnitXY
import info.benjaminhill.micro3d.PrettyPrint.round

// TODO: Can micro-stepping decrease this number?
//const val SMALLEST_XY: Double = 0.1
//const val SMALLEST_Z: Double = 0.04

suspend fun main() {
    EasyCamera().use { cam ->
        EasyPort.connect().use { port ->
            runApp(GCodeCommand.POS.result(port), cam, port)
        }
    }
}

suspend fun runApp(startPoint: Point3D, cam: EasyCamera, port: EasyPort) {
    println("Currently at point $startPoint")
    val path = Paths.mooreCurve().toUnitXY().map { (x, y) ->
        Point3D(
            x = x.toDouble() + startPoint.x, y = y.toDouble() + startPoint.y, z = startPoint.z
        )
    }
    println("Min/Max X: ${path.minOfOrNull { it.x }}, ${path.maxOfOrNull { it.x }}")
    println("Min/Max Y: ${path.minOfOrNull { it.y }}, ${path.maxOfOrNull { it.y }}")
    path.forEach { point ->
        port.writeAndWait(point.toGCode())
        cam.capture("test_${point.x.round()}_${point.y.round()}.png")
    }
}
