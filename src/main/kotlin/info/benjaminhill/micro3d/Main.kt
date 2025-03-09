package info.benjaminhill.micro3d

import info.benjaminhill.micro3d.GCodeCommand.Companion.SMALLEST_XY
import info.benjaminhill.micro3d.GCodeCommand.Companion.SMALLEST_Z
import info.benjaminhill.micro3d.Paths.toUnitXY
import info.benjaminhill.micro3d.PrettyPrint.round
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import java.io.BufferedWriter
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.io.PrintWriter
import java.nio.file.Path
import java.util.*
import kotlin.io.path.createDirectories
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

val exportDir: Path = Path.of("./output").also { it.createDirectories() }

suspend fun main() {
    EasyCamera(0).use { cam ->
        cam.setResolution(640, 480)
        EasyPort.connect().use { port ->
            delay(5.seconds)
            println("Starting commands")
            val commands = GCodeCommand(port)
            val app = MicroscopeCamera(cam, commands)
            println("Starting command loop")
            while (true) {
                print("> ")
                val cmd = readlnOrNull() ?: continue
                when (cmd) {
                    "exit" -> break
                    "focus" -> app.focus()
                    "grid" -> app.captureGrid()
                    else -> commands.invokeUsingName(cmd)
                }
            }
        }
    }
}

class MicroscopeCamera(private val cam: EasyCamera, private val commands: GCodeCommand) {


    suspend fun focus(): Point3D {
        val sortedFocusSamples: NavigableMap<Point3D, Double> = TreeMap { p1, p2 ->
            p1.z.compareTo(p2.z)
        }

        suspend fun addLocationToStack(newPoint: Point3D) {
            sortedFocusSamples.getOrPut(newPoint) {
                commands.jump(newPoint)
                delay(750.milliseconds)
                val newFocus = Focus.calculateLaplacianVariance(cam.capture())
                cam.capture(exportDir.resolve("focus_${newPoint.z.round(2)}_${newFocus.toInt()}").toString())
                println("Added new focus point to stack: ${newPoint.z} = $newFocus")
                newFocus
            }
        }

        val startPoint = commands.getLocation()
        // Seed the focus stack
        (-5..5).forEach {
            addLocationToStack(startPoint.copy(z = startPoint.z + (SMALLEST_Z * it)))
        }

        while (true) {
            val bestFocus = sortedFocusSamples.toList().maxBy { it.second }
            when (bestFocus.first) {
                sortedFocusSamples.firstKey() -> addLocationToStack(
                    sortedFocusSamples.firstKey().copy(z = sortedFocusSamples.firstKey().z - SMALLEST_Z)
                )

                sortedFocusSamples.lastKey() -> addLocationToStack(
                    sortedFocusSamples.lastKey().copy(z = sortedFocusSamples.lastKey().z + SMALLEST_Z)
                )

                else -> {
                    println("Best focus found: ${bestFocus.first.z.round(2)} with focus val ${bestFocus.second.round(2)}")
                    commands.jump(bestFocus.first)
                    return bestFocus.first
                }
            }
        }
    }

    suspend fun captureGrid() = runBlocking {
        val startPoint = commands.getLocation()
        println("Currently at point $startPoint")
        val path = (listOf(0 to 0) + Paths.mooreCurve(3).toUnitXY())
            .map { (x, y) ->
            Point3D(
                x = x.toDouble() * SMALLEST_XY * 2 + startPoint.x,
                y = y.toDouble() * SMALLEST_XY * 2 + startPoint.y,
                z = startPoint.z,
            )
        }
        println("Min/Max X: ${path.minOfOrNull { it.x }}, ${path.maxOfOrNull { it.x }}")
        println("Min/Max Y: ${path.minOfOrNull { it.y }}, ${path.maxOfOrNull { it.y }}")

        PrintWriter(BufferedWriter(OutputStreamWriter(FileOutputStream(
            exportDir.resolve("capture.pto").toFile())))).use { pto ->
            path.forEachIndexed { idx, point ->
                commands.jump(point)
                delay(500.milliseconds)
                val filename = "test_${idx.toString().padStart(4, '0')}_${point.x.round()}_${point.y.round()}.png"
                cam.capture(exportDir.resolve(filename).toString())
                pto.println("i w640 h480 f0 v29.97 n\"$filename\" TrX0 TrY0 y0 p0 r0")
            }
        }
    }

}





