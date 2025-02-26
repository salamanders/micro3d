package info.benjaminhill.micro3d

import jssc.SerialPort
import jssc.SerialPortEvent
import jssc.SerialPortList
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.onFailure
import kotlinx.coroutines.channels.onSuccess
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.runBlocking
import kotlin.time.Duration.Companion.seconds

const val SMALLEST_XY: Double = 0.1
const val SMALLEST_Z: Double = 0.04

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


    suspend fun prepare() {
        println("Pausing for 2 seconds, reading all in string.")
        delay(2.seconds)
        initString = readString()
        //println("Purging port.")
        //purgePort(PURGE_RXCLEAR or PURGE_TXCLEAR)
        //delay(2.seconds)
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

        suspend fun connect(): EasyPort {
            val portName = choosePort()
            val ep = EasyPort(portName)
            ep.prepare()
            return ep
        }
    }
}


fun main() = runBlocking {
    EasyPort.connect().use { port ->
        println("Homing `G28`.")
        val homingResults = port.writeAndWait("G28")
        println("Homing results:")
        println(homingResults.joinToString(separator = "\n  ", prefix = "  "))
        println("Getting position M114.")
        val positionResults = port.writeAndWait("M114")
        println("Position results:")
        println(positionResults.joinToString(separator = "\n  ", prefix = "  "))
        val originalLocation = Point3D.fromPosition(positionResults.first())
        println("Original location: `$originalLocation`")

        repeat(100) { yi ->
            val yStartLocation = originalLocation.copy(y = originalLocation.y + yi * SMALLEST_XY)
            println("yStartLocation: `$yStartLocation`")

            repeat(100) { xi ->
                val xLocation = yStartLocation.copy(x = yStartLocation.x + xi * SMALLEST_XY)
                println("xLocation: `$xLocation`")
                port.writeAndWait(xLocation.toString())
            }
        }
    }
    println("Ending app.")
}