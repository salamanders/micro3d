package info.benjaminhill.micro3d

import info.benjaminhill.micro3d.GCodeCommand.Companion.toGCode
import info.benjaminhill.micro3d.Paths.toUnitXY
import info.benjaminhill.micro3d.PrettyPrint.round
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import java.io.BufferedWriter
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.io.PrintWriter
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

// TODO: Can micro-stepping decrease this number?
//const val SMALLEST_XY: Double = 0.1
const val SMALLEST_Z: Double = 0.04

suspend fun main() {
    EasyCamera(0).use { cam ->
        cam.setResolution(640, 480)
        EasyPort.connect().use { port ->
            delay(2.seconds)
            val startingPoint = GCodeCommand.POS.result(port)

            cam.capture("focus_before")
            focus(startingPoint, cam = cam, port = port)
            cam.capture("focus_after")

            runApp(GCodeCommand.POS.result(port), cam = cam, port = port)
        }
    }
}

suspend fun focus(startPoint: Point3D, cam: EasyCamera, port: EasyPort) {
    var currentFocusPoint = startPoint.copy()
    println("Starting focus at point $currentFocusPoint")
    while (true) {
        val focusStack = (-2..2).associate { zStep ->
            val newLocation = currentFocusPoint.copy(z = startPoint.z + (SMALLEST_Z * zStep))
            port.writeAndWait(newLocation.toGCode())
            delay(250.milliseconds)
            newLocation to Focus.calculateLaplacianVariance(cam.capture())
        }

        val (improvedLocation, _) = focusStack.maxByOrNull { it.value } ?: break // Exit loop if no max found

        if (improvedLocation == currentFocusPoint) {
            println("Best focus found")
            port.writeAndWait(improvedLocation.toGCode())
            break
        }

        val direction = when {
            improvedLocation.z < currentFocusPoint.z -> "closer"
            else -> "farther"
        }
        println("Focusing $direction")
        currentFocusPoint = improvedLocation
    }
}


suspend fun runApp(startPoint: Point3D, cam: EasyCamera, port: EasyPort) = runBlocking {
    println("Currently at point $startPoint")
    val path = Paths.mooreCurve(2).toUnitXY().map { (x, y) ->
        Point3D(
            x = x.toDouble() + startPoint.x, y = y.toDouble() + startPoint.y, z = startPoint.z
        )
    }
    println("Min/Max X: ${path.minOfOrNull { it.x }}, ${path.maxOfOrNull { it.x }}")
    println("Min/Max Y: ${path.minOfOrNull { it.y }}, ${path.maxOfOrNull { it.y }}")

    PrintWriter(BufferedWriter(OutputStreamWriter(FileOutputStream("capture.pto")))).use { pto ->
        path.forEachIndexed { idx, point ->
            port.writeAndWait(point.toGCode())
            delay(500.milliseconds)
            val filename = "test_${idx.toString().padStart(4, '0')}_${point.x.round()}_${point.y.round()}.png"
            cam.capture(filename)
            pto.println("i w640 h480 f0 v29.97 n\"$filename\" TrX0 TrY0 y0 p0 r0")
        }
    }

}
