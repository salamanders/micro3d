package info.benjaminhill.micro3d

import nu.pattern.OpenCV
import org.opencv.core.Mat
import org.opencv.imgcodecs.Imgcodecs
import org.opencv.videoio.VideoCapture
import java.lang.AutoCloseable

class EasyCamera : AutoCloseable {
    private val camera: VideoCapture
    val frame: Mat

    init {
        OpenCV.loadLocally()
        camera = VideoCapture(0)
        frame = Mat()
    }

    fun capture(id: String) {
        require(camera.isOpened) { "Error: Could not open camera" }
        require(camera.read(frame)) { "Error: Could not read frame" }
        Imgcodecs.imwrite("capture_$id.png", frame)
        println("Image saved to captured_image.png")
    }

    override fun close() {
        camera.release()
    }
}
