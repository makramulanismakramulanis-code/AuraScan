package com.example.ui.screens.tools

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.ui.theme.AuraPrimary
import com.example.ads.AdService
import com.example.ui.components.RewardedAdModal
import com.example.util.PdfExporter
import java.io.File
import java.io.FileOutputStream

data class SignaturePath(
    val points: List<Offset>,
    val color: Color,
    val strokeWidth: Float
)

@Composable
fun SignatureTool(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var showRewardedModal by remember { mutableStateOf(false) }

    val paths = remember { mutableStateListOf<SignaturePath>() }
    var currentPoints = remember { mutableStateListOf<Offset>() }
    var selectedColor by remember { mutableStateOf(Color(0xFF0F172A)) }
    var strokeThickness by remember { mutableFloatStateOf(6f) }

    val colors = listOf(
        Color(0xFF0F172A), // Black / Dark Navy
        Color(0xFF1D4ED8), // Royal Blue
        Color(0xFFB91C1C), // Deep Red
        Color(0xFF047857)  // Forest Green
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("tool_signature")
    ) {
        // Header
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 2.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Digital Signature Studio",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Options: Color chips & thickness
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Color selection
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    colors.forEach { c ->
                        val isSel = selectedColor == c
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(c)
                                .border(
                                    width = if (isSel) 3.dp else 1.dp,
                                    color = if (isSel) AuraPrimary else Color.Gray.copy(alpha = 0.5f),
                                    shape = CircleShape
                                )
                                .clickable { selectedColor = c }
                        )
                    }
                }

                // Stroke width buttons
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(4f to "Thin", 7f to "Med", 11f to "Thick").forEach { (width, label) ->
                        val isSel = strokeThickness == width
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSel) AuraPrimary else MaterialTheme.colorScheme.surfaceVariant)
                                .clickable { strokeThickness = width }
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = label,
                                fontSize = 11.sp,
                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSel) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Signature Canvas Area (Transparent / White with subtle guideline)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White)
                    .border(1.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                    .pointerInput(selectedColor, strokeThickness) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                currentPoints.clear()
                                currentPoints.add(offset)
                            },
                            onDrag = { change, _ ->
                                change.consume()
                                currentPoints.add(change.position)
                            },
                            onDragEnd = {
                                if (currentPoints.isNotEmpty()) {
                                    paths.add(SignaturePath(currentPoints.toList(), selectedColor, strokeThickness))
                                    currentPoints.clear()
                                }
                            }
                        )
                    }
                    .testTag("signature_canvas")
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    // Subtle baseline
                    drawLine(
                        color = Color(0xFFCBD5E1),
                        start = Offset(40f, size.height * 0.75f),
                        end = Offset(size.width - 40f, size.height * 0.75f),
                        strokeWidth = 2f
                    )

                    // Draw completed paths
                    paths.forEach { path ->
                        for (i in 0 until path.points.size - 1) {
                            drawLine(
                                color = path.color,
                                start = path.points[i],
                                end = path.points[i + 1],
                                strokeWidth = path.strokeWidth,
                                cap = StrokeCap.Round
                            )
                        }
                    }

                    // Draw current live path
                    for (i in 0 until currentPoints.size - 1) {
                        drawLine(
                            color = selectedColor,
                            start = currentPoints[i],
                            end = currentPoints[i + 1],
                            strokeWidth = strokeThickness,
                            cap = StrokeCap.Round
                        )
                    }
                }

                if (paths.isEmpty() && currentPoints.isEmpty()) {
                    Text(
                        text = "Sign with finger or stylus here",
                        color = Color(0xFF94A3B8),
                        fontSize = 14.sp,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Bottom Actions: Clear and Download PNG
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        paths.clear()
                        currentPoints.clear()
                    },
                    modifier = Modifier.weight(1f).height(48.dp).testTag("signature_clear_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Clear, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Clear")
                }

                Button(
                    onClick = {
                        if (paths.isEmpty()) {
                            Toast.makeText(context, "Please sign on the canvas first", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        
                        if (AdService.shouldShowAds(context)) {
                            val (canWatch, _) = AdService.canWatchRewarded(context)
                            if (canWatch) {
                                showRewardedModal = true
                            } else {
                                generateAndShareSignaturePdf(context, paths.toList())
                            }
                        } else {
                            generateAndShareSignaturePdf(context, paths.toList())
                        }
                    },
                    modifier = Modifier.weight(1.4f).height(48.dp).testTag("signature_pdf_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = AuraPrimary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Download PDF", fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    if (showRewardedModal) {
        RewardedAdModal(
            onDismiss = { 
                showRewardedModal = false 
            },
            onRewardEarned = {
                showRewardedModal = false
                generateAndShareSignaturePdf(context, paths.toList())
            }
        )
    }
}

private fun generateAndShareSignaturePdf(context: Context, paths: List<SignaturePath>) {
    try {
        val bmp = exportSignatureToPng(context, paths)
        val bitmap = BitmapFactory.decodeFile(bmp.absolutePath)
        val pdfFile = PdfExporter.generateDocumentPdf(context, bitmap, "AuraScan Digital Signature")
        
        val uri: Uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", pdfFile)
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Save Signature PDF"))
    } catch (e: Exception) {
        Toast.makeText(context, "Failed to generate PDF: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}

private fun exportSignatureToPng(context: Context, paths: List<SignaturePath>): File {
    val w = 800
    val h = 450
    // Transparent background ARGB
    val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)
    canvas.drawColor(AndroidColor.TRANSPARENT)

    val paint = Paint().apply {
        isAntiAlias = true
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    paths.forEach { path ->
        paint.color = path.color.toArgb()
        paint.strokeWidth = path.strokeWidth * 1.5f
        for (i in 0 until path.points.size - 1) {
            canvas.drawLine(
                path.points[i].x,
                path.points[i].y,
                path.points[i + 1].x,
                path.points[i + 1].y,
                paint
            )
        }
    }

    val file = File(context.cacheDir, "AuraScan_Signature_${System.currentTimeMillis()}.png")
    FileOutputStream(file).use { out ->
        bmp.compress(Bitmap.CompressFormat.PNG, 100, out)
    }
    return file
}
