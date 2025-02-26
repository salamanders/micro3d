package info.benjaminhill.micro3d

import info.benjaminhill.micro3d.Point3D.Companion.round
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

