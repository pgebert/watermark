package watermark

import java.awt.Color
import java.awt.Transparency
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

enum class PositionMethod {
    SINGLE,
    GRID
}

typealias Position = Pair<Int, Int>

fun main() {

    try {
        var useAlpha = false
        var transparencyColor: Color? = null
        var watermarkPosition: Position = Pair(0, 0)

        val image = promptImage()
        val watermark = promptWatermark(image)

        val hasAlphaChannel = watermark.colorModel.pixelSize == 32

        if (hasAlphaChannel) {
            useAlpha = promptWatermarkAlpha(watermark)
        } else {
            transparencyColor = promptTransparencyColor()
        }

        val weight = promptColorWeight()
        val positionMethod = promptPositionMethod()

        if (positionMethod == PositionMethod.SINGLE) {
            watermarkPosition = promptWatermarkPosition(watermark, image)
        }

        val outputFile = promptOutputFile()

        val watermarkedImage =
            blend(image, watermark, weight, useAlpha, transparencyColor, watermarkPosition, positionMethod)

        save(watermarkedImage, outputFile)


    } catch (e: Exception) {
        println(e.message)
    }

}


fun blend(
    image: BufferedImage,
    watermark: BufferedImage,
    weight: Int,
    useAlpha: Boolean,
    transparencyColor: Color?,
    watermarkPosition: Position,
    positionMethod: PositionMethod
): BufferedImage {

    val (xStart, yStart) = watermarkPosition

    for (x in 0 until image.width) {
        for (y in 0 until image.height) {

            if (positionMethod == PositionMethod.SINGLE
                && (x - xStart !in 0..watermark.width
                        || y - yStart !in 0..watermark.height)
            ) continue

            val i = Color(image.getRGB(x, y))
            val w = Color(watermark.getRGB((x - xStart) % watermark.width, (y - yStart) % watermark.height), useAlpha)

            val color = if ((useAlpha && w.alpha > 0) || (!useAlpha && w != transparencyColor)) {
                Color(
                    (weight * w.red + (100 - weight) * i.red) / 100,
                    (weight * w.green + (100 - weight) * i.green) / 100,
                    (weight * w.blue + (100 - weight) * i.blue) / 100
                )
            } else {
                i
            }
            image.setRGB(x, y, color.rgb)
        }
    }

    return image
}

fun promptImage(): BufferedImage {

    println("Input the image filename:")
    val file = File(readln())


    if (!file.exists()) throw Exception("The file ${file.path} doesn't exist.")

    val image = ImageIO.read(file)

    if (image.colorModel.numColorComponents != 3) throw Exception("The number of image color components isn't 3.")
    if (image.colorModel.pixelSize !in listOf(24, 32)) throw Exception("The image isn't 24 or 32-bit.")

    return image

}

fun promptWatermark(image: BufferedImage): BufferedImage {

    println("Input the watermark image filename:")
    val file = File(readln())


    if (!file.exists()) throw Exception("The file ${file.path} doesn't exist.")

    val watermark = ImageIO.read(file)

    if (watermark.colorModel.numColorComponents != 3) throw Exception("The number of watermark color components isn't 3.")
    if (watermark.colorModel.pixelSize !in listOf(24, 32)) throw Exception("The watermark isn't 24 or 32-bit.")

    if (watermark.height > image.height || watermark.width > image.width) throw Exception("The watermark's dimensions are larger.")

    return watermark

}

fun promptColorWeight(): Int {

    println("Input the watermark transparency percentage (Integer 0-100):")

    try {
        val weight = readln().toInt()

        if (weight !in 0..100) throw Exception("The transparency percentage is out of range.")

        return weight
    } catch (e: NumberFormatException) {
        throw Exception("The transparency percentage isn't an integer number.")
    }

}

fun promptWatermarkAlpha(watermark: BufferedImage): Boolean {

    if (watermark.transparency != Transparency.TRANSLUCENT) return false

    println("Do you want to use the watermark's Alpha channel?")

    return readln().lowercase() == "yes"

}

fun promptTransparencyColor(): Color? {

    println("Do you want to set a transparency color?")
    val answer = readln()

    if (answer != "yes") return null

    println("Input a transparency color ([Red] [Green] [Blue]):")

    try {
        val values = readln().split(" ").map { it.toInt() }

        if (values.size != 3 || values.any { it !in 0..255 }) throw Exception()

        return Color(values[0], values[1], values[2])
    } catch (e: Exception) {
        throw Exception("The transparency color input is invalid.")
    }

}

fun promptPositionMethod(): PositionMethod {

    println("Choose the position method (single, grid):")

    try {
        return PositionMethod.valueOf(readln().uppercase())
    } catch (e: Exception) {
        throw Exception("The position method input is invalid.")
    }

}

fun promptWatermarkPosition(watermark: BufferedImage, image: BufferedImage): Position {

    val maxXPosition = image.width - watermark.width
    val maxYPosition = image.height - watermark.height

    println("Input the watermark position ([x 0-$maxXPosition] [y 0-$maxYPosition]):")

    try {
        val values = readln().split(" ").map { it.toInt() }

        if (values.size != 2) throw Exception("The position input is invalid.")

        if (values.first() !in 0..maxXPosition
            || values.last() !in 0..maxYPosition
        ) {
            throw Exception("The position input is out of range.")
        }

        return Position(values.first(), values.last())
    } catch (e: NumberFormatException) {
        throw Exception("The position input is invalid.")
    }
}

fun promptOutputFile(): File {

    println("Input the output image filename (jpg or png extension):")
    val file = File(readln())

    if (file.extension !in listOf("jpg", "png")) throw Exception("The output file extension isn't \"jpg\" or \"png\".")

    return file
}

fun save(image: BufferedImage, file: File) {
    ImageIO.write(image, file.extension, file)
    println("The watermarked image ${file.path} has been created.")
}