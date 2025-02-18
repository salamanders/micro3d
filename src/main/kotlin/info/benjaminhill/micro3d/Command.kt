package info.benjaminhill.micro3d

import info.benjaminhill.micro3d.GCodeCommand.*
import java.util.*

sealed class Command(val trigger: String) {
    companion object {
        fun values(): Set<Command> = setOf(Exit) +
                setOf(
                    MoveXPlus(),
                    MoveXNeg(),
                    MoveYPlus(),
                    MoveYNeg(),
                    MoveZPlus(),
                    MoveZNeg(),
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
        require(rawGCode.startsWith("g", true))
    }
}

/** commands that require translating to GCode */
sealed class GCodeCommand(trigger: String) : Command(trigger) {
    abstract fun toGCode(location: Point3D? = null): String

    abstract class AbsoluteMove(trigger: String, val axisName: String, val stepDirection: Int) :
        GCodeCommand(trigger) {
        private val stepSize: Double = 1.0 // Smallest move. Adjust if needed.

        // G1 for a precision move
        private val gcodeAbsoluteMove = "G1 %s%.2f"
        override fun toGCode(location: Point3D?): String {
            location!! += axisName to (stepDirection * stepSize)
            return gcodeAbsoluteMove.format(axisName, location[axisName])
        }
    }

    class MoveXPlus() : AbsoluteMove("q", "X", 1)
    class MoveXNeg() : AbsoluteMove("a", "X", -1)

    class MoveYPlus() : AbsoluteMove("w", "Y",  1)
    class MoveYNeg() : AbsoluteMove("s", "Y",  -1)

    class MoveZPlus() : AbsoluteMove("e", "Z",  1)
    class MoveZNeg() : AbsoluteMove("d", "Z", -1)

    data object Home : GCodeCommand("home") {
        override fun toGCode(location: Point3D?): String = "G28"
    }

    data object GetPosition : GCodeCommand("getpos") {
        override fun toGCode(location: Point3D?): String = "M114"
    }
}