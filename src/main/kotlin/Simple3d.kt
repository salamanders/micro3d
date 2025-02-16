import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.system.exitProcess

import com.fazecast.jSerialComm.SerialPort
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking

fun main() {
    val serialPorts = SerialPort.getCommPorts()

    if (serialPorts.isEmpty()) {
        println("No serial ports found.  Is your printer plugged in?")
        exitProcess(1)
    }

    val printerPort = serialPorts.firstOrNull { it.descriptivePortName.contains("3D Printer", true) } //Try to be a little smart about this.

    if (printerPort == null) {
        println("No 3D printer found.  Please ensure it is plugged in and recognized.")
        println("Available Ports:")
        serialPorts.forEach { println(it.descriptivePortName) }
        exitProcess(1)
    }


    println("Connecting to ${printerPort.descriptivePortName}")

    try {
        printerPort.setComPortParameters(115200, 8, 1, 0) // Adjust as needed for your printer
        printerPort.setComPortTimeouts(SerialPort.TIMEOUT_READ_BLOCKING, 1000, 0) // Adjust as needed
        printerPort.openPort()
    } catch (e: Exception) {
        println("Error opening port: ${e.message}")
        exitProcess(1)
    }



    println("Connected. Waiting 30 seconds before homing...")

    runBlocking {
        delay(TimeUnit.SECONDS.toMillis(30))

        try {
            val gcodeCommand = "G28\n" // Homing command
            //printerPort.writeBytes(gcodeCommand.toByteArray(), gcodeCommand.length.toLong())
            println("Homing command sent.")
        } catch (e: IOException) {
            println("Error sending G-code: ${e.message}")
        } finally {
            printerPort.closePort()
            println("Connection closed.")
        }
    }
}