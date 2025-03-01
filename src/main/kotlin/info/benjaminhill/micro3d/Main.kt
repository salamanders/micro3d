package info.benjaminhill.micro3d

import info.benjaminhill.micro3d.GCodeCommand.Companion.toGCode
import info.benjaminhill.micro3d.Paths.toUnitXY
import info.benjaminhill.micro3d.PrettyPrint.round
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.BufferedWriter
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.io.PrintWriter
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

// TODO: Can micro-stepping decrease this number?
//const val SMALLEST_XY: Double = 0.1
//const val SMALLEST_Z: Double = 0.04

suspend fun main() {
    EasyCamera(0).use { cam ->
        cam.setResolution(640, 480)
        EasyPort.connect().use { port ->
            delay(2.seconds)
            runApp(GCodeCommand.POS.result(port), cam = cam, port = port)
        }
    }
}

suspend fun runApp(startPoint: Point3D, cam: EasyCamera? = null, port: EasyPort? = null) {
    println("Currently at point $startPoint")
    val path = Paths.mooreCurve().toUnitXY().map { (x, y) ->
        Point3D(
            x = x.toDouble() + startPoint.x, y = y.toDouble() + startPoint.y, z = startPoint.z
        )
    }
    println("Min/Max X: ${path.minOfOrNull { it.x }}, ${path.maxOfOrNull { it.x }}")
    println("Min/Max Y: ${path.minOfOrNull { it.y }}, ${path.maxOfOrNull { it.y }}")

    withContext(Dispatchers.IO) {
        PrintWriter(BufferedWriter(OutputStreamWriter(FileOutputStream("capture.pto")))).use { pto ->
            path.forEach { point ->
                port?.writeAndWait(point.toGCode())
                delay(500.milliseconds)
                val filename = "test_${point.x.round()}_${point.y.round()}.png"
                cam?.capture(filename)
                pto.println("i w640 h480 f0 v29.97 n\"$filename\" TrX${point.x.round()} TrY${point.y.round()} y0 p0 r0")
            }
        }
    }


}
