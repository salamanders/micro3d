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
        if (!camera.isOpened) {
            System.err.println("Error: Could not open camera")
            return
        }
        if (!camera.read(frame)) {
            System.err.println("Error: Could not read frame")
            return
        }
        Imgcodecs.imwrite("capture_$id.png", frame)
        println("Image saved to captured_image.png")
    }

    override fun close() {
        camera.release()
    }
}
