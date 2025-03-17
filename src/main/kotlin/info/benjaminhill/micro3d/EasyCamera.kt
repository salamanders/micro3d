package info.benjaminhill.micro3d

import kotlinx.coroutines.delay
import org.bytedeco.javacpp.Loader
import org.bytedeco.javacv.FrameGrabber
import org.bytedeco.javacv.OpenCVFrameConverter
import org.bytedeco.javacv.OpenCVFrameGrabber
import org.bytedeco.opencv.opencv_java
import org.opencv.core.Mat
import org.opencv.imgcodecs.Imgcodecs
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds


class EasyCamera(cameraIndex: Int = 0) : AutoCloseable {
    //private val camera: VideoCapture
    private val grabber: FrameGrabber = OpenCVFrameGrabber(cameraIndex).also {
        // System.loadLibrary(org.opencv.core.Core.NATIVE_LIBRARY_NAME)
        Loader.load(opencv_java::class.java)
        it.start()
        println("Default camera resolution: ${it.imageWidth}x${it.imageHeight}")
    }
    private val bMatToMatConverter = OpenCVFrameConverter.ToOrgOpenCvCoreMat()

    suspend fun captureMat(waitBeforeCapture: Duration = 750.milliseconds): Mat {
        // require(camera.isOpened) { "Error: Could not open camera" }
        delay(waitBeforeCapture)
        return bMatToMatConverter.convertToOrgOpenCvCoreMat(grabber.grabFrame())
    }

    suspend fun captureToFile(fileName: String, waitBeforeCapture: Duration = 750.milliseconds) {
        val fileNameWithExtension = if (fileName.endsWith(".png", true)) fileName else "$fileName.png"
        Imgcodecs.imwrite(fileNameWithExtension, captureMat(waitBeforeCapture))
        println("Image saved to $fileNameWithExtension")
    }

    override fun close() {
        grabber.close()
    }
}
