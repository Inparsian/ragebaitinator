package sn.inpr.ragebaitinator.utility

import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.sin

object MathUtil {
    fun pages(size: Double, pageSize: Int): Int = ceil(size / pageSize).toInt()
    fun pages(size: Int, pageSize: Int): Int = pages(size.toDouble(), pageSize)

    /**
     * Returns the trigonometric sine of an angle converted to radians. Equivalent to Math.sin(Math.toRadians(angle)).
     * @param angle Angle in degrees
     * @return Sine of the angle converted to radians
     */
    fun rsin(angle: Double): Double = sin(Math.toRadians(angle))

    /**
     * Returns the trigonometric cosine of an angle converted to radians. Equivalent to Math.cos(Math.toRadians(angle)).
     * @param angle Angle in degrees
     * @return Cosine of the angle converted to radians
     */
    fun rcos(angle: Double): Double = cos(Math.toRadians(angle))

    fun rsin(angle: Int): Double = rsin(angle.toDouble())
    fun rcos(angle: Int): Double = rcos(angle.toDouble())
    fun rsin(angle: Float): Float = sin(Math.toRadians(angle.toDouble())).toFloat()
    fun rcos(angle: Float): Float = cos(Math.toRadians(angle.toDouble())).toFloat()

    fun random(min: Double, max: Double): Double = min + (max - min) * Math.random()
    fun random(min: Int, max: Int): Int = min + ((max - min) * Math.random()).toInt()
    fun random(min: Long, max: Long): Long = min + ((max - min) * Math.random()).toLong()
    fun random(min: Float, max: Float): Float = min + (max - min) * Math.random().toFloat()
    fun randomBool(threshold: Double = 0.5): Boolean = Math.random() < threshold
    fun randomBool(): Boolean = randomBool(0.5)
}