package info.benjaminhill.micro3d


object PrettyPrint {
    const val ANSI_RESET = "\u001B[0m"
    const val ANSI_GREEN = "\u001B[32m"
    const val ANSI_RED = "\u001B[31m"

    //val ANSI_YELLOW = "\u001B[33m"
    const val ANSI_BLUE = "\u001B[34m"

    //val ANSI_PURPLE = "\u001B[35m"
    //val ANSI_CYAN = "\u001B[36m"
    //val ANSI_WHITE = "\u001B[37m"
    const val ANSI_BOLD = "\u001B[1m"

    fun printlnError(text: String) {
        println(ANSI_RED + ANSI_BOLD + text + ANSI_RESET)
    }

    fun printlnBlue(text: String) {
        println(ANSI_BLUE + text + ANSI_RESET)
    }

    fun printlnGreen(text: String) {
        println(ANSI_GREEN + text + ANSI_RESET)
    }
}
