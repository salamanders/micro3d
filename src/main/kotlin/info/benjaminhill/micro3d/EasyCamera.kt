package info.benjaminhill.micro3d

import nu.pattern.OpenCV
import org.opencv.core.Mat
import org.opencv.imgcodecs.Imgcodecs
import org.opencv.videoio.VideoCapture
import org.opencv.videoio.Videoio.CAP_PROP_FRAME_HEIGHT
import org.opencv.videoio.Videoio.CAP_PROP_FRAME_WIDTH

class EasyCamera(cameraIndex: Int = 0) : AutoCloseable {
    private val camera: VideoCapture
    private val frame: Mat

    init {
        OpenCV.loadLocally()

        camera = VideoCapture(cameraIndex)
        frame = Mat()
    }

    fun setResolution(width: Int, height: Int) {
        require(camera.set(CAP_PROP_FRAME_WIDTH, width.toDouble()))
        require(camera.set(CAP_PROP_FRAME_HEIGHT, height.toDouble()))
    }

    fun capture(): Mat {
        require(camera.isOpened) { "Error: Could not open camera" }
        require(camera.read(frame)) { "Error: Could not read frame" }
        return frame
    }

    fun capture(fileName: String) {
        val fileNameWithExtension = if (fileName.endsWith(".png", true)) fileName else "$fileName.png"
        Imgcodecs.imwrite(fileNameWithExtension, capture())
        println("Image saved to $fileNameWithExtension")
    }

    override fun close() {
        camera.release()
    }
}
