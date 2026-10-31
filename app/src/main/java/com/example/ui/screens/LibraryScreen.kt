package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.example.model.DocumentType
import com.example.model.ScanDocument
import com.example.ui.theme.AuraPrimary
import com.example.ui.theme.CoinGold
import com.example.util.PdfExporter
import com.example.util.PdfPageSize
import com.example.util.PdfQuality
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun LibraryScreen(
    documents: List<ScanDocument>,
    userCoins: Int,
    onSpendCoins: (Int) -> Unit,
    onRenameDocument: (String, String) -> Unit,
    onDeleteDocument: (String) -> Unit
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilterType by remember { mutableStateOf<DocumentType?>(null) }
    var selectedDocForDetail by remember { mutableStateOf<ScanDocument?>(null) }
    var docToRename by remember { mutableStateOf<ScanDocument?>(null) }
    var renameInput by remember { mutableStateOf("") }
    var docToExportPdf by remember { mutableStateOf<ScanDocument?>(null) }

    val filteredDocs = remember(documents, searchQuery, selectedFilterType) {
        documents.filter { doc ->
            val matchesSearch = searchQuery.isBlank() ||
                    doc.title.contains(searchQuery, ignoreCase = true) ||
                    doc.extractedText.contains(searchQuery, ignoreCase = true)
            val matchesType = selectedFilterType == null || doc.type == selectedFilterType
            matchesSearch && matchesType
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("library_screen")
    ) {
        // Search & Filter Header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("library_search_input"),
                placeholder = { Text("Search by title or extracted text...") },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = "Search", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear")
                        }
                    }
                },
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Filter Chips
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                item {
                    FilterChip(
                        label = "All",
                        isSelected = selectedFilterType == null,
                        onClick = { selectedFilterType = null }
                    )
                }
                items(DocumentType.entries) { type ->
                    FilterChip(
                        label = type.label,
                        isSelected = selectedFilterType == type,
                        onClick = { selectedFilterType = if (selectedFilterType == type) null else type }
                    )
                }
            }
        }

        // Grid of Documents
        if (filteredDocs.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Description,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = if (searchQuery.isNotEmpty()) "No matching scans found" else "Library is empty",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = if (searchQuery.isNotEmpty()) "Try a different search keyword" else "Documents you scan will be safely stored here.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 90.dp, top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(filteredDocs) { doc ->
                    LibraryDocCard(
                        doc = doc,
                        onClick = { selectedDocForDetail = doc }
                    )
                }
            }
        }
    }

    // Detail & Actions Dialog
    selectedDocForDetail?.let { doc ->
        val dateStr = SimpleDateFormat("dd MMMM yyyy, hh:mm a", Locale.getDefault()).format(Date(doc.dateAdded))
        AlertDialog(
            onDismissRequest = { selectedDocForDetail = null },
            title = {
                Text(text = doc.title, fontWeight = FontWeight.Bold, maxLines = 1)
            },
            text = {
                Column {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.Black),
                        contentAlignment = Alignment.Center
                    ) {
                        val file = File(doc.imagePath)
                        if (file.exists()) {
                            AsyncImage(
                                model = file,
                                contentDescription = doc.title,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Fit
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = "Type: ${doc.type.label}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(text = "Scanned: $dateStr", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

                    if (doc.extractedText.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = doc.extractedText.take(120) + if (doc.extractedText.length > 120) "..." else "",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            docToExportPdf = doc
                            selectedDocForDetail = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AuraPrimary)
                    ) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Export PDF")
                    }

                    OutlinedButton(
                        onClick = {
                            shareDocument(context, doc)
                            selectedDocForDetail = null
                        }
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Share")
                    }
                }
            },
            dismissButton = {
                Row {
                    TextButton(
                        onClick = {
                            docToRename = doc
                            renameInput = doc.title
                            selectedDocForDetail = null
                        }
                    ) {
                        Text("Rename")
                    }
                    TextButton(
                        onClick = {
                            onDeleteDocument(doc.id)
                            selectedDocForDetail = null
                            Toast.makeText(context, "Document deleted", Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Text("Delete", color = Color.Red)
                    }
                }
            }
        )
    }

    // Rename Dialog
    docToRename?.let { doc ->
        AlertDialog(
            onDismissRequest = { docToRename = null },
            title = { Text("Rename Document") },
            text = {
                OutlinedTextField(
                    value = renameInput,
                    onValueChange = { renameInput = it },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (renameInput.isNotBlank()) {
                            onRenameDocument(doc.id, renameInput.trim())
                            docToRename = null
                        }
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { docToRename = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // PDF Export Options Dialog
    docToExportPdf?.let { doc ->
        var selectedPageSize by remember { mutableStateOf(PdfPageSize.A4) }
        var selectedQuality by remember { mutableStateOf(PdfQuality.STANDARD) }

        AlertDialog(
            onDismissRequest = { docToExportPdf = null },
            title = { Text("PDF Export Settings", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Page Size", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    PdfPageSize.entries.forEach { size ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedPageSize = size }
                                .padding(vertical = 2.dp)
                        ) {
                            RadioButton(selected = selectedPageSize == size, onClick = { selectedPageSize = size })
                            Text(text = size.label, fontSize = 13.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text("Quality & Coins", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    PdfQuality.entries.forEach { quality ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedQuality = quality }
                                .padding(vertical = 2.dp)
                        ) {
                            RadioButton(selected = selectedQuality == quality, onClick = { selectedQuality = quality })
                            Text(text = quality.label, fontSize = 13.sp)
                        }
                    }

                    if (selectedQuality == PdfQuality.HD) {
                        Text(
                            text = "Your Balance: $userCoins Coins",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = CoinGold,
                            modifier = Modifier.padding(start = 12.dp, top = 4.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (selectedQuality == PdfQuality.HD && userCoins < 50) {
                            Toast.makeText(context, "Insufficient coins for HD export. Watch a rewarded video or choose Standard.", Toast.LENGTH_LONG).show()
                            return@Button
                        }
                        if (selectedQuality == PdfQuality.HD) {
                            onSpendCoins(50)
                        }

                        val bitmap = BitmapFactory.decodeFile(doc.imagePath)
                        if (bitmap != null) {
                            val pdfFile = PdfExporter.generateDocumentPdf(
                                context = context,
                                bitmap = bitmap,
                                title = doc.title,
                                pageSize = selectedPageSize,
                                quality = selectedQuality
                            )
                            sharePdfFile(context, pdfFile, doc.title)
                            Toast.makeText(context, "PDF generated successfully!", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Failed to load document image", Toast.LENGTH_SHORT).show()
                        }
                        docToExportPdf = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AuraPrimary)
                ) {
                    Text("Generate & Share")
                }
            },
            dismissButton = {
                TextButton(onClick = { docToExportPdf = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun FilterChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (isSelected) AuraPrimary else MaterialTheme.colorScheme.surfaceVariant)
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 6.dp)
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun LibraryDocCard(
    doc: ScanDocument,
    onClick: () -> Unit
) {
    val dateStr = remember(doc.dateAdded) {
        SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date(doc.dateAdded))
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .testTag("library_card_${doc.id.take(6)}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .background(Color(0xFFE2E8F0)),
                contentAlignment = Alignment.Center
            ) {
                val file = File(doc.imagePath)
                if (file.exists()) {
                    AsyncImage(
                        model = file,
                        contentDescription = doc.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(doc.type.badgeColor).copy(alpha = 0.9f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = doc.type.label,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            Column(modifier = Modifier.padding(10.dp)) {
                Text(
                    text = doc.title,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    maxLines = 1,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = dateStr,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun shareDocument(context: Context, doc: ScanDocument) {
    try {
        val file = File(doc.imagePath)
        val uri: Uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "image/jpeg"
            putExtra(Intent.EXTRA_STREAM, uri)
            if (doc.extractedText.isNotEmpty()) {
                putExtra(Intent.EXTRA_TEXT, "${doc.title}\n\n${doc.extractedText}")
            }
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Share Document via"))
    } catch (e: Exception) {
        Toast.makeText(context, "Could not share file: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}

private fun sharePdfFile(context: Context, file: File, title: String) {
    try {
        val uri: Uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, title)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Export & Share PDF"))
    } catch (e: Exception) {
        Toast.makeText(context, "Could not share PDF: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}
