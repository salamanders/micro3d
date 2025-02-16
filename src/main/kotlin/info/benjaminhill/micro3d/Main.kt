package info.benjaminhill.micro3d

import kotlinx.coroutines.runBlocking

fun main() = runBlocking {
    val printer = Simple3D()
    printer.connectToPrinter()
    printer.processCommands(printer.listenForCommands()) // suspends until the flow completes.
    printer.close()
}