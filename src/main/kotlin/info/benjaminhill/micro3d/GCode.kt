package info.benjaminhill.micro3d

import info.benjaminhill.micro3d.ConsolePrettyPrint.printlnError
import kotlinx.coroutines.runBlocking
import kotlin.math.abs
import kotlin.reflect.KCallable
import kotlin.reflect.KClass
import kotlin.reflect.full.memberFunctions
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class GCode(private val port: EasyPort) {
    private var currentLocation: Point3D

    private suspend fun writeAndWait(gcode: String, maxDuration: Duration = 2.seconds) =
        port.writeAndWait(gcode, "ok", maxDuration)

    init {
        runBlocking {
            println("Setting up, getting initial position...")
            currentLocation = position()
            println("done getting position.")
        }
    }

    fun getLocation() = currentLocation.copy()

    suspend fun jump(newLocation: Point3D) {
        writeAndWait(newLocation.toGCode())
        currentLocation = newLocation
    }

    suspend fun home() = writeAndWait("G28")

    private suspend fun position(): Point3D {
        val positionString = writeAndWait("M114", maxDuration = 5.seconds).first { it.contains("X:") }
        val positionRe = """X:(-?[0-9.]+) Y:(-?[0-9.]+) Z:(-?[0-9.]+)""".toRegex()
        val (x, y, z) = positionRe.find(positionString)!!.groupValues.drop(1).map(String::toDouble)
        return Point3D(x, y, z)
    }

    suspend fun up() = jump(currentLocation.copy(z = currentLocation.z + SMALLEST_Z))
    suspend fun down() = jump(currentLocation.copy(z = currentLocation.z - SMALLEST_Z))
    suspend fun left() = jump(currentLocation.copy(x = currentLocation.x - SMALLEST_XY))
    suspend fun right() = jump(currentLocation.copy(x = currentLocation.x + SMALLEST_XY))
    suspend fun forward() = jump(currentLocation.copy(y = currentLocation.y + SMALLEST_XY))
    suspend fun back() = jump(currentLocation.copy(y = currentLocation.y - SMALLEST_XY))

    suspend fun invokeUsingName(commandLetter: Char) {
        when (commandLetter) {
            'u' -> up()
            'd' -> down()
            'l' -> left()
            'r' -> right()
            'f' -> forward()
            'b' -> back()
            'h' -> home()
            else -> printlnError("Unknown: `$commandLetter`")
        }
    }

    fun invokeUsingNameDynamic(prefix: String) {
        val kClass: KClass<out GCode> = this::class
        val uFunctions = kClass.memberFunctions.filter { it.name.startsWith(prefix) }
        when {
            uFunctions.isEmpty() -> printlnError("No function starts with `$prefix`")
            uFunctions.size > 1 -> printlnError("More than one function starts with `$prefix`")
            else -> {
                val firstUFunction: KCallable<*> = uFunctions.first()
                println(firstUFunction.name)
                println(firstUFunction.parameters.size)
                firstUFunction.parameters.forEach { p ->
                    println("  param: name:${p.name} kind:${p.kind} type:${p.type}")
                }
                try {
                    firstUFunction.call(this) // Invoke the function
                } catch (e: Exception) {
                    printlnError("Error invoking function: ${e.message}")
                }
            }
        }
    }

    companion object {

        // TODO: Can micro-stepping decrease this number?
        const val SMALLEST_XY: Double = 0.1
        const val SMALLEST_Z: Double = 0.04

        /**
         * Returns a string representation of the point in G-code format.
         *
         * @return A string in the format "G1 X%.2f Y%.2f Z%.2f".
         */
        fun Point3D.toGCode(): String {
            return "G1 X%.2f Y%.2f Z%.2f".format(x.roundToZeroIfClose(), y.roundToZeroIfClose(), z.roundToZeroIfClose())
        }

        private const val EPSILON = 1e-4 // Define a small tolerance for near-zero values

        private fun Double.roundToZeroIfClose(): Double {
            return if (abs(this) < EPSILON) 0.0 else this
        }
    }
}
