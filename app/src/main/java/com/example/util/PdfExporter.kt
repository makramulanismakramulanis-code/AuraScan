package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.example.model.MarkSheetData
import java.io.File
import java.io.FileOutputStream
import java.util.Locale

enum class PdfPageSize(val label: String, val widthPt: Int, val heightPt: Int) {
    A4("A4 (210 x 297 mm)", 595, 842),
    LETTER("US Letter", 612, 792),
    FIT("Fit to Image", 0, 0)
}

enum class PdfQuality(val label: String, val coinCost: Int) {
    STANDARD("Standard Quality", 0),
    HD("HD High Definition (50 Coins)", 50)
}

object PdfExporter {

    fun generateDocumentPdf(
        context: Context,
        bitmap: Bitmap,
        title: String,
        pageSize: PdfPageSize = PdfPageSize.A4,
        quality: PdfQuality = PdfQuality.STANDARD
    ): File {
        val pdfDoc = PdfDocument()

        val pw: Int
        val ph: Int
        if (pageSize == PdfPageSize.FIT) {
            pw = bitmap.width.coerceAtMost(1200)
            ph = ((pw.toFloat() / bitmap.width.toFloat()) * bitmap.height).toInt()
        } else {
            pw = pageSize.widthPt
            ph = pageSize.heightPt
        }

        val pageInfo = PdfDocument.PageInfo.Builder(pw, ph, 1).create()
        val page = pdfDoc.startPage(pageInfo)
        val canvas = page.canvas

        // Background white
        canvas.drawColor(Color.WHITE)

        val margin = if (pageSize == PdfPageSize.FIT) 0f else 36f
        val printableW = pw - (margin * 2)
        val printableH = ph - (margin * 2)

        val scale = minOf(printableW / bitmap.width, printableH / bitmap.height)
        val destW = bitmap.width * scale
        val destH = bitmap.height * scale
        val left = margin + (printableW - destW) / 2f
        val top = margin + (printableH - destH) / 2f

        val paint = Paint().apply {
            isAntiAlias = true
            isFilterBitmap = true
        }

        val destRect = RectF(left, top, left + destW, top + destH)
        canvas.drawBitmap(bitmap, null, destRect, paint)

        pdfDoc.finishPage(page)

        val cleanTitle = title.replace("[^a-zA-Z0-9_-]".toRegex(), "_").take(30)
        val outFile = File(context.cacheDir, "AuraScan_${cleanTitle}_${System.currentTimeMillis()}.pdf")
        FileOutputStream(outFile).use { out ->
            pdfDoc.writeTo(out)
        }
        pdfDoc.close()
        return outFile
    }

    fun generateMarkSheetPdf(
        context: Context,
        data: MarkSheetData,
        totalScore: Int,
        maxScore: Int,
        percentage: Double,
        grade: String
    ): File {
        val pdfDoc = PdfDocument()
        val pw = 595
        val ph = 842
        val pageInfo = PdfDocument.PageInfo.Builder(pw, ph, 1).create()
        val page = pdfDoc.startPage(pageInfo)
        val canvas = page.canvas

        canvas.drawColor(Color.WHITE)

        val paint = Paint().apply { isAntiAlias = true }

        // Header Banner
        paint.color = Color.parseColor("#4361EE")
        paint.style = Paint.Style.FILL
        canvas.drawRect(0f, 0f, pw.toFloat(), 48f, paint)

        // Title
        paint.color = Color.WHITE
        paint.textSize = 20f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("ACADEMIC MARK SHEET RECORD", 40f, 32f, paint)

        var y = 80f

        // Board / Council
        paint.color = Color.DKGRAY
        paint.textSize = 12f
        paint.typeface = Typeface.DEFAULT
        canvas.drawText("Board / University: ${data.boardCouncil.ifEmpty { "General Council" }}", 40f, y, paint)
        y += 24f

        // Student Info Box
        paint.color = Color.parseColor("#F1F5F9")
        canvas.drawRoundRect(RectF(40f, y, (pw - 40).toFloat(), y + 60f), 8f, 8f, paint)

        paint.color = Color.parseColor("#1E293B")
        paint.textSize = 14f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("Student Name: ${data.studentName.ifEmpty { "N/A" }}", 56f, y + 26f, paint)
        canvas.drawText("Roll Number: ${data.rollNumber.ifEmpty { "N/A" }}", 56f, y + 48f, paint)

        y += 84f

        // Table Header
        paint.color = Color.parseColor("#4361EE")
        canvas.drawRoundRect(RectF(40f, y, (pw - 40).toFloat(), y + 30f), 6f, 6f, paint)

        paint.color = Color.WHITE
        paint.textSize = 13f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("Subject", 56f, y + 20f, paint)
        canvas.drawText("Max Marks", 340f, y + 20f, paint)
        canvas.drawText("Marks Obtained", 440f, y + 20f, paint)

        y += 30f

        // Subjects rows
        paint.typeface = Typeface.DEFAULT
        data.subjects.forEachIndexed { index, sub ->
            paint.color = if (index % 2 == 0) Color.parseColor("#F8FAFC") else Color.WHITE
            canvas.drawRect(40f, y, (pw - 40).toFloat(), y + 28f, paint)

            paint.color = Color.parseColor("#1E293B")
            paint.textSize = 12f
            canvas.drawText(sub.name, 56f, y + 19f, paint)
            canvas.drawText("${sub.maxMarks}", 350f, y + 19f, paint)
            canvas.drawText("${sub.marksObtained}", 460f, y + 19f, paint)

            // Divider
            paint.color = Color.parseColor("#E2E8F0")
            canvas.drawLine(40f, y + 28f, (pw - 40).toFloat(), y + 28f, paint)

            y += 28f
        }

        y += 20f

        // Result Summary Card
        paint.color = Color.parseColor("#EEF2FF")
        canvas.drawRoundRect(RectF(40f, y, (pw - 40).toFloat(), y + 70f), 8f, 8f, paint)

        paint.color = Color.parseColor("#3F37C9")
        paint.textSize = 14f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("TOTAL SCORE: $totalScore / $maxScore", 56f, y + 26f, paint)
        canvas.drawText("PERCENTAGE: ${String.format(Locale.US, "%.2f", percentage)}%", 56f, y + 50f, paint)

        paint.color = Color.parseColor("#10B981")
        paint.textSize = 16f
        canvas.drawText("GRADE: $grade", 360f, y + 38f, paint)

        // Mandatory Disclaimer Footer
        paint.color = Color.parseColor("#94A3B8")
        paint.textSize = 10f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
        val footerText = "Digital copy for personal reference. Not an official document."
        val textWidth = paint.measureText(footerText)
        canvas.drawText(footerText, (pw - textWidth) / 2f, ph - 30f, paint)

        pdfDoc.finishPage(page)

        val outFile = File(context.cacheDir, "AuraScan_Marksheet_${System.currentTimeMillis()}.pdf")
        FileOutputStream(outFile).use { out ->
            pdfDoc.writeTo(out)
        }
        pdfDoc.close()
        return outFile
    }

    fun generateMergedIdCardPdf(
        context: Context,
        frontBitmap: Bitmap,
        backBitmap: Bitmap
    ): File {
        val pdfDoc = PdfDocument()
        val pw = 595
        val ph = 842
        val pageInfo = PdfDocument.PageInfo.Builder(pw, ph, 1).create()
        val page = pdfDoc.startPage(pageInfo)
        val canvas = page.canvas

        canvas.drawColor(Color.WHITE)

        val paint = Paint().apply { isAntiAlias = true }

        // Header Title
        paint.color = Color.parseColor("#4361EE")
        paint.textSize = 18f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("2-SIDED ID CARD - PRINT READY", 50f, 50f, paint)

        paint.color = Color.GRAY
        paint.textSize = 11f
        paint.typeface = Typeface.DEFAULT
        canvas.drawText("Standard ID-1 format (85.6 mm x 54.0 mm) scaled for 1:1 printing", 50f, 70f, paint)

        val cardW = 243f
        val cardH = 153f
        val centerX = (pw - cardW) / 2f

        // Front Card
        var yPos = 120f
        paint.color = Color.parseColor("#1E293B")
        paint.textSize = 13f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("FRONT SIDE", centerX, yPos - 12f, paint)

        val frontDest = RectF(centerX, yPos, centerX + cardW, yPos + cardH)
        canvas.drawBitmap(frontBitmap, null, frontDest, paint)

        // Card Border
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1.5f
        paint.color = Color.parseColor("#CBD5E1")
        canvas.drawRoundRect(frontDest, 12f, 12f, paint)

        // Back Card
        yPos = 340f
        paint.style = Paint.Style.FILL
        paint.color = Color.parseColor("#1E293B")
        paint.textSize = 13f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("BACK SIDE", centerX, yPos - 12f, paint)

        val backDest = RectF(centerX, yPos, centerX + cardW, yPos + cardH)
        canvas.drawBitmap(backBitmap, null, backDest, paint)

        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1.5f
        paint.color = Color.parseColor("#CBD5E1")
        canvas.drawRoundRect(backDest, 12f, 12f, paint)

        // Cut Guides
        paint.style = Paint.Style.FILL
        paint.color = Color.parseColor("#64748B")
        paint.textSize = 10f
        paint.typeface = Typeface.DEFAULT
        canvas.drawText("✂ Cut along the rounded borders. Laminate or insert into standard card wallet.", 50f, 540f, paint)

        // Footer
        paint.color = Color.parseColor("#94A3B8")
        canvas.drawText("Generated via AuraScan Smart AI Document Scanner", 50f, ph - 40f, paint)

        pdfDoc.finishPage(page)

        val outFile = File(context.cacheDir, "AuraScan_ID_Card_${System.currentTimeMillis()}.pdf")
        FileOutputStream(outFile).use { out ->
            pdfDoc.writeTo(out)
        }
        pdfDoc.close()
        return outFile
    }
}
