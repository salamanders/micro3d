package info.benjaminhill.micro3d


fun expandLSystem(axiom: String, rules: Map<Char, String>, iterations: Int): String =
    (1..iterations).fold(axiom) { currentString, _ -> // Using a range for iterations
        currentString.map { c ->
            rules[c] ?: c.toString()
        }.joinToString("")
    }.filter { it !in rules.keys }

fun main() {
    val axiom = "A"
    // "F" means "draw forward",
    // "+" means "turn left 90°",
    // "-" means "turn right 90°"
    // and "A" and "B" are ignored during drawing.

    val expandedString = expandLSystem(
        // Hilbert Curve
        axiom = "A", rules = mapOf(
            'A' to "+BF-AFA-FB+",
            'B' to "-AF+BFB+FA-"
        ), iterations = 2
    )
    println("Expanded L-System String:")
    println(expandedString)
}