package info.benjaminhill.micro3d

import nu.pattern.OpenCV
import org.opencv.core.Mat
import org.opencv.imgcodecs.Imgcodecs
import org.opencv.videoio.VideoCapture

class EasyCamera : AutoCloseable {
    private val camera: VideoCapture
    private val frame: Mat

    init {
        OpenCV.loadLocally()
        camera = VideoCapture(0)
        frame = Mat()
    }

    fun capture(fileName: String) {
        val fileNameWithExtension = if (fileName.endsWith(".png", true)) fileName else "$fileName.png"
        require(camera.isOpened) { "Error: Could not open camera" }
        require(camera.read(frame)) { "Error: Could not read frame" }
        Imgcodecs.imwrite(fileNameWithExtension, frame)
        println("Image saved to $fileNameWithExtension")
    }

    override fun close() {
        camera.release()
    }
}
