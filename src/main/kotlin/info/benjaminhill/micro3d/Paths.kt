package info.benjaminhill.micro3d

/**
 * Object containing functions related to path generation, particularly the Moore curve.
 */
object Paths {

    /**
     * Expands an L-system string based on given rules and iterations.
     *
     * An L-system (Lindenmayer system) is a string rewriting system that can generate complex patterns.
     * This function takes an initial string (axiom), a set of replacement rules, and a number of iterations.
     * In each iteration, it replaces characters in the string according to the rules.
     * After all iterations, it filters out characters that are keys in the rules.
     *
     * @param axiom The initial string of the L-system.
     * @param rules A map where keys are characters to be replaced, and values are their replacements.
     * @param iterations The number of times to apply the expansion rules.
     * @return A list of characters representing the final expanded string.
     */
    fun expandLSystem(axiom: String, rules: Map<Char, String>, iterations: Int): List<Char> =
        (1..iterations).fold(axiom) { currentString, _ -> // Using a range for iterations
            currentString.map { c ->
                rules[c] ?: c.toString()
            }.joinToString("")
        }.filter { it !in rules.keys }.toList()

    /**
     * Generates the Moore curve as a list of directional actions.
     * Based on https://en.wikipedia.org/wiki/Moore_curve
     *
     * @param iterations The number of iterations for generating the Moore curve.
     * @return A list of characters representing the actions for drawing the curve.
     */
    fun mooreCurve(iterations: Int = 3) = expandLSystem(
        axiom = "LFL+F+LFL", rules = mapOf(
            'L' to "-RF+LFL+FR-", 'R' to "+LF-RFR-FL+"
        ), iterations = iterations
    )

    /**
     * Converts a list of directional actions to a sequence of unit XY coordinates.
     *
     * "F" means "draw forward",
     * "+" means "turn left 90°",
     * "-" means "turn right 90°"
     * Anything else should be filtered out before drawing.
     *
     * @receiver A list of characters representing the actions.
     * @return A list of pairs, where each pair is an (x, y) coordinate.
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
