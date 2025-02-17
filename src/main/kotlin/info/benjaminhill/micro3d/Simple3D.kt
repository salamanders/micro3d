package info.benjaminhill.micro3d

import com.fazecast.jSerialComm.SerialPort
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.io.IOException
import java.util.*

class Simple3D : AutoCloseable {

    private lateinit var serialPort: SerialPort
    private val scope = CoroutineScope(Dispatchers.IO + Job())

    fun connectToPrinter() {
        serialPort = selectSerialPort()
        serialPort.apply {
            baudRate = 115200
            numDataBits = 8
            numStopBits = 1
            parity = SerialPort.NO_PARITY
            setFlowControl(SerialPort.FLOW_CONTROL_XONXOFF_IN_ENABLED or SerialPort.FLOW_CONTROL_XONXOFF_OUT_ENABLED)
        }

        require(serialPort.openPort()) { "Failed to open port: ${serialPort.systemPortName}" }
        println("Successfully connected to ${serialPort.systemPortName}")

        scope.launch {
            delay(2000) // Give the printer time to initialize
            clearBuffer()
            println("Ready to run.")
        }
        println("Switching to relative mode...")
        sendGCode("G91") // switch to relative mode for the move commands
    }

    private fun selectSerialPort(): SerialPort {
        val ports = SerialPort.getCommPorts()
        require(ports.isNotEmpty()) { "No serial ports found. Is the printer connected and powered on?" }

        println("Available serial ports:")
        ports.forEachIndexed { index, port ->
            println("$index: ${port.systemPortName} - ${port.portDescription} - ${port.descriptivePortName}")
            if (port.descriptivePortName.contains("3D Printer", true)) {
                println("  -- best guess!")
            }
        }
        print("Port number: ")
        return ports[readln().toInt()]
    }


    private fun sendGCode(gcode: String) = scope.launch {
        require(gcode.startsWith("g", true) || gcode.startsWith("m", true)) { "Rejecting command `${gcode.trim()}`." }
        val command = if (gcode.endsWith("\n")) gcode else "$gcode\n"
        try {
            withContext(Dispatchers.IO) { // Correctly uses Dispatchers.IO for blocking I/O
                serialPort.outputStream.write(command.toByteArray())
                serialPort.outputStream.flush()
            }
            println("Sent GCode: `${command.trim()}`") // Log after sending, inside the launch
            waitForOk()
        } catch (e: IOException) {
            println("Error sending GCode: ${e.message}")
        }
    }

    /** After every sendGCode.  Important to use withContext(Dispatchers.IO) */
    private suspend fun waitForOk() = withContext(Dispatchers.IO) {
        val response = StringBuilder()
        withTimeoutOrNull(5_000) {
            while (true) {
                val available = serialPort.inputStream.available()
                if (available > 0) {
                    val buffer = ByteArray(available)
                    val bytesRead = serialPort.inputStream.read(buffer)
                    response.append(String(buffer, 0, bytesRead))
                    if (response.contains("ok")) {
                        println("Received: ${response.trim()}")
                        return@withTimeoutOrNull
                    }
                } else {
                    delay(50)
                }
            }
        } ?: println("Timeout waiting for 'ok' response. Received: '${response.trim()}'")
    }

    /** After initial connection */
    private suspend fun clearBuffer() = withContext(Dispatchers.IO) {
        while (serialPort.inputStream.available() > 0) {
            serialPort.inputStream.read() // Read and discard
        }
        println("Buffer cleared.")
    }

    fun listenForCommands(): Flow<Command> = flow {
        while (true) {
            print("> ")
            val input = readlnOrNull()?.trim()?.lowercase(Locale.getDefault()) ?: continue
            val command = Command.fromInput(input)
            command?.let { emit(it) } ?: println(
                "Invalid command: '$input'.  ${
                Command.values().joinToString(",") { it.trigger }
            }")
        }
    }.onCompletion { cause ->
        if (cause != null) {
            println("Command listener stopped: ${cause.message}")
        } else {
            println("Command listener stopped gracefully.")
        }
    }.flowOn(Dispatchers.IO)

    //  Separate function to process the command Flow.
    suspend fun processCommands(commandFlow: Flow<Command>) {
        commandFlow.takeWhile { it != Exit }.collect { command ->
            when (command) {
                is Exit -> { /* Handled by takeWhile. */
                }

                is GCodeCommand -> sendGCode(command.toGCode())
                is RawGCode -> sendGCode(command.rawGCode)
            }
        }
    }

    override fun close() {
        runBlocking {  // wait for cleanup
            if (::serialPort.isInitialized && serialPort.isOpen) {
                sendGCode("M84") // Disable steppers
                try {
                    serialPort.outputStream.close()
                    serialPort.inputStream.close()
                } catch (e: IOException) {
                    println("Error closing streams: ${e.message}")
                }
                serialPort.closePort()
                println("Disconnected from printer.")
            }
            scope.cancel()
        }
    }
}
