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
        override fun toGCode(): String = "G0 X%.3f Y%.3f Z%.3f".format(STEP_SIZE, 0.0, 0.0)
    }

    class MoveXNeg : GCodeCommand("a") {
        override fun toGCode(): String = "G0 X%.3f Y%.3f Z%.3f".format(-STEP_SIZE, 0.0, 0.0)
    }

    class MoveYPlus : GCodeCommand("w") {
        override fun toGCode(): String = "G0 X%.3f Y%.3f Z%.3f".format(0.0, STEP_SIZE, 0.0)
    }

    class MoveYNeg : GCodeCommand("s") {
        override fun toGCode(): String = "G0 X%.3f Y%.3f Z%.3f".format(0.0, -STEP_SIZE, 0.0)
    }

    class MoveZPlus : GCodeCommand("e") {
        override fun toGCode(): String = "G0 X%.3f Y%.3f Z%.3f".format(0.0, 0.0, STEP_SIZE)
    }

    class MoveZNeg : GCodeCommand("d") {
        override fun toGCode(): String = "G0 X%.3f Y%.3f Z%.3f".format(0.0, 0.0, -STEP_SIZE)
    }

    data object Home : GCodeCommand("home") {
        override fun toGCode(): String = "G28"
    }

    data object GetPosition : GCodeCommand("getpos") {
        override fun toGCode(): String = "M114"
    }

    companion object {
        const val STEP_SIZE = 0.1 // Smallest move. Adjust if needed.
    }
}