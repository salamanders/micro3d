package info.benjaminhill.micro3d

import jssc.SerialPort
import jssc.SerialPort.BAUDRATE_115200
import jssc.SerialPort.DATABITS_8
import jssc.SerialPort.FLOWCONTROL_XONXOFF_IN
import jssc.SerialPort.FLOWCONTROL_XONXOFF_OUT
import jssc.SerialPort.MASK_RXCHAR
import jssc.SerialPort.PARITY_NONE
import jssc.SerialPort.STOPBITS_1
import jssc.SerialPortEvent
import jssc.SerialPortList
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.onFailure
import kotlinx.coroutines.channels.onSuccess
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.flow.toList
import kotlin.collections.contains


class EasyPort(
    portName: String, baudRate: Int = BAUDRATE_115200, dataBits: Int = DATABITS_8,

    stopBits: Int = STOPBITS_1, parity: Int = PARITY_NONE
) : SerialPort(portName), AutoCloseable {
    private lateinit var initString: String
    private var receiveFlow: Flow<String>

    init {
        openPort()
        setParams(baudRate, dataBits, stopBits, parity)
        setFlowControlMode(FLOWCONTROL_XONXOFF_IN or FLOWCONTROL_XONXOFF_OUT)
        eventsMask = MASK_RXCHAR

        receiveFlow = callbackFlow<String> {
            val buffer = StringBuilder()
            val listener = { _: SerialPortEvent ->
                while (true) {
                    buffer.append(readString() ?: break)
                    while (true) {
                        val newlineIndex = buffer.indexOfOrNull('\n') ?: break
                        val line = buffer.substring(0, newlineIndex)
                        buffer.delete(0, newlineIndex + 1)
                        trySendBlocking(line).onSuccess { }.onFailure { t: Throwable? -> println("Bad things: $t") }
                    }
                }
            }

            addEventListener(listener)

            awaitClose {
                println("receiveFlow closing, removeEventListener")
                removeEventListener()
            }
        }.onCompletion {
            println("receiveFlow onCompletion")
        }
    }
    suspend fun writeAndWait(gcode: String): List<String> {
        val command = if (gcode.endsWith("\n")) gcode else "$gcode\n"
        require(writeString(command))
        return receiveFlow.takeWhile { it != "ok" }.toList()
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
                ports.forEach {
                    println("  $it")
                }
                print("PORT? >")
                return readln().also { require(it in ports) }
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