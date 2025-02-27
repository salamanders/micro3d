package info.benjaminhill.micro3d

import info.benjaminhill.micro3d.Paths.toUnitXY

object Paths {

    fun expandLSystem(axiom: String, rules: Map<Char, String>, iterations: Int): List<Char> =
        (1..iterations).fold(axiom) { currentString, _ -> // Using a range for iterations
            currentString.map { c ->
                rules[c] ?: c.toString()
            }.joinToString("")
        }.filter { it !in rules.keys }.toList()

    fun mooreCurve(iterations: Int = 3) = expandLSystem(
        // https://en.wikipedia.org/wiki/Moore_curve
        axiom = "LFL+F+LFL", rules = mapOf(
            'L' to "-RF+LFL+FR-", 'R' to "+LF-RFR-FL+"
        ), iterations = iterations
    )

    /**
     *  "F" means "draw forward",
     *  "+" means "turn left 90°",
     *  "-" means "turn right 90°"
     *  Anything else should be filtered out before drawing
     */
    fun List<Char>.toUnitXY(): List<Pair<Int, Int>> {

        var direction = 0
        var x = 0
        var y = 0

        return mapNotNull { action ->
            when (action) {
                '+' -> {
                    direction -= 90
                    null
                }

                '-' -> {
                    direction += 90
                    null
                }

                'F' -> {
                    when ((direction % 360 + 360) % 360) {
                        0 -> y -= 1
                        90 -> x += 1
                        180 -> y += 1
                        270 -> x -= 1
                        else -> error("Bad direction:${direction}")
                    }
                    x to y
                }

                else -> error("Bad action:$action")
            }
        }
    }
}
