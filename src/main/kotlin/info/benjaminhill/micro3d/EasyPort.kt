package info.benjaminhill.micro3d

import info.benjaminhill.micro3d.ConsolePrettyPrint.printlnBlue
import info.benjaminhill.micro3d.ConsolePrettyPrint.printlnError
import info.benjaminhill.micro3d.ConsolePrettyPrint.printlnGreen
import jssc.SerialPort
import jssc.SerialPortEvent
import jssc.SerialPortException
import jssc.SerialPortList
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.onFailure
import kotlinx.coroutines.channels.onSuccess
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * Simplified interface for interacting with a serial port, specifically for sending G-code commands.
 * All commands written to the port wait for an "ok" reply
 *
 * @param portName The name of the serial port to connect to (e.g., "COM3", "/dev/ttyUSB0").
 * @param baudRate The baud rate for the serial communication (default: 115200).
 * @param dataBits The number of data bits (default: 8).
 * @param stopBits The number of stop bits (default: 1).
 * @param parity The parity setting (default: none).
 */
class EasyPort(
    portName: String, baudRate: Int = BAUDRATE_115200, dataBits: Int = DATABITS_8,
    stopBits: Int = STOPBITS_1, parity: Int = PARITY_NONE
) : SerialPort(portName), AutoCloseable {
    private var receiveFlow: Flow<String>

    init {
        openPort()

        Runtime.getRuntime().addShutdownHook(object : Thread() {
            override fun run() {
                try {
                    if (this@EasyPort.closePort()) {
                        printlnError("Port was closed through Shutdown Hook")
                    }
                } catch (e: SerialPortException) {
                    // ignore
                }
            }
        })

        setParams(baudRate, dataBits, stopBits, parity)
        setFlowControlMode(FLOWCONTROL_XONXOFF_IN or FLOWCONTROL_XONXOFF_OUT)
        eventsMask = MASK_RXCHAR

        // discard anything already in the buffer
        runBlocking {
            delay(100.milliseconds)
        }
        this@EasyPort.purgePort(PURGE_RXCLEAR or PURGE_TXCLEAR)
        readString()

        receiveFlow = callbackFlow<String> {
            val buffer = StringBuilder()
            val listener = { _: SerialPortEvent ->
                while (true) {
                    buffer.append(readString() ?: break)
                    while (true) {
                        val newlineIndex = buffer.indexOfOrNull('\n') ?: break
                        val line = buffer.substring(0, newlineIndex)
                        printlnBlue(line.trim())
                        buffer.delete(0, newlineIndex + 1)
                        trySendBlocking(line).onSuccess { }
                            .onFailure { t: Throwable? -> println("trySendBlocking failure: $t") }
                    }
                }
            }

            addEventListener(listener)

            awaitClose {
                // println("receiveFlow closing, removeEventListener")
                removeEventListener()
            }
        }.onCompletion {
            // println("receiveFlow onCompletion")
        }
        println("Connected to port `$portName`")
        printlnGreen("  Color for SEND")
        printlnBlue("  Color for RECEIVE")
    }

    suspend fun writeAndWait(command: String, waitFor: String, maxDuration: Duration = 1.seconds): List<String> {
        val commandWithNewline = if (command.endsWith("\n")) command else "$command\n"
        printlnGreen(commandWithNewline.trim())
        require(writeString(commandWithNewline))
        return withTimeout(maxDuration) {
            receiveFlow.takeWhile { it != waitFor }
                .toList()
        }
    }

    override fun close() {
        closePort()
    }

    companion object {
        private fun CharSequence.indexOfOrNull(c: Char): Int? = indexOf(c).takeIf { it >= 0 }

        private fun choosePort(): String {
            val ports = SerialPortList.getPortNames()
            println("Found ${ports.size} ports.")
            require(ports.isNotEmpty()) { "Must have found at least one port to choose." }

            if (ports.size > 1) {
                ports.forEachIndexed { idx, port ->
                    println("$idx: $port")
                }
                print("PORT? > ")
                return ports[readln().toInt()]
            }
            return ports.first()
        }

        fun connect(): EasyPort {
            val portName = choosePort()
            val ep = EasyPort(portName)
            return ep
        }
    }
}