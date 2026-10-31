package com.example.ui.screens

import android.graphics.BitmapFactory
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.model.DocumentType
import com.example.model.ExpenseItem
import com.example.model.ScanDocument
import com.example.ui.components.InlineAdCard
import com.example.ui.components.RewardedAdModal
import com.example.ui.theme.AuraPrimary
import com.example.ui.theme.CoinGold
import com.example.util.PdfExporter
import com.example.util.PdfPageSize
import com.example.util.PdfQuality
import java.io.File
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(
    recentScans: List<ScanDocument>,
    expenses: List<ExpenseItem>,
    onScanClick: (DocumentType) -> Unit,
    onViewExpenseReport: () -> Unit,
    onDocumentClick: (ScanDocument) -> Unit,
    onViewAllScans: () -> Unit,
    userCoins: Int = 0,
    onAddCoins: ((Int) -> Unit)? = null,
    onSpendCoins: ((Int) -> Boolean)? = null
) {
    val context = LocalContext.current

    // Rewarded Ad & PDF Download States
    var showRewardedAdModal by remember { mutableStateOf(false) }
    var showDownloadDialog by remember { mutableStateOf(false) }
    var selectedDocForExport by remember { mutableStateOf<ScanDocument?>(null) }
    var targetDocForPdf by remember { mutableStateOf<ScanDocument?>(null) }
    var selectedPageSize by remember { mutableStateOf(PdfPageSize.A4) }
    var selectedQuality by remember { mutableStateOf(PdfQuality.HD) }

    // Calculate this month's expense total in ₹
    val thisMonthTotal = remember(expenses) {
        val currentMonthStr = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())
        expenses.filter {
            val itemMonth = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date(it.timestamp))
            itemMonth == currentMonthStr
        }.sumOf { it.amount }
    }


    val currencyFormatter = remember {
        val format = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("en-IN"))
        format.maximumFractionDigits = 2
        format
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("home_screen"),
        contentPadding = PaddingValues(bottom = 90.dp)
    ) {
        // Hero Title Section
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp)
            ) {
                Text(
                    text = "Fast Document Scanner",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "On-device OCR, Indian GST bill detection & instant PDF generation.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Big Primary Button: "Tap to Scan Document"
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp)
            ) {
                Button(
                    onClick = { onScanClick(DocumentType.DOCUMENT) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .testTag("home_big_scan_button"),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AuraPrimary),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = "Camera",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Tap to Scan Document",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }

        // Quick Scanner Modes: 4 tappable tiles
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 18.dp)
            ) {
                Text(
                    text = "Quick Scanner Modes",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    QuickModeTile(
                        title = "Document",
                        icon = Icons.Default.Description,
                        color = Color(0xFF4361EE),
                        modifier = Modifier.weight(1f),
                        onClick = { onScanClick(DocumentType.DOCUMENT) }
                    )
                    QuickModeTile(
                        title = "ID Card",
                        icon = Icons.Default.Badge,
                        color = Color(0xFF3F37C9),
                        modifier = Modifier.weight(1f),
                        onClick = { onScanClick(DocumentType.ID_CARD) }
                    )
                    QuickModeTile(
                        title = "Bill / GST",
                        icon = Icons.AutoMirrored.Filled.ReceiptLong,
                        color = Color(0xFF10B981),
                        modifier = Modifier.weight(1f),
                        onClick = { onScanClick(DocumentType.BILL) }
                    )
                    QuickModeTile(
                        title = "Notes",
                        icon = Icons.Default.EditNote,
                        color = Color(0xFFF59E0B),
                        modifier = Modifier.weight(1f),
                        onClick = { onScanClick(DocumentType.NOTES) }
                    )
                }
            }
        }

        // Rewarded Ad Option on Home Screen (Always shows Ad option to earn coins)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .border(
                        width = 1.5.dp,
                        color = Color(0xFFF59E0B),
                        shape = RoundedCornerShape(16.dp)
                    )
                    .testTag("home_rewarded_pdf_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFF59E0B).copy(alpha = 0.08f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFF59E0B).copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MonetizationOn,
                                    contentDescription = "PDF Reward",
                                    tint = Color(0xFFF59E0B),
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Watch Ad to Earn Coins",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "+50 Coins per ad • PDF costs 250 Coins",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFFF59E0B),
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        // Badge
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFF59E0B)
                        ) {
                            Text(
                                text = "+50 COINS",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Watch a video ad to earn 50 AuraCoins. Downloading each PDF requires 250 AuraCoins. Your balance is shown in the top right.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = {
                            showRewardedAdModal = true
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("home_rewarded_pdf_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFF59E0B)
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayCircle,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Watch Video Ad (+50 Coins)",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }

        // Expense & GST Tracker Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { onViewExpenseReport() }
                    .testTag("home_expense_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF10B981).copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                                    contentDescription = "Expenses",
                                    tint = Color(0xFF10B981),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Expense & GST Tracker",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "This Month's Total",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Text(
                            text = "View Report →",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = AuraPrimary
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = currencyFormatter.format(thisMonthTotal),
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Track and split personal expenses directly from your scanned bills.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Advertisement Card on Home
        item {
            InlineAdCard(modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp))
        }

        // Recent Scans Section
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recent Scans",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (recentScans.isNotEmpty()) {
                    TextButton(onClick = onViewAllScans) {
                        Text(text = "View All", color = AuraPrimary)
                    }
                }
            }
        }

        if (recentScans.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Description,
                            contentDescription = "Empty",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.size(52.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "No scans yet",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Tap the camera button to create your first document scan.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }
        } else {
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(recentScans.take(6)) { doc ->
                        RecentScanCard(
                            doc = doc,
                            onClick = { onDocumentClick(doc) },
                            onDownloadPdfClick = {
                                targetDocForPdf = doc
                                selectedDocForExport = doc
                                showDownloadDialog = true
                            }
                        )
                    }
                }
            }
        }
    }

    // Rewarded Ad Modal on HomeScreen
    if (showRewardedAdModal) {
        RewardedAdModal(
            onDismiss = {
                showRewardedAdModal = false
            },
            onRewardEarned = { coins ->
                onAddCoins?.invoke(coins)
                showRewardedAdModal = false
                Toast.makeText(context, "+$coins AuraCoins earned! Coins updated in top right.", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // Download PDF Dialog (Requires 250 Coins)
    if (showDownloadDialog) {
        val currentDoc = selectedDocForExport ?: recentScans.firstOrNull()
        val hasEnoughCoins = userCoins >= 250

        AlertDialog(
            onDismissRequest = {
                showDownloadDialog = false
                targetDocForPdf = null
            },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.PictureAsPdf,
                        contentDescription = null,
                        tint = AuraPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Download PDF Document", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column {
                    // Coins Status Banner
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (hasEnoughCoins) Color(0xFF10B981).copy(alpha = 0.12f) else Color(0xFFF59E0B).copy(alpha = 0.12f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = if (hasEnoughCoins) "✓ 250 Coins Required" else "⚠️ 250 Coins Required",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = if (hasEnoughCoins) Color(0xFF047857) else Color(0xFFB45309)
                                )
                                Text(
                                    text = "Your Balance (Top Right): $userCoins Coins",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (!hasEnoughCoins) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color(0xFFF59E0B)
                                ) {
                                    Text(
                                        text = "Need ${250 - userCoins} more",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    if (currentDoc != null) {
                        Text("Select Document", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        if (recentScans.size > 1) {
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                            ) {
                                items(recentScans) { doc ->
                                    val isSelected = (currentDoc.id == doc.id)
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isSelected) AuraPrimary else MaterialTheme.colorScheme.surfaceVariant,
                                        modifier = Modifier.clickable { selectedDocForExport = doc }
                                    ) {
                                        Text(
                                            text = doc.title.take(16),
                                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                            fontSize = 11.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                                        )
                                    }
                                }
                            }
                        } else {
                            Text(
                                text = "Document: ${currentDoc.title}",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))
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
                                Text(text = size.label, fontSize = 12.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Quality", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        PdfQuality.entries.forEach { quality ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedQuality = quality }
                                    .padding(vertical = 2.dp)
                            ) {
                                RadioButton(selected = selectedQuality == quality, onClick = { selectedQuality = quality })
                                Text(
                                    text = quality.label,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    } else {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "You don't have any scans yet. You can download a sample verification PDF (requires 250 coins) or scan a document first.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            confirmButton = {
                if (hasEnoughCoins) {
                    Button(
                        onClick = {
                            val success = onSpendCoins?.invoke(250) ?: true
                            if (success) {
                                if (currentDoc != null) {
                                    val bitmap = BitmapFactory.decodeFile(currentDoc.imagePath)
                                    if (bitmap != null) {
                                        val pdfFile = PdfExporter.generateDocumentPdf(
                                            context = context,
                                            bitmap = bitmap,
                                            title = currentDoc.title,
                                            pageSize = selectedPageSize,
                                            quality = selectedQuality
                                        )
                                        PdfExporter.sharePdf(context, pdfFile, currentDoc.title)
                                        Toast.makeText(context, "PDF downloaded! 250 AuraCoins deducted. Remaining: ${userCoins - 250}", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "Failed to load document image", Toast.LENGTH_SHORT).show()
                                    }
                                } else {
                                    val sampleFile = PdfExporter.generateSamplePdf(context)
                                    PdfExporter.sharePdf(context, sampleFile, "AuraScan Sample PDF")
                                    Toast.makeText(context, "Sample PDF downloaded! 250 AuraCoins deducted.", Toast.LENGTH_SHORT).show()
                                }
                                showDownloadDialog = false
                                targetDocForPdf = null
                            } else {
                                Toast.makeText(context, "Insufficient coins! Watch an ad to earn 50 coins.", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AuraPrimary)
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Download PDF (250 Coins)")
                    }
                } else {
                    Button(
                        onClick = {
                            showDownloadDialog = false
                            showRewardedAdModal = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B))
                    ) {
                        Icon(Icons.Default.PlayCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Watch Ad (+50 Coins)")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showDownloadDialog = false
                    targetDocForPdf = null
                }) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
fun QuickModeTile(
    title: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .height(78.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .testTag("quick_mode_${title.lowercase().replace(" ", "_").replace("/", "")}"),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = color,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = title,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun RecentScanCard(
    doc: ScanDocument,
    onClick: () -> Unit,
    onDownloadPdfClick: (() -> Unit)? = null
) {
    val dateStr = remember(doc.dateAdded) {
        SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date(doc.dateAdded))
    }

    Card(
        modifier = Modifier
            .width(152.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(96.dp)
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
                } else {
                    Icon(
                        imageVector = Icons.Default.PictureAsPdf,
                        contentDescription = null,
                        tint = Color.Gray,
                        modifier = Modifier.size(36.dp)
                    )
                }

                // Type Badge
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

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = doc.title,
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1
                    )
                    Text(
                        text = dateStr,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (onDownloadPdfClick != null) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(AuraPrimary.copy(alpha = 0.12f))
                            .clickable { onDownloadPdfClick() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = "Download PDF",
                            tint = AuraPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

