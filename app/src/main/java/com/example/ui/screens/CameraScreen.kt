package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.launch
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.model.DocumentType
import com.example.ui.theme.AuraPrimary
import com.example.util.ImageProcessor

@Composable
fun CameraScreen(
    initialMode: DocumentType,
    onClose: () -> Unit,
    onImageCaptured: (Bitmap, DocumentType) -> Unit
) {
    val context = LocalContext.current
    var selectedMode by remember { mutableStateOf(initialMode) }
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    fun vibrateDevice() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator?.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                @Suppress("DEPRECATION")
                vibrator?.vibrate(50)
            }
        } catch (_: Exception) {}
    }

    // Photo Picker (zero permissions needed on Android)
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    val bitmap = BitmapFactory.decodeStream(stream)
                    if (bitmap != null) {
                        vibrateDevice()
                        val resized = ImageProcessor.resizeBitmap(bitmap, 1600)
                        onImageCaptured(resized, selectedMode)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Camera Preview capture launcher
    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            vibrateDevice()
            val resized = ImageProcessor.resizeBitmap(bitmap, 1600)
            onImageCaptured(resized, selectedMode)
        }
    }

    // Permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
        if (granted) {
            takePictureLauncher.launch()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .testTag("camera_screen")
    ) {
        // Simulated Camera Viewfinder Grid / Frame
        Canvas(modifier = Modifier.fillMaxSize()) {
            val marginH = size.width * 0.1f
            val marginV = size.height * 0.2f
            val frameW = size.width - (marginH * 2)
            val frameH = size.height - (marginV * 2)

            // Draw dark vignette around the document box
            drawRoundRect(
                color = Color.White.copy(alpha = 0.85f),
                topLeft = Offset(marginH, marginV),
                size = Size(frameW, frameH),
                cornerRadius = CornerRadius(16.dp.toPx(), 16.dp.toPx()),
                style = Stroke(width = 3.dp.toPx())
            )

            // Corner accents
            val cornerLen = 24.dp.toPx()
            val strokeW = 5.dp.toPx()
            // Top Left
            drawLine(AuraPrimary, Offset(marginH - 2, marginV), Offset(marginH + cornerLen, marginV), strokeW)
            drawLine(AuraPrimary, Offset(marginH, marginV - 2), Offset(marginH, marginV + cornerLen), strokeW)
            // Top Right
            drawLine(AuraPrimary, Offset(marginH + frameW - cornerLen, marginV), Offset(marginH + frameW + 2, marginV), strokeW)
            drawLine(AuraPrimary, Offset(marginH + frameW, marginV - 2), Offset(marginH + frameW, marginV + cornerLen), strokeW)
            // Bottom Left
            drawLine(AuraPrimary, Offset(marginH - 2, marginV + frameH), Offset(marginH + cornerLen, marginV + frameH), strokeW)
            drawLine(AuraPrimary, Offset(marginH, marginV + frameH - cornerLen), Offset(marginH, marginV + frameH + 2), strokeW)
            // Bottom Right
            drawLine(AuraPrimary, Offset(marginH + frameW - cornerLen, marginV + frameH), Offset(marginH + frameW + 2, marginV + frameH), strokeW)
            drawLine(AuraPrimary, Offset(marginH + frameW, marginV + frameH - cornerLen), Offset(marginH + frameW, marginV + frameH + 2), strokeW)
        }

        // Top Overlay Controls
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 40.dp, start = 16.dp, end = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onClose,
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.5f))
                    .testTag("camera_close_button")
            ) {
                Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
            }

            // Mode Indicator Capsule
            Surface(
                color = Color.Black.copy(alpha = 0.6f),
                shape = RoundedCornerShape(20.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.3f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DocumentType.entries.filter { it != DocumentType.MARKSHEET }.forEach { mode ->
                        val isSel = selectedMode == mode
                        Text(
                            text = mode.label,
                            fontSize = 12.sp,
                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSel) AuraPrimary else Color.White.copy(alpha = 0.7f),
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { selectedMode = mode }
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            IconButton(
                onClick = {
                    if (hasCameraPermission) {
                        takePictureLauncher.launch()
                    } else {
                        permissionLauncher.launch(Manifest.permission.CAMERA)
                    }
                },
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.5f))
            ) {
                Icon(Icons.Default.Cameraswitch, contentDescription = "Switch Camera", tint = Color.White)
            }
        }

        // Center Guide Text
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(bottom = 120.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "ALIGN DOCUMENT HERE",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
                color = Color.White.copy(alpha = 0.9f)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Hold still for sharp text & auto-crop",
                fontSize = 11.sp,
                color = Color.White.copy(alpha = 0.6f)
            )
        }

        // Bottom Controls Bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .background(Color.Black.copy(alpha = 0.85f))
                .padding(horizontal = 24.dp, vertical = 24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Gallery Picker
                IconButton(
                    onClick = {
                        photoPickerLauncher.launch(
                            androidx.activity.result.PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.2f))
                        .testTag("camera_gallery_picker")
                ) {
                    Icon(
                        imageVector = Icons.Default.PhotoLibrary,
                        contentDescription = "Gallery",
                        tint = Color.White,
                        modifier = Modifier.size(26.dp)
                    )
                }

                // Shutter Button
                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .border(4.dp, Color.White, CircleShape)
                        .padding(6.dp)
                        .clip(CircleShape)
                        .background(AuraPrimary)
                        .clickable {
                            if (hasCameraPermission) {
                                takePictureLauncher.launch()
                            } else {
                                // Direct permission prompt or mock capture sample document for emulator testability
                                permissionLauncher.launch(Manifest.permission.CAMERA)
                            }
                        }
                        .testTag("camera_shutter_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                    )
                }

                // Mode label / Mock sample loader (useful for testing on emulator without physical camera)
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White.copy(alpha = 0.15f),
                    modifier = Modifier
                        .clickable {
                            // Create a realistic sample bill bitmap for immediate testing on emulator
                            vibrateDevice()
                            val sample = createSampleTestBitmap(selectedMode)
                            onImageCaptured(sample, selectedMode)
                        }
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                        .testTag("camera_mode_indicator")
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = selectedMode.label,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Auto-Doc",
                            fontSize = 9.sp,
                            color = Color.White.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Creates a crisp high-res sample document bitmap for instant testing on emulators
 */
private fun createSampleTestBitmap(type: DocumentType): Bitmap {
    val w = 900
    val h = 1200
    val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    canvas.drawColor(AndroidColor.WHITE)

    val paint = Paint().apply {
        isAntiAlias = true
        color = AndroidColor.BLACK
    }

    // Header Banner
    paint.color = AndroidColor.parseColor("#4361EE")
    canvas.drawRect(0f, 0f, w.toFloat(), 120f, paint)

    paint.color = AndroidColor.WHITE
    paint.textSize = 36f
    paint.isFakeBoldText = true
    val title = when (type) {
        DocumentType.BILL -> "TAX INVOICE / GST BILL"
        DocumentType.ID_CARD -> "GOVERNMENT ID CARD"
        DocumentType.NOTES -> "ENGINEERING LECTURE NOTES"
        else -> "OFFICIAL DOCUMENT RECORD"
    }
    canvas.drawText(title, 50f, 75f, paint)

    // Body Lines
    paint.color = AndroidColor.DKGRAY
    paint.textSize = 26f
    paint.isFakeBoldText = false

    var y = 200f
    val lines = when (type) {
        DocumentType.BILL -> listOf(
            "Merchant: Fresh Mart Organics Ltd.",
            "GSTIN: 27AABCF1234F1Z9",
            "Date: 2026-10-30   Time: 14:22",
            "--------------------------------------------------",
            "1. Premium Basmati Rice 5kg     ₹ 450.00",
            "2. Farm Fresh Butter 500g        ₹ 260.00",
            "3. Organic Green Tea 250g        ₹ 180.00",
            "4. Almond Milk Carton            ₹ 160.00",
            "--------------------------------------------------",
            "Subtotal:                       ₹ 1,050.00",
            "CGST (2.5%):                     ₹    26.25",
            "SGST (2.5%):                     ₹    26.25",
            "Grand Total:                    ₹ 1,102.50",
            "Payment Method: UPI / GPay Paid",
            "Thank you for shopping with us!"
        )
        DocumentType.ID_CARD -> listOf(
            "REPUBLIC OF INDIA",
            "Unique Identification Authority",
            "Name: Rahul Sharma",
            "DOB: 15/08/1995",
            "Gender: Male",
            "Aadhaar No: 2345 6789 1234",
            "Address: 42 Palm Avenue, Bengaluru, Karnataka",
            "PIN: 560001"
        )
        DocumentType.NOTES -> listOf(
            "Topic: Artificial Intelligence & Neural Networks",
            "1. Perceptron architecture and activation functions (ReLU, Sigmoid).",
            "2. Gradient Descent optimization formula: W = W - alpha * dL/dW.",
            "3. Convolutional filters for 2D feature extraction in OCR.",
            "Action Item: Finish assignment #4 before Friday 5 PM."
        )
        else -> listOf(
            "CERTIFICATE OF COMPLETION",
            "This certifies that Rahul Sharma has successfully completed",
            "the Advanced Mobile Architecture Program with distinction.",
            "Issued: October 2026",
            "Verification Code: AS-9843-2026"
        )
    }

    lines.forEach { line ->
        canvas.drawText(line, 50f, y, paint)
        y += 45f
    }

    return bitmap
}
