package com.example.ui.screens.tools

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.ai.GeminiService
import com.example.model.MarkSheetData
import com.example.model.SubjectScore
import com.example.ui.theme.AuraPrimary
import com.example.util.PdfExporter
import kotlinx.coroutines.launch
import java.io.File
import java.util.Locale

@Composable
fun MarkSheetTool(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var studentName by remember { mutableStateOf("Rahul Sharma") }
    var rollNumber by remember { mutableStateOf("AS-2026-904") }
    var boardCouncil by remember { mutableStateOf("Central Board of Secondary Education") }

    var subjects by remember {
        mutableStateOf(
            listOf(
                SubjectScore(name = "English Language", maxMarks = 100, marksObtained = 88),
                SubjectScore(name = "Mathematics", maxMarks = 100, marksObtained = 94),
                SubjectScore(name = "Science", maxMarks = 100, marksObtained = 91),
                SubjectScore(name = "Social Studies", maxMarks = 100, marksObtained = 86),
                SubjectScore(name = "Second Language (Hindi)", maxMarks = 100, marksObtained = 90)
            )
        )
    }

    var isScanning by remember { mutableStateOf(false) }

    // Calculated Scores
    val totalMax = subjects.sumOf { it.maxMarks }
    val totalObtained = subjects.sumOf { it.marksObtained }
    val percentage = if (totalMax > 0) (totalObtained.toDouble() / totalMax.toDouble()) * 100 else 0.0

    val gradeLabel = when {
        percentage >= 85.0 -> "Distinction"
        percentage >= 70.0 -> "First Class"
        percentage >= 60.0 -> "Second Class"
        percentage >= 40.0 -> "Pass"
        else -> "Needs Improvement"
    }

    // Photo picker to auto fill from scan
    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            isScanning = true
            scope.launch {
                try {
                    context.contentResolver.openInputStream(uri)?.use { stream ->
                        val bitmap = android.graphics.BitmapFactory.decodeStream(stream)
                        if (bitmap != null) {
                            val res = GeminiService.parseMarkSheetFromImage(bitmap)
                            res.onSuccess { data ->
                                if (data.studentName.isNotEmpty()) studentName = data.studentName
                                if (data.rollNumber.isNotEmpty()) rollNumber = data.rollNumber
                                if (data.boardCouncil.isNotEmpty()) boardCouncil = data.boardCouncil
                                if (data.subjects.isNotEmpty()) subjects = data.subjects
                            }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    isScanning = false
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("tool_marksheet")
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
                    text = "Mark Sheet Formatter",
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
            // Auto Fill from Scan Button
            OutlinedButton(
                onClick = { photoPicker.launch("image/*") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .testTag("scan_marksheet_button"),
                shape = RoundedCornerShape(10.dp)
            ) {
                if (isScanning) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = AuraPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Scanning mark sheet with AI...")
                } else {
                    Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp), tint = AuraPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Auto-Fill from Mark Sheet Photo", color = AuraPrimary)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Student Info Inputs
            OutlinedTextField(
                value = studentName,
                onValueChange = { studentName = it },
                label = { Text("Student Name") },
                modifier = Modifier.fillMaxWidth().testTag("student_name_input"),
                singleLine = true,
                shape = RoundedCornerShape(10.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = rollNumber,
                    onValueChange = { rollNumber = it },
                    label = { Text("Roll Number") },
                    modifier = Modifier.weight(1f).testTag("roll_number_input"),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp)
                )

                OutlinedTextField(
                    value = boardCouncil,
                    onValueChange = { boardCouncil = it },
                    label = { Text("Board / Council") },
                    modifier = Modifier.weight(1.2f).testTag("board_input"),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Subject Table
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "SUBJECTS & SCORES",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                TextButton(
                    onClick = {
                        subjects = subjects + SubjectScore(name = "New Subject ${subjects.size + 1}", maxMarks = 100, marksObtained = 0)
                    }
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Subject", fontSize = 12.sp)
                }
            }

            subjects.forEachIndexed { index, sub ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    OutlinedTextField(
                        value = sub.name,
                        onValueChange = { newName ->
                            val list = subjects.toMutableList()
                            list[index] = sub.copy(name = newName)
                            subjects = list
                        },
                        modifier = Modifier.weight(2f),
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp)
                    )

                    OutlinedTextField(
                        value = if (sub.marksObtained == 0) "" else sub.marksObtained.toString(),
                        onValueChange = { newMarks ->
                            val list = subjects.toMutableList()
                            val parsed = newMarks.toIntOrNull() ?: 0
                            list[index] = sub.copy(marksObtained = parsed.coerceIn(0, sub.maxMarks))
                            subjects = list
                        },
                        placeholder = { Text("0") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp)
                    )

                    Text(text = "/ ${sub.maxMarks}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

                    if (subjects.size > 1) {
                        IconButton(
                            onClick = {
                                val list = subjects.toMutableList()
                                list.removeAt(index)
                                subjects = list
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Remove", tint = Color.Red.copy(alpha = 0.7f), modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Score Summary Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp)),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(text = "TOTAL SCORE", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = "$totalObtained / $totalMax",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = AuraPrimary
                            )
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "PERCENTAGE", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = "${String.format(Locale.US, "%.1f", percentage)}%",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(text = "GRADE", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = gradeLabel,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color(0xFF10B981)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Download Mark Sheet PDF Button
            Button(
                onClick = {
                    val data = MarkSheetData(
                        studentName = studentName,
                        rollNumber = rollNumber,
                        boardCouncil = boardCouncil,
                        subjects = subjects
                    )
                    val pdfFile = PdfExporter.generateMarkSheetPdf(
                        context = context,
                        data = data,
                        totalScore = totalObtained,
                        maxScore = totalMax,
                        percentage = percentage,
                        grade = gradeLabel
                    )

                    // Share PDF
                    val uri: Uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", pdfFile)
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "application/pdf"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        putExtra(Intent.EXTRA_SUBJECT, "Mark Sheet - $studentName")
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(Intent.createChooser(shareIntent, "Share Mark Sheet PDF"))
                    Toast.makeText(context, "Mark Sheet PDF generated!", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("download_marksheet_pdf_button"),
                colors = ButtonDefaults.buttonColors(containerColor = AuraPrimary),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Download Mark Sheet PDF", fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Notice: Digital copy for personal reference. Not an official document.",
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    }
}
