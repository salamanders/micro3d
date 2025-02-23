package info.benjaminhill.micro3d

import jssc.SerialPort
import jssc.SerialPortList
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class EasyPort(
    portName: String, baudRate: Int = BAUDRATE_115200, dataBits: Int = DATABITS_8,

    stopBits: Int = STOPBITS_1, parity: Int = PARITY_NONE
) : SerialPort(portName), AutoCloseable {
    lateinit var initString: String

    init {
        openPort()
        setParams(baudRate, dataBits, stopBits, parity)
        setFlowControlMode(FLOWCONTROL_XONXOFF_IN or FLOWCONTROL_XONXOFF_OUT)
    }

    suspend fun prepare() {
        println("Pausing for 2 seconds, reading all in string.")
        delay(2.seconds)
        initString = readString()
        //println("Purging port.")
        //purgePort(PURGE_RXCLEAR or PURGE_TXCLEAR)
        //delay(2.seconds)
    }


    private fun portToFlow(): Flow<String> = channelFlow {
        var lookingForOk = true
        val buffer = StringBuilder()
        while (lookingForOk) {
            readString()?.let { buffer.append(it) }
            val newlineIndex = buffer.indexOf('\n')
            if (newlineIndex > -1) {
                val line = buffer.substring(0, newlineIndex)
                buffer.delete(0, newlineIndex + 1) // Remove the emitted line and newline
                if (line.trim() == "ok") {
                    lookingForOk = false
                } else {
                    send(line)
                }
            }
            if (lookingForOk) {
                delay(50.milliseconds)
            }
        }
    }

    suspend fun waitForOk(): List<String> = portToFlow().toList()

    override fun close() {
        closePort()
    }

    companion object {
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
        require(port.writeString("G28\n"))
        val homingResults = port.waitForOk()
        println("Homing results:")
        println(homingResults.joinToString(separator = "\n  ", prefix = "  "))
        println("Getting position M114.")
        require(port.writeString("M114\n"))
        val positionResults = port.waitForOk()
        println("Position results:")
        println(positionResults.joinToString(separator = "\n  ", prefix = "  "))
    }
    println("Ending app.")
}


//
//fun writeData() {
//    val port = SerialPort("COM10")
//    port.openPort()
//    port.setParams(BAUDRATE_115200, DATABITS_8, STOPBITS_1, PARITY_NONE)
//    port.writeBytes("Testing serial from Kotlin".encodeToByteArray())
//    // The following shows up in the serial port (prettified for readability):
//    // 54 65 73 74 69 6E 67 20 73 65 72 69 61 6C 20 66 72 6F 6D 20 4B 6F 74 6C 69 6E
//    port.closePort()
//}
//
//fun readData() {
//    val port = SerialPort("COM10")
//    port.openPort()
//    port.setParams(BAUDRATE_115200, DATABITS_8, STOPBITS_1, PARITY_NONE)
//    // port.setParams(9600, 8, 1, 0); // alternate technique
//    val buffer = port.readBytes(10 /* read the first 10 bytes */)
//    // Print the buffer but pretty, with spaces between bytes and padding to two characters with 0
//    // See below for implementation
//    println(buffer.fancyToString())
//    // Using the same bytes as used in the writeData() method above we get:
//    // 54 65 73 74 69 6E 67 20 73 65 72 69 61 6C 20 66 72 6F 6D 20 4B 6F 74 6C 69 6E
//    port.closePort()
//}
//
//fun eventListener() {
//    val port = SerialPort("COM10")
//    port.openPort()
//    port.setParams(BAUDRATE_115200, DATABITS_8, STOPBITS_1, PARITY_NONE)
//    // port.setParams(9600, 8, 1, 0); // alternate technique
//    val mask = MASK_RXCHAR + MASK_CTS + MASK_DSR
//    port.eventsMask = mask
//    port.addEventListener(MyPortListener(port))
//}
//
///*
// * In this class must implement the method serialEvent, through it we learn about
// * events that happened to our port. But we will not report on all events but only
// * those that we put in the mask. In this case the arrival of the data and change the
// * status lines CTS and DSR
// */
//internal class MyPortListener(private val port: SerialPort) : SerialPortEventListener {
//    override fun serialEvent(event: SerialPortEvent) {
//        when {
//            event.isRXCHAR -> { // data is available
//                // read data, if 10 bytes available
//                if (event.eventValue == 10) {
//                    try {
//                        println(port.readBytes(10).fancyToString())
//                    } catch (ex: SerialPortException) {
//                        println(ex)
//                    }
//                }
//            }
//
//            event.isCTS -> { // CTS line has changed state
//                if (event.eventValue == 1) { // line is ON
//                    println("CTS - ON")
//                } else {
//                    println("CTS - OFF")
//                }
//            }
//
//            event.isDSR -> { // DSR line has changed state
//                if (event.eventValue == 1) { // line is ON
//                    println("DSR - ON")
//                } else {
//                    println("DSR - OFF")
//                }
//            }
//        }
//    }
//}
//
//// Please note that Unsigned Integers are a stable feature in Kotlin version >= 1.5
//// This is an extension function for ByteArrays to print them as unsigned hexadecimals
//@OptIn(ExperimentalUnsignedTypes::class)
//private fun ByteArray.fancyToString(): String {
//    var res = ""
//    // Converts each element from a signed byte to an unsigned byte
//    this.asUByteArray().forEach {
//        // Returns the bytes as two hexadecimals, separated by spaces per pair. Fills out the leading zero if necessary
//        res += it.toString(16).uppercase(Locale.getDefault()).padStart(2, '0').padEnd(3, ' ')
//    }
//    // And remove the trailing whitespace
//    return res.trim()
//}
//
//@OptIn(ExperimentalUnsignedTypes::class)
//private fun ByteArray.fancyToString2(): String {
//    return this.asUByteArray().joinToString(" ") { it.toString(16).uppercase(Locale.US).padStart(2, '0') }
//}