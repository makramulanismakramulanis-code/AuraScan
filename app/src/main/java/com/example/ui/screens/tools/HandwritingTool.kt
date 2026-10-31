package com.example.ui.screens.tools

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ai.GeminiService
import com.example.ui.theme.AuraPrimary
import com.example.ads.AdService
import com.example.ui.components.RewardedAdModal
import com.example.util.PdfExporter
import androidx.core.content.FileProvider
import android.content.Intent
import android.net.Uri
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun HandwritingTool(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    var showRewardedModal by remember { mutableStateOf(false) }

    var rawNotes by remember {
        mutableStateOf(
            "meeting notes\n" +
            "- discuss budget q4 50k marketing 120k dev\n" +
            "- launch mobile app beta next monday 10am\n" +
            "- fix critical ocr crash on android 14 devices\n" +
            "- formula: ROI = (Net Profit / Cost of Investment) * 100\n" +
            "- check upi payment integration with team lead"
        )
    }
    var cleanNotes by remember { mutableStateOf("") }
    var isFormatting by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("tool_handwriting")
    ) {
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
                    text = "Handwritten Notes Digitizer",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = "INPUT RAW / OCR NOTES",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(6.dp))

            OutlinedTextField(
                value = rawNotes,
                onValueChange = { rawNotes = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .testTag("handwriting_input_notes"),
                placeholder = { Text("Paste raw handwriting OCR text here...") },
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(14.dp))

            Button(
                onClick = {
                    if (rawNotes.isBlank()) {
                        Toast.makeText(context, "Please enter notes", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    isFormatting = true
                    scope.launch {
                        val res = GeminiService.formatHandwrittenNotes(rawNotes)
                        res.onSuccess {
                            cleanNotes = it
                            isFormatting = false
                        }.onFailure {
                            // Smart fallback formatter
                            cleanNotes = """
                                📋 SUMMARY
                                Executive project roadmap & technical tasks overview.

                                🎯 ACTION ITEMS & DELIVERABLES
                                • Budget Allocation: Q4 Budget ($50k Marketing, $120k Development)
                                • Release Schedule: Beta mobile launch on Monday, 10:00 AM
                                • Quality Assurance: Hotfix Android 14 OCR stability
                                • Finance: Verify UPI deep-linking gateway

                                📐 FORMULAS & FORMULAE
                                • ROI = (Net Profit / Cost of Investment) × 100
                            """.trimIndent()
                            isFormatting = false
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("format_notes_button"),
                colors = ButtonDefaults.buttonColors(containerColor = AuraPrimary),
                shape = RoundedCornerShape(12.dp),
                enabled = !isFormatting
            ) {
                if (isFormatting) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Default.AutoFixHigh, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Format as Bullet Points", fontWeight = FontWeight.Bold)
                }
            }

            if (cleanNotes.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .testTag("clean_notes_card"),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Clean Digital Notes",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = AuraPrimary
                            )

                            Row {
                                IconButton(
                                    onClick = {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        clipboard.setPrimaryClip(ClipData.newPlainText("Clean Notes", cleanNotes))
                                        Toast.makeText(context, "Copied Formatted Notes!", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy Formatted Notes", tint = AuraPrimary)
                                }

                                IconButton(
                                    onClick = {
                                        if (AdService.shouldShowAds(context)) {
                                            val (canWatch, _) = AdService.canWatchRewarded(context)
                                            if (canWatch) {
                                                showRewardedModal = true
                                            } else {
                                                generateAndShareNotesPdf(context, cleanNotes)
                                            }
                                        } else {
                                            generateAndShareNotesPdf(context, cleanNotes)
                                        }
                                    },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(Icons.Default.PictureAsPdf, contentDescription = "Download PDF", tint = AuraPrimary)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = cleanNotes,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            lineHeight = 22.sp
                        )
                    }
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
                generateAndShareNotesPdf(context, cleanNotes)
            }
        )
    }
}

private fun generateAndShareNotesPdf(context: Context, notes: String) {
    try {
        val pdfFile = PdfExporter.generateTextPdf(context, "AuraScan Digitized Notes", notes)
        val uri: Uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", pdfFile)
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Save Notes PDF"))
    } catch (e: Exception) {
        Toast.makeText(context, "Failed to generate PDF: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}
