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
    abstract fun toGCode(): String

    class MoveXPlus : GCodeCommand("q") {
        // G91 = relative.  G0 = non-extruder
        override fun toGCode(): String = RELATIVE_MOVE.format('X', STEP_SIZE)
    }

    class MoveXNeg : GCodeCommand("a") {
        override fun toGCode(): String = RELATIVE_MOVE.format('X', -STEP_SIZE)
    }

    class MoveYPlus : GCodeCommand("w") {
        override fun toGCode(): String = RELATIVE_MOVE.format('Y', STEP_SIZE)
    }

    class MoveYNeg : GCodeCommand("s") {
        override fun toGCode(): String = RELATIVE_MOVE.format('Y', -STEP_SIZE)
    }

    class MoveZPlus : GCodeCommand("e") {
        override fun toGCode(): String = RELATIVE_MOVE.format('Z', STEP_SIZE)
    }

    class MoveZNeg : GCodeCommand("d") {
        override fun toGCode(): String = RELATIVE_MOVE.format('Z', -STEP_SIZE)
    }

    data object Home : GCodeCommand("home") {
        override fun toGCode(): String = "G28"
    }

    data object GetPosition : GCodeCommand("getpos") {
        override fun toGCode(): String = "M114"
    }

    companion object {
        const val STEP_SIZE: Double = 1.0 // Smallest move. Adjust if needed.

        private const val RELATIVE_MOVE = "G1 %c%.2f"
    }
}