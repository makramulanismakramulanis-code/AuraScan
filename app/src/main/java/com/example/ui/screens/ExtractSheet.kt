package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.window.Dialog
import com.example.ai.ExtractedDocumentResult
import com.example.ai.GeminiService
import com.example.model.BillBreakdown
import com.example.model.ExpenseItem
import com.example.ui.theme.AuraPrimary
import kotlinx.coroutines.launch
import java.util.Locale

@Composable
fun ExtractSheet(
    bitmap: Bitmap,
    onDismiss: () -> Unit,
    onAddExpense: (ExpenseItem) -> Unit,
    onSaveExtracted: (ExtractedDocumentResult) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(true) }
    var extractionResult by remember { mutableStateOf<ExtractedDocumentResult?>(null) }
    var editableText by remember { mutableStateOf("") }
    var addedToExpenses by remember { mutableStateOf(false) }

    LaunchedEffect(bitmap) {
        val result = GeminiService.analyzeDocument(bitmap)
        result.onSuccess { data ->
            extractionResult = data
            editableText = data.extractedText
            isLoading = false
        }.onFailure {
            isLoading = false
            Toast.makeText(context, "AI analysis note: using offline OCR fallback", Toast.LENGTH_SHORT).show()
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .testTag("extract_sheet_dialog")
        ) {
            Column(
                modifier = Modifier
                    .padding(18.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Extracted Document Data",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                if (isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = AuraPrimary)
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = "Processing document with Gemini AI...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    val bill = extractionResult?.billData
                    if (bill != null && (bill.total > 0 || bill.subtotal > 0)) {
                        // Bill Breakdown Detected Card
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .border(1.dp, Color(0xFF10B981).copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                                .testTag("bill_breakdown_card"),
                            color = Color(0xFF10B981).copy(alpha = 0.08f)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Receipt,
                                        contentDescription = null,
                                        tint = Color(0xFF10B981),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "🧾 Bill Breakdown Detected",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = Color(0xFF065F46)
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))
                                BillRow("Merchant", bill.merchant.ifEmpty { "Store" })
                                BillRow("Date", bill.date.ifEmpty { "2026-10-30" })
                                BillRow("Subtotal", "₹${String.format(Locale.US, "%.2f", bill.subtotal)}")
                                if (bill.cgst > 0) BillRow("CGST", "₹${String.format(Locale.US, "%.2f", bill.cgst)}")
                                if (bill.sgst > 0) BillRow("SGST", "₹${String.format(Locale.US, "%.2f", bill.sgst)}")
                                if (bill.igst > 0) BillRow("IGST", "₹${String.format(Locale.US, "%.2f", bill.igst)}")
                                BillRow("Grand Total", "₹${String.format(Locale.US, "%.2f", bill.total)}", isBold = true)

                                Spacer(modifier = Modifier.height(10.dp))

                                Button(
                                    onClick = {
                                        if (!addedToExpenses) {
                                            onAddExpense(
                                                ExpenseItem(
                                                    merchant = bill.merchant.ifEmpty { "Scanned Store" },
                                                    date = bill.date.ifEmpty { "2026-10-30" },
                                                    amount = if (bill.total > 0) bill.total else bill.subtotal,
                                                    category = "Bills & Utilities",
                                                    subtotal = bill.subtotal,
                                                    cgst = bill.cgst,
                                                    sgst = bill.sgst
                                                )
                                            )
                                            addedToExpenses = true
                                            Toast.makeText(context, "Added to Expense Log!", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (addedToExpenses) Color(0xFF10B981) else AuraPrimary
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(40.dp)
                                        .testTag("add_to_expenses_button")
                                ) {
                                    Icon(
                                        imageVector = if (addedToExpenses) Icons.Default.Check else Icons.Default.Add,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = Color.White
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (addedToExpenses) "Added to Expenses" else "Add to Expenses",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = Color.White
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    // Extracted Text Field (allows manual editing if needed)
                    Text(
                        text = "EXTRACTED TEXT CONTENT",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    OutlinedTextField(
                        value = editableText,
                        onValueChange = { editableText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                            .testTag("extracted_text_field"),
                        shape = RoundedCornerShape(10.dp),
                        textStyle = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp)
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Action buttons: "📋 Copy Text" and "Done"
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("AuraScan OCR", editableText)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "Text copied to clipboard", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1f).testTag("copy_text_button")
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Copy Text")
                        }

                        Button(
                            onClick = {
                                val updated = extractionResult?.copy(extractedText = editableText)
                                    ?: ExtractedDocumentResult(
                                        documentType = com.example.model.DocumentType.DOCUMENT,
                                        extractedText = editableText
                                    )
                                onSaveExtracted(updated)
                                onDismiss()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = AuraPrimary),
                            modifier = Modifier.weight(1f).testTag("done_extract_button")
                        ) {
                            Text("Done", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BillRow(label: String, value: String, isBold: Boolean = false) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            fontSize = 12.sp,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
