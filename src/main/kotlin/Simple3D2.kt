import com.fazecast.jSerialComm.SerialPort
import kotlinx.coroutines.*
import java.io.IOException
import java.util.*

fun main() = runBlocking {
    Simple3D().use { printer ->
        if (!printer.connectToPrinter()) {
            println("Failed to connect to the printer.")
            return@runBlocking
        }

        printer.homePrinter()

        println("Printer connected and homed. Use q/w/e for +X/+Y/+Z, a/s/d for -X/-Y/-Z. 'exit' to quit.")
        printer.listenForCommands()
    }
}

class Simple3D : AutoCloseable {
    private lateinit var serialPort: SerialPort
    private val baudRate = 250000 // Or 115200, match your printer's firmware.
    private val stepSize = 0.1 // Smallest move. Adjust if needed.

    private var x = 0.0
    private var y = 0.0
    private var z = 0.0

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob()) // Use IO dispatcher for blocking operations

    fun connectToPrinter(): Boolean {
        val port = selectSerialPort() ?: return false

        serialPort = port
        serialPort.apply {
            baudRate = this@Simple3D.baudRate
            numDataBits = 8
            numStopBits = 1
            parity = SerialPort.NO_PARITY
            setFlowControl(SerialPort.FLOW_CONTROL_XONXOFF_IN_ENABLED or SerialPort.FLOW_CONTROL_XONXOFF_OUT_ENABLED)
        }

        return if (serialPort.openPort()) {
            println("Successfully connected to ${serialPort.systemPortName}")
            // Use launch to perform non-blocking initialization
            scope.launch {
                delay(2000) // Give the printer time to initialize.  Use delay, not Thread.sleep
                clearBuffer()
            }
            true
        } else {
            println("Failed to open port: ${serialPort.systemPortName}")
            false
        }
    }

    private fun selectSerialPort(): SerialPort? {
        val ports = SerialPort.getCommPorts()
        if (ports.isEmpty()) {
            println("No serial ports found. Is the printer connected and powered on?")
            return null
        }

        println("Available serial ports:")
        ports.forEachIndexed { index, port ->
            println("$index: ${port.systemPortName} - ${port.portDescription}")
        }

        print("Enter the number of the port to use: ")
        return readlnOrNull()?.toIntOrNull()?.let { index ->
            if (index in ports.indices) ports[index] else {
                println("Invalid port selection.")
                selectSerialPort() // Recursive call for retry
            }
        } ?: selectSerialPort()  // Handle null or non-integer input
    }

    fun homePrinter() {
        sendGCode("G28") // Home all axes
    }

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

    private suspend fun waitForOk() = withContext(Dispatchers.IO) { // Switch to IO dispatcher
        val response = StringBuilder()
        withTimeoutOrNull(5000) { // 5-second timeout using Kotlin's timeout
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