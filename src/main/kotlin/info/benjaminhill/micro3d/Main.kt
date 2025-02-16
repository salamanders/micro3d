package info.benjaminhill.micro3d

import info.benjaminhill.micro3d.Simple3D
import kotlinx.coroutines.runBlocking
import kotlin.use

fun main() = runBlocking {
    Simple3D().use { printer ->
        printer.connectToPrinter()
        // printer.homePrinter()
        println("Printer connected. Use q/w/e for +X/+Y/+Z, a/s/d for -X/-Y/-Z. 'exit' to quit.")
        printer.listenForCommands()
    }
}