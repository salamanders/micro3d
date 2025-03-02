package info.benjaminhill.micro3d

import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.MatOfDouble
import org.opencv.imgcodecs.Imgcodecs
import org.opencv.imgproc.Imgproc
import java.io.File
import kotlin.math.pow

class Focus {

    /** For focus.  Higher is better. */
    fun calculateLaplacianVariance(imageFile: File): Double {
        val src: Mat = Imgcodecs.imread(imageFile.absolutePath, Imgcodecs.IMREAD_GRAYSCALE)
        val dst = Mat()
        Imgproc.Laplacian(src, dst, CvType.CV_64F)

        val mean = MatOfDouble() // Create MatOfDouble objects
        val stddev = MatOfDouble()

        Core.meanStdDev(dst, mean, stddev)

        // Access the standard deviation value correctly
        val stddevArray = stddev.toArray()
        return stddevArray[0] * stddevArray[0] // Variance (stddev squared)
    }
}