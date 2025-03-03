package info.benjaminhill.micro3d

import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.MatOfDouble
import org.opencv.imgproc.Imgproc

object Focus {
    /** For focus.  Higher is better. */
    fun calculateLaplacianVariance(inputMat: Mat): Double {
        val grayMat = Mat()
        // Check if the input is BGR (3 channels) or grayscale (1 channel)
        if (inputMat.channels() == 3) {
            Imgproc.cvtColor(inputMat, grayMat, Imgproc.COLOR_BGR2GRAY)
        } else {
            // If already grayscale, copy the input
            inputMat.copyTo(grayMat)
        }

        val dst = Mat()
        val depth = CvType.CV_16S // Use CV_16S to avoid overflow
        Imgproc.Laplacian(grayMat, dst, depth)

        val mean = MatOfDouble() // Create MatOfDouble objects
        val stddev = MatOfDouble()

        Core.meanStdDev(dst, mean, stddev)

        // Access the standard deviation value correctly
        val stddevArray = stddev.toArray()
        return stddevArray[0] * stddevArray[0] // Variance (stddev squared)
    }
}