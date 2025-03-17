package info.benjaminhill.micro3d

import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.MatOfDouble
import org.opencv.imgproc.Imgproc

typealias OpenCVMat = org.opencv.core.Mat
// typealias BMat = org.bytedeco.opencv.opencv_core.Mat

object ImageUtils {

    /** For focus.  Higher is better. */
    fun calculateLaplacianVariance(inputMat: OpenCVMat): Double {
        val grayMat = OpenCVMat()
        // Check if the input is BGR (3 channels) or grayscale (1 channel)
        if (inputMat.channels() == 3) {
            Imgproc.cvtColor(inputMat, grayMat, Imgproc.COLOR_BGR2GRAY)
        } else {
            // If already grayscale, copy the input
            inputMat.copyTo(grayMat)
        }

        val dst = OpenCVMat()
        val depth = CvType.CV_16S // Use CV_16S to avoid overflow
        Imgproc.Laplacian(grayMat, dst, depth)

        val mean = MatOfDouble() // Create MatOfDouble objects
        val standardDev = MatOfDouble()

        Core.meanStdDev(dst, mean, standardDev)

        // Access the standard deviation value correctly
        val standardDevArray = standardDev.toArray()
        return standardDevArray[0] * standardDevArray[0] // Variance (stddev squared)
    }
}
