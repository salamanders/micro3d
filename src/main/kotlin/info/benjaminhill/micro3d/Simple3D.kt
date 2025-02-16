package info.benjaminhill.micro3d

import com.fazecast.jSerialComm.SerialPort
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.IOException
import java.util.Locale

class Simple3D : AutoCloseable {
    private lateinit var serialPort: SerialPort
    private val stepSize = 0.1 // Smallest move. Adjust if needed.

    private var x = 0.0
    private var y = 0.0
    private var z = 0.0

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob()) // Use IO dispatcher for blocking operations

    fun connectToPrinter() {
        serialPort = selectSerialPort()
        serialPort.apply {
            baudRate = 115200 // 250000 or 115200, match your printer's firmware.
            numDataBits = 8
            numStopBits = 1
            parity = SerialPort.NO_PARITY
            setFlowControl(SerialPort.FLOW_CONTROL_XONXOFF_IN_ENABLED or SerialPort.FLOW_CONTROL_XONXOFF_OUT_ENABLED)
            // setComPortTimeouts(SerialPort.TIMEOUT_READ_BLOCKING, 1000, 0) // Adjust as needed
        }
        require(serialPort.openPort()) { "Failed to open port: ${serialPort.systemPortName}" }
        println("Successfully connected to ${serialPort.systemPortName}")
        scope.launch {
            delay(2000)
            clearBuffer()
            println("Ready to run.")
        }
    }

    private fun selectSerialPort(): SerialPort {
        val ports = SerialPort.getCommPorts()
        require(ports.isNotEmpty()) { "No serial ports found. Is the printer connected and powered on?" }
        println("Available serial ports:")
        ports.forEachIndexed { index, port ->
            println("$index: ${port.systemPortName} - ${port.portDescription} - ${port.descriptivePortName}")
            if(port.descriptivePortName .contains("3D Printer", true)) {
                println("  -- best guess!")
            }
        }
        print("Enter the number of the port to use: ")
        return  ports[readln().toInt()]
    }

//    fun homePrinter() {
//        sendGCode("G28") // Home all axes
//    }

    private fun sendGCode(gcode: String) = scope.launch {
        val command = if (gcode.endsWith("\n")) gcode else "$gcode\n"
        try {
            serialPort.outputStream.write(command.toByteArray())
            serialPort.outputStream.flush()
            println("Sent: ${command.trim()}")
            waitForOk()
        } catch (e: IOException) {
            println("Error sending GCode: ${e.message}")
        }
    }

    private suspend fun waitForOk() = withContext(Dispatchers.IO) {
        val response = StringBuilder()
        withTimeoutOrNull(5_000) { // 5-second timeout using Kotlin's timeout, will normally be much faster
            while (true) {
                if (serialPort.inputStream.available() > 0) {
                    val c = serialPort.inputStream.read().toChar()
                    response.append(c)
                    if (response.contains("ok")) {
                        println("Received: ${response.trim()}")
                        return@withTimeoutOrNull
                    }
                } else {
                    delay(50) // Non-blocking delay
                }
            }
        } ?: println("Timeout waiting for 'ok' response. Received: '${response.trim()}'")
    }

    private suspend fun clearBuffer() = withContext(Dispatchers.IO) {
        while (serialPort.inputStream.available() > 0) {
            serialPort.inputStream.read()
        }
        println("Buffer cleared.")
    }

    suspend fun listenForCommands() = withContext(Dispatchers.IO) { // Use withContext for blocking input
        while (true) {
            val command = readlnOrNull()?.trim()?.lowercase(Locale.getDefault()) ?: continue

            if (command == "exit") break

            when (command) {
                "q" -> move(x = stepSize)
                "a" -> move(x = -stepSize)
                "w" -> move(y = stepSize)
                "s" -> move(y = -stepSize)
                "e" -> move(z = stepSize)
                "d" -> move(z = -stepSize)
                else -> println("Invalid command.")
            }
        }
    }

    private fun move(x: Double = 0.0, y: Double = 0.0, z: Double = 0.0) {
        this.x += x
        this.y += y
        this.z += z
        sendGCode("G0 X%.3f Y%.3f Z%.3f".format(this.x, this.y, this.z))
    }


    override fun close() {
        if (::serialPort.isInitialized && serialPort.isOpen) {
            sendGCode("M84") // Disable steppers
            scope.launch { // Use a coroutine to close without blocking
                try {
                    serialPort.outputStream.close()
                    serialPort.inputStream.close()
                } catch (e: IOException) {
                    println("Error closing streams: ${e.message}")
                }
                serialPort.closePort()
                println("Disconnected from printer.")
                scope.cancel() // Cancel all coroutines in the scope
            }
        }
    }
}