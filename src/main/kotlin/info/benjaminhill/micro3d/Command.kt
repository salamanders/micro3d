package info.benjaminhill.micro3d

import info.benjaminhill.micro3d.GCodeCommand.*
import info.benjaminhill.micro3d.Point3D.Companion.Axis
import java.util.*
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

sealed class Command(val trigger: String) {
    companion object {
        fun values(): Set<Command> = setOf(Exit) +
                setOf(
                    MoveXPlus,
                    MoveXNeg,
                    MoveYPlus,
                    MoveYNeg,
                    MoveZPlus,
                    MoveZNeg,
                    TurnOffFan0,
                    TurnOffFan1,
                ) + setOf(Home, GetPosition)

        fun fromInput(input: String): Command? {
            val normalizedInput = input.lowercase(Locale.getDefault())
            return values().firstOrNull { normalizedInput == it.trigger }
                ?: if (normalizedInput.startsWith("g")) RawGCode(normalizedInput) else null
        }
    }
}

object Exit : Command("exit")

data class RawGCode(val rawGCode: String) : Command("g") {
    init {
        require(rawGCode.startsWith("g", true) || rawGCode.startsWith("m", true))
        println("Raw GCode: `$rawGCode`")
    }
}

/** commands that require translating to GCode */
sealed class GCodeCommand(trigger: String,  val duration:Duration = 5.seconds) : Command(trigger) {
    abstract fun toGCode(location: Point3D? = null): String
    abstract class AbsoluteMove(trigger: String, val axis: Axis, val step: Double) :
        GCodeCommand(trigger) {
        override fun toGCode(location: Point3D?): String {
            location!! += axis to step
            return location.toString()
        }
    }



    object MoveXPlus : AbsoluteMove("q", Axis.X, SMALLEST_XY)
    object MoveXNeg : AbsoluteMove("a", Axis.X, -SMALLEST_XY)

    object MoveYPlus : AbsoluteMove("w", Axis.Y, SMALLEST_XY)
    object MoveYNeg : AbsoluteMove("s", Axis.Y, -SMALLEST_XY)

    object MoveZPlus : AbsoluteMove("e", Axis.Z, SMALLEST_Z)
    object MoveZNeg : AbsoluteMove("d", Axis.Z, -SMALLEST_Z)

    object TurnOffFan0 : GCodeCommand("fan0") {
        override fun toGCode(location: Point3D?): String = "M107"
    }

    object TurnOffFan1 : GCodeCommand("fan1") {
        override fun toGCode(location: Point3D?): String = "M107 P1"
    }

     object Home : GCodeCommand("home", 30.seconds) {
        override fun toGCode(location: Point3D?): String = "G28"
    }

     object GetPosition : GCodeCommand("getpos") {
        // "X:0.90 Y:0.00 Z:0.45 E:0.00 Count X:72 Y:0 Z:180"
        // X:103.00 Y:150.00 Z:10.00 E:0.00 Count X:8240 Y:12000 Z:4000
        override fun toGCode(location: Point3D?): String = "M114"
    }

    companion object {
        const val SMALLEST_XY:Double = 0.1
        const val SMALLEST_Z:Double = 0.04
    }
}