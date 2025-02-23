package info.benjaminhill.micro3d

import com.fazecast.jSerialComm.SerialPort
import info.benjaminhill.micro3d.GCodeCommand.GetPosition
import info.benjaminhill.micro3d.GCodeCommand.Home
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.io.IOException
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Keeps state like current location
 */
class Simple3DPrinter : AutoCloseable {

    private lateinit var serialPort: SerialPort
    private val scope = CoroutineScope(Dispatchers.IO + Job())

    // Be sure to update once real location is known.
    var location: Point3D = Point3D(103.0, 150.0, 10.0)

    val responses = PrinterResponse()

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
            // Attempt to get actual start location
            processCommands(flowOf(Home, GetPosition))
        }
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


    private fun sendGCode(gcode: String, duration: Duration = 5.seconds) = runBlocking {
        require(gcode.startsWith("g", true) || gcode.startsWith("m", true)) { "Rejecting command `${gcode.trim()}`." }
        val command = if (gcode.endsWith("\n")) gcode else "$gcode\n"
        try {
            withContext(Dispatchers.IO) { // Correctly uses Dispatchers.IO for blocking I/O
                serialPort.outputStream.write(command.toByteArray())
                serialPort.outputStream.flush()
            }
            responses.appendLog("SENT: `${command.trim()}`")
            waitFor("ok", duration)
        } catch (e: IOException) {
            printErrorLn("Error sending GCode: ${e.message}")
        }
        return@runBlocking
    }

    /** After every sendGCode.  Important to use withContext(Dispatchers.IO) */
    private suspend fun waitFor(allDoneIndicator: String = "ok", duration: Duration) = withContext(Dispatchers.IO) {
        val response = StringBuilder()
        val foundIndicator = AtomicBoolean(false)
        withTimeoutOrNull(duration) {
            while (!foundIndicator.get()) {
                val available = serialPort.inputStream.available()
                if (available > 0) {
                    val buffer = ByteArray(available)
                    val bytesRead = serialPort.inputStream.read(buffer)
                    response.append(String(buffer, 0, bytesRead))
                    // Special handling for replies like getpos
                    if (response.contains(allDoneIndicator)) {
                        foundIndicator.set(true)
                    }
                } else {
                    delay(50)
                }
            }
            responses.appendLog(response.trim().toString())
        } ?: responses.appendLog("Timeout > $duration while waiting for `$allDoneIndicator`")
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
            command?.let { emit(it) } ?: printErrorLn(
                "Invalid command: '$input'.  ${
                    Command.values().joinToString(",") { it.trigger }
                }")
        }
    }.onCompletion { cause ->
        if (cause != null) {
            printErrorLn("Command listener stopped: ${cause.message}")
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

                is GCodeCommand -> sendGCode(command.toGCode(location), command.duration)
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
                    printErrorLn("Error closing streams: ${e.message}")
                }
                serialPort.closePort()
                println("Disconnected from printer.")
            }
            scope.cancel()
        }
    }

    companion object {

        fun printErrorLn(msg: String) {
            System.err.println(msg)
        }
    }
}
