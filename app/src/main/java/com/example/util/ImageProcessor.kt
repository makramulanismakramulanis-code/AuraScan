package com.example.util

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Rect
import com.example.model.ImageFilterType
import kotlin.math.min

object ImageProcessor {

    fun resizeBitmap(bitmap: Bitmap, maxDim: Int = 1600): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        if (width <= maxDim && height <= maxDim) return bitmap

        val ratio = width.toFloat() / height.toFloat()
        val targetWidth: Int
        val targetHeight: Int
        if (width > height) {
            targetWidth = maxDim
            targetHeight = (maxDim / ratio).toInt()
        } else {
            targetHeight = maxDim
            targetWidth = (maxDim * ratio).toInt()
        }
        return Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true)
    }

    fun rotateBitmap(bitmap: Bitmap, degrees: Float = 90f): Bitmap {
        val matrix = Matrix().apply { postRotate(degrees) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    fun applyFilter(bitmap: Bitmap, filter: ImageFilterType): Bitmap {
        return when (filter) {
            ImageFilterType.ORIGINAL -> bitmap
            ImageFilterType.GRAYSCALE -> toGrayscale(bitmap)
            ImageFilterType.BW -> toBlackAndWhite(bitmap)
            ImageFilterType.MAGIC_COLOR -> toMagicColor(bitmap)
            ImageFilterType.DOCUMENT -> toDocumentFilter(bitmap)
        }
    }

    private fun toGrayscale(src: Bitmap): Bitmap {
        val result = Bitmap.createBitmap(src.width, src.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        val paint = Paint()
        val matrix = ColorMatrix().apply { setSaturation(0f) }
        paint.colorFilter = ColorMatrixColorFilter(matrix)
        canvas.drawBitmap(src, 0f, 0f, paint)
        return result
    }

    private fun toBlackAndWhite(src: Bitmap): Bitmap {
        val gray = toGrayscale(src)
        val result = Bitmap.createBitmap(gray.width, gray.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        val paint = Paint()

        // High contrast matrix threshold
        val cm = ColorMatrix(
            floatArrayOf(
                3.5f, 0f, 0f, 0f, -250f,
                0f, 3.5f, 0f, 0f, -250f,
                0f, 0f, 3.5f, 0f, -250f,
                0f, 0f, 0f, 1f, 0f
            )
        )
        paint.colorFilter = ColorMatrixColorFilter(cm)
        canvas.drawBitmap(gray, 0f, 0f, paint)
        return result
    }

    private fun toMagicColor(src: Bitmap): Bitmap {
        val result = Bitmap.createBitmap(src.width, src.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        val paint = Paint()

        // Boost saturation and mild contrast/brightness
        val satMatrix = ColorMatrix().apply { setSaturation(1.35f) }
        val contrast = 1.2f
        val translate = (-0.5f * contrast + 0.5f) * 255f + 15f
        val contrastMatrix = ColorMatrix(
            floatArrayOf(
                contrast, 0f, 0f, 0f, translate,
                0f, contrast, 0f, 0f, translate,
                0f, 0f, contrast, 0f, translate,
                0f, 0f, 0f, 1f, 0f
            )
        )
        contrastMatrix.postConcat(satMatrix)
        paint.colorFilter = ColorMatrixColorFilter(contrastMatrix)
        canvas.drawBitmap(src, 0f, 0f, paint)
        return result
    }

    private fun toDocumentFilter(src: Bitmap): Bitmap {
        val result = Bitmap.createBitmap(src.width, src.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        val paint = Paint()

        // Lighten paper background, enhance black text
        val cm = ColorMatrix(
            floatArrayOf(
                1.5f, 0f, 0f, 0f, -40f,
                0f, 1.5f, 0f, 0f, -40f,
                0f, 0f, 1.5f, 0f, -40f,
                0f, 0f, 0f, 1f, 0f
            )
        )
        paint.colorFilter = ColorMatrixColorFilter(cm)
        canvas.drawBitmap(src, 0f, 0f, paint)
        return result
    }

    fun maskAadhaarNumber(src: Bitmap, maskRectRatio: Rect? = null): Bitmap {
        val copy = src.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(copy)
        val paint = Paint().apply {
            color = Color.BLACK
            style = Paint.Style.FILL
        }

        if (maskRectRatio != null) {
            canvas.drawRect(maskRectRatio, paint)
        } else {
            // Default Aadhaar mask box: bottom-center area where Aadhaar 12 digits appear (blacking out first 8 digits)
            val left = (copy.width * 0.25f).toInt()
            val right = (copy.width * 0.65f).toInt()
            val top = (copy.height * 0.72f).toInt()
            val bottom = (copy.height * 0.82f).toInt()
            canvas.drawRect(Rect(left, top, right, bottom), paint)
        }
        return copy
    }
}
