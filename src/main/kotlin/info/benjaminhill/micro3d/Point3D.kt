package info.benjaminhill.micro3d

data class Point3D(val x:Double, val y:Double, val z:Double) {
    override fun toString(): String {
        return "G1 X%.2f Y%.2f Z%.2f".format(x, y, z)
    }

    companion object {
        fun fromPosition(positionString:String) :Point3D {
            val positionRe = """X:([0-9.]+) Y:([0-9.]+) Z:([0-9.]+)""".toRegex()
            val (x, y, z) = positionRe.find(positionString)!!.groupValues.drop(1).map(String::toDouble)
            return Point3D(x, y, z)
        }
    }

}