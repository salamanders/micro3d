package info.benjaminhill.micro3d

import info.benjaminhill.micro3d.ConsolePrettyPrint.round
import info.benjaminhill.micro3d.GCode.Companion.SMALLEST_XY
import info.benjaminhill.micro3d.GCode.Companion.SMALLEST_Z
import info.benjaminhill.micro3d.Paths.toUnitXY
import kotlinx.coroutines.runBlocking
import java.io.BufferedWriter
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.io.PrintWriter
import java.util.*


class MicroscopeCamera(private val printer: GCode) {
    suspend fun invokeUsingName(command: Char) {
        printer.invokeUsingName(command)
    }

    suspend fun focus(): Point3D {
        EasyCamera(0).use { cam ->

            val sortedFocusSamples: NavigableMap<Point3D, Double> = TreeMap { p1, p2 ->
                p1.z.compareTo(p2.z)
            }

            suspend fun addLocationToStack(newPoint: Point3D) {
                sortedFocusSamples.getOrPut(newPoint) {
                    printer.jump(newPoint)
                    val newFocus = ImageUtils.calculateLaplacianVariance(cam.capture())
                    val imageName = "focus_${newPoint.z.round(2)}_${newFocus.toInt()}"
                    cam.captureToFile(exportDir.resolve(imageName).toString())
                    println("Added new focus point to stack: ${newPoint.z} = $newFocus")
                    newFocus
                }
            }

            val startPoint = printer.getLocation()
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
                        println(
                            "Best focus found: ${bestFocus.first.z.round(2)} with focus val ${
                                bestFocus.second.round(
                                    2
                                )
                            }"
                        )
                        printer.jump(bestFocus.first)
                        return bestFocus.first
                    }
                }
            }
        }
    }

    suspend fun captureStack() = runBlocking {
        val startPoint = printer.getLocation()
        println("Currently at point $startPoint")
        EasyCamera(0).use { cam ->
            (0..200).forEach { stack ->
                val stackPoint = startPoint.copy(z = startPoint.z + (SMALLEST_Z * stack))
                printer.jump(stackPoint)
                val filename = "stack_${stack.toString().padStart(4, '0')}.png"
                cam.captureToFile(exportDir.resolve(filename).toString())
            }
        }
    }


    suspend fun captureGrid() = runBlocking {
        val startPoint = printer.getLocation()

        println("Currently at point $startPoint")
        val path = (listOf(0 to 0) + Paths.mooreCurve(2).toUnitXY())
            .map { (x, y) ->
                Point3D(
                    x = x.toDouble() * SMALLEST_XY * 2 + startPoint.x,
                    y = y.toDouble() * SMALLEST_XY * 2 + startPoint.y,
                    z = startPoint.z,
                )
            }
        println("Min/Max X: ${path.minOfOrNull { it.x }}, ${path.maxOfOrNull { it.x }}")
        println("Min/Max Y: ${path.minOfOrNull { it.y }}, ${path.maxOfOrNull { it.y }}")

        PrintWriter(
            BufferedWriter(
                OutputStreamWriter(
                    FileOutputStream(
                        exportDir.resolve("capture.pto").toFile()
                    )
                )
            )
        ).use { pto ->
            EasyCamera(0).use { cam ->
                path.forEachIndexed { idx, point ->
                    printer.jump(point)
                    val filename = "test_${idx.toString().padStart(4, '0')}_${point.x.round()}_${point.y.round()}.png"
                    cam.captureToFile(exportDir.resolve(filename).toString())
                    pto.println("i w640 h480 f0 v29.97 n\"$filename\" TrX0 TrY0 y0 p0 r0")
                }
            }
        }
    }
}




