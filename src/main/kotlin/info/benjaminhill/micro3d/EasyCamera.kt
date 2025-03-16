package info.benjaminhill.micro3d

import kotlinx.coroutines.delay
import nu.pattern.OpenCV
import org.opencv.core.Mat
import org.opencv.imgcodecs.Imgcodecs
import org.opencv.videoio.VideoCapture
import org.opencv.videoio.Videoio.*
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds


class EasyCamera(cameraIndex: Int = 0) : AutoCloseable {
    private val camera: VideoCapture
    private val frame: Mat

    init {
        OpenCV.loadLocally()

        camera = VideoCapture(cameraIndex, CAP_DSHOW)
        frame = Mat()

        setResolution(640, 480)
    }

    fun setResolution(width: Int, height: Int) {
        require(camera.set(CAP_PROP_FRAME_WIDTH, width.toDouble()))
        require(camera.set(CAP_PROP_FRAME_HEIGHT, height.toDouble()))

        val actualWidth = camera[CAP_PROP_FRAME_WIDTH]
        val actualHeight = camera[CAP_PROP_FRAME_HEIGHT]
        println("Attempted to set to:$width,$height.  Actual result:$actualWidth,$actualHeight")
    }

    suspend fun capture(waitBeforeCapture: Duration = 750.milliseconds): Mat {
        require(camera.isOpened) { "Error: Could not open camera" }
        delay(waitBeforeCapture)
        require(camera.read(frame)) { "Error: Could not read frame" }
        return frame
    }

    suspend fun captureToFile(fileName: String, waitBeforeCapture: Duration = 750.milliseconds) {
        val fileNameWithExtension = if (fileName.endsWith(".png", true)) fileName else "$fileName.png"
        Imgcodecs.imwrite(fileNameWithExtension, capture(waitBeforeCapture))
        println("Image saved to $fileNameWithExtension")
    }

    override fun close() {
        camera.release()
    }
}
