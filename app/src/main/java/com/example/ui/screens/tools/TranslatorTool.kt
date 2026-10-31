package com.example.ui.screens.tools

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.speech.tts.TextToSpeech
import android.widget.Toast
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import java.util.Locale

@Composable
fun TranslatorTool(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    var showRewardedModal by remember { mutableStateOf(false) }
    var pendingPdfAction by remember { mutableStateOf(false) }

    val languages = listOf(
        "Auto Detect", "English", "Hindi", "Tamil", "Telugu",
        "Kannada", "Bengali", "Marathi", "Gujarati", "Spanish", "French"
    )

    var sourceLang by remember { mutableStateOf("Auto Detect") }
    var targetLang by remember { mutableStateOf("Hindi") }
    var inputText by remember { mutableStateOf("") }
    var translatedText by remember { mutableStateOf("") }
    var isTranslating by remember { mutableStateOf(false) }

    var showSourceMenu by remember { mutableStateOf(false) }
    var showTargetMenu by remember { mutableStateOf(false) }

    // TTS Setup
    var tts by remember { mutableStateOf<TextToSpeech?>(null) }
    DisposableEffect(Unit) {
        val speech = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                // Initialized
            }
        }
        tts = speech
        onDispose {
            speech.stop()
            speech.shutdown()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("tool_translator")
    ) {
        // Tool Header
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
                    Icon(androidx.compose.material.icons.Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Universal Language Translator",
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
            // Language Selection Bar with Swap
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Source Dropdown Box
                Box(modifier = Modifier.weight(1f)) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { showSourceMenu = true }
                            .padding(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = sourceLang,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                        )
                    }
                    DropdownMenu(expanded = showSourceMenu, onDismissRequest = { showSourceMenu = false }) {
                        languages.forEach { lang ->
                            DropdownMenuItem(text = { Text(lang) }, onClick = {
                                sourceLang = lang
                                showSourceMenu = false
                            })
                        }
                    }
                }

                // Swap Button
                IconButton(
                    onClick = {
                        if (sourceLang != "Auto Detect") {
                            val temp = sourceLang
                            sourceLang = targetLang
                            targetLang = temp
                        }
                    },
                    modifier = Modifier.size(38.dp)
                ) {
                    Icon(Icons.Default.SwapHoriz, contentDescription = "Swap Languages", tint = AuraPrimary)
                }

                // Target Dropdown Box
                Box(modifier = Modifier.weight(1f)) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { showTargetMenu = true }
                            .padding(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = targetLang,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                        )
                    }
                    DropdownMenu(expanded = showTargetMenu, onDismissRequest = { showTargetMenu = false }) {
                        languages.filter { it != "Auto Detect" }.forEach { lang ->
                            DropdownMenuItem(text = { Text(lang) }, onClick = {
                                targetLang = lang
                                showTargetMenu = false
                            })
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Input Text Field
            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .testTag("translator_input_text"),
                placeholder = { Text("Enter or paste text to translate...") },
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Translate Now Button
            Button(
                onClick = {
                    if (inputText.isBlank()) {
                        Toast.makeText(context, "Please enter some text", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    isTranslating = true
                    scope.launch {
                        val res = GeminiService.translateText(inputText, sourceLang, targetLang)
                        res.onSuccess {
                            translatedText = it
                            isTranslating = false
                        }.onFailure { err ->
                            val msg = err.message ?: "Unknown Connection Error"
                            // For development, provide a result even on failure so PDF export can be tested
                            translatedText = "[DEMO MODE] Translated Output for: \"$inputText\"\nTarget: $targetLang\n\n(Note: This is a simulation because the AI service reported an error: $msg. Please ensure your Gemini API Key is set in the Secrets panel for real translations.)"
                            Toast.makeText(context, "AI Service Note: Using Demo Fallback", Toast.LENGTH_LONG).show()
                            isTranslating = false
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("translate_now_button"),
                colors = ButtonDefaults.buttonColors(containerColor = AuraPrimary),
                shape = RoundedCornerShape(12.dp),
                enabled = !isTranslating
            ) {
                if (isTranslating) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Default.Translate, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Translate Now", fontWeight = FontWeight.Bold)
                }
            }

            // Output Card
            if (translatedText.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .testTag("translated_text_card"),
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
                                text = "Translated ($targetLang)",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = AuraPrimary
                            )

                            Row {
                                // Listen Button
                                IconButton(
                                    onClick = {
                                        val langLocale = when (targetLang.lowercase()) {
                                            "hindi" -> Locale.forLanguageTag("hi-IN")
                                            "spanish" -> Locale.forLanguageTag("es-ES")
                                            "french" -> Locale.forLanguageTag("fr-FR")
                                            else -> Locale.ENGLISH
                                        }
                                        tts?.language = langLocale
                                        tts?.speak(translatedText, TextToSpeech.QUEUE_FLUSH, null, "tts_translate")
                                    },
                                    modifier = Modifier.size(34.dp)
                                ) {
                                    Icon(androidx.compose.material.icons.Icons.AutoMirrored.Filled.VolumeUp, contentDescription = "Listen", tint = AuraPrimary)
                                }

                                // Copy Button
                                IconButton(
                                    onClick = {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        clipboard.setPrimaryClip(ClipData.newPlainText("Translation", translatedText))
                                        Toast.makeText(context, "Translation copied!", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.size(34.dp)
                                ) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = AuraPrimary)
                                }

                                // Download PDF Button
                                IconButton(
                                    onClick = {
                                        if (AdService.shouldShowAds(context)) {
                                            val (canWatch, _) = AdService.canWatchRewarded(context)
                                            if (canWatch) {
                                                showRewardedModal = true
                                            } else {
                                                // If cannot watch (cooldown), just proceed
                                                generateAndShareTranslatorPdf(context, sourceLang, targetLang, inputText, translatedText)
                                            }
                                        } else {
                                            generateAndShareTranslatorPdf(context, sourceLang, targetLang, inputText, translatedText)
                                        }
                                    },
                                    modifier = Modifier.size(34.dp)
                                ) {
                                    Icon(Icons.Default.PictureAsPdf, contentDescription = "Download PDF", tint = AuraPrimary)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = translatedText,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
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
                generateAndShareTranslatorPdf(context, sourceLang, targetLang, inputText, translatedText)
            }
        )
    }
}

private fun generateAndShareTranslatorPdf(
    context: Context,
    source: String,
    target: String,
    original: String,
    translated: String
) {
    try {
        val content = "Source Language: $source\nTarget Language: $target\n\nORIGINAL TEXT:\n$original\n\nTRANSLATED TEXT:\n$translated"
        val pdfFile = PdfExporter.generateTextPdf(context, "AuraScan Translation", content)
        val uri: Uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", pdfFile)
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Save Translation PDF"))
    } catch (e: Exception) {
        Toast.makeText(context, "Failed to generate PDF: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}
