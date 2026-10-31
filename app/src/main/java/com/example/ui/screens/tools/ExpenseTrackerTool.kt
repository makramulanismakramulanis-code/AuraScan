package com.example.ui.screens.tools

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Receipt
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.example.model.ExpenseItem
import com.example.ui.theme.AuraPrimary
import java.io.File
import java.io.FileOutputStream
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun ExpenseTrackerTool(
    expenses: List<ExpenseItem>,
    onAddExpense: (ExpenseItem) -> Unit,
    onDeleteExpense: (String) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var currentCalendar by remember { mutableStateOf(Calendar.getInstance()) }

    // Quick add inputs
    var quickAmount by remember { mutableStateOf("") }
    var quickMerchant by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Food & Dining") }

    val categories = listOf("Food & Dining", "Shopping", "Bills & Utilities", "Travel & Fuel", "Other")

    // Split Bill State
    var showSplitDialogForExpense by remember { mutableStateOf<ExpenseItem?>(null) }
    var splitCount by remember { mutableIntStateOf(2) }

    val monthYearFormat = remember { SimpleDateFormat("MMMM yyyy", Locale.getDefault()) }
    val monthFilterKey = remember { SimpleDateFormat("yyyy-MM", Locale.getDefault()) }
    val currentMonthKey = remember(currentCalendar) { monthFilterKey.format(currentCalendar.time) }

    val monthExpenses = remember(expenses, currentMonthKey) {
        expenses.filter {
            val key = monthFilterKey.format(Date(it.timestamp))
            key == currentMonthKey
        }
    }

    val totalSpent = remember(monthExpenses) { monthExpenses.sumOf { it.amount } }
    val totalCgst = remember(monthExpenses) { monthExpenses.sumOf { it.cgst } }
    val totalSgst = remember(monthExpenses) { monthExpenses.sumOf { it.sgst } }
    val totalGst = totalCgst + totalSgst

    val currencyFormatter = remember {
        val format = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("en-IN"))
        format.maximumFractionDigits = 2
        format
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("tool_expense_tracker")
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
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                    Text(
                        text = "Expense & GST Log",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Export CSV Button
                OutlinedButton(
                    onClick = {
                        val csvFile = exportExpensesToCsv(context, monthExpenses, monthYearFormat.format(currentCalendar.time))
                        shareCsvFile(context, csvFile)
                    },
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("export_csv_button")
                ) {
                    Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Export CSV", fontSize = 11.sp)
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Month Navigation Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = {
                    val cal = currentCalendar.clone() as Calendar
                    cal.add(Calendar.MONTH, -1)
                    currentCalendar = cal
                }) {
                    Icon(Icons.Default.ChevronLeft, contentDescription = "Previous Month")
                }

                Text(
                    text = "${monthYearFormat.format(currentCalendar.time)} Expenses",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )

                IconButton(onClick = {
                    val cal = currentCalendar.clone() as Calendar
                    cal.add(Calendar.MONTH, 1)
                    currentCalendar = cal
                }) {
                    Icon(Icons.Default.ChevronRight, contentDescription = "Next Month")
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Stat Cards: TOTAL SPENT and GST BREAKDOWN in ₹
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = AuraPrimary.copy(alpha = 0.1f))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(text = "TOTAL SPENT", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = AuraPrimary)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = currencyFormatter.format(totalSpent),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                            color = AuraPrimary
                        )
                    }
                }

                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF10B981).copy(alpha = 0.1f))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(text = "GST BREAKDOWN", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF065F46))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = currencyFormatter.format(totalGst),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                            color = Color(0xFF10B981)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Add Quick Expense Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Add Quick Expense",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = quickAmount,
                            onValueChange = { quickAmount = it },
                            placeholder = { Text("Amount (₹)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f).testTag("quick_expense_amount"),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp)
                        )

                        OutlinedTextField(
                            value = quickMerchant,
                            onValueChange = { quickMerchant = it },
                            placeholder = { Text("Merchant / Note") },
                            modifier = Modifier.weight(1.4f).testTag("quick_expense_merchant"),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Category Chips
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(categories) { cat ->
                            val isSel = selectedCategory == cat
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(if (isSel) AuraPrimary else MaterialTheme.colorScheme.surface)
                                    .clickable { selectedCategory = cat }
                                    .padding(horizontal = 10.dp, vertical = 5.dp)
                            ) {
                                Text(
                                    text = cat,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSel) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = {
                            val amountVal = quickAmount.toDoubleOrNull() ?: 0.0
                            if (amountVal <= 0.0) {
                                Toast.makeText(context, "Please enter a valid amount", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                            onAddExpense(
                                ExpenseItem(
                                    merchant = quickMerchant.ifBlank { "Quick Expense" },
                                    date = todayStr,
                                    amount = amountVal,
                                    category = selectedCategory,
                                    subtotal = amountVal * 0.9,
                                    cgst = (amountVal * 0.05),
                                    sgst = (amountVal * 0.05)
                                )
                            )
                            quickAmount = ""
                            quickMerchant = ""
                            Toast.makeText(context, "Expense recorded!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.fillMaxWidth().height(42.dp).testTag("add_quick_expense_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = AuraPrimary),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Record Expense", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Recent Recorded Entries Header
            Text(
                text = "RECENT RECORDED ENTRIES (${monthExpenses.size})",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (monthExpenses.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No expenses recorded for this month.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                monthExpenses.forEach { exp ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(AuraPrimary.copy(alpha = 0.12f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Receipt, contentDescription = null, tint = AuraPrimary, modifier = Modifier.size(18.dp))
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(text = exp.merchant, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                                    Text(text = "${exp.category} • ${exp.date}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = currencyFormatter.format(exp.amount),
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.width(6.dp))

                                // Split Button
                                IconButton(
                                    onClick = { showSplitDialogForExpense = exp },
                                    modifier = Modifier.size(30.dp)
                                ) {
                                    Icon(Icons.Default.Group, contentDescription = "Split Bill", tint = AuraPrimary, modifier = Modifier.size(16.dp))
                                }

                                // Delete Button
                                IconButton(
                                    onClick = { onDeleteExpense(exp.id) },
                                    modifier = Modifier.size(30.dp)
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red.copy(alpha = 0.7f), modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Split Bill Dialog
    showSplitDialogForExpense?.let { exp ->
        val perPerson = exp.amount / splitCount
        AlertDialog(
            onDismissRequest = { showSplitDialogForExpense = null },
            title = { Text("Split Bill: ${exp.merchant}", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(text = "Total Amount: ${currencyFormatter.format(exp.amount)}", fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "Split between $splitCount people", fontWeight = FontWeight.SemiBold)
                        Row {
                            IconButton(onClick = { if (splitCount > 2) splitCount-- }, modifier = Modifier.size(32.dp)) {
                                Text("-", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                            }
                            IconButton(onClick = { splitCount++ }, modifier = Modifier.size(32.dp)) {
                                Text("+", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = AuraPrimary.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(text = "EACH PERSON PAYS", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = AuraPrimary)
                            Text(
                                text = currencyFormatter.format(perPerson),
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                                color = AuraPrimary
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val message = "Hey! Here's the split for ${exp.merchant} (Total: ${currencyFormatter.format(exp.amount)}). Each person's share is ${currencyFormatter.format(perPerson)}."
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("Bill Split", message))
                        Toast.makeText(context, "Split message copied to clipboard!", Toast.LENGTH_SHORT).show()
                        showSplitDialogForExpense = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AuraPrimary)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Copy Split Message")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSplitDialogForExpense = null }) {
                    Text("Close")
                }
            }
        )
    }
}

private fun exportExpensesToCsv(context: Context, items: List<ExpenseItem>, monthStr: String): File {
    val file = File(context.cacheDir, "AuraScan_Expenses_${monthStr.replace(" ", "_")}.csv")
    FileOutputStream(file).use { out ->
        out.write("date,merchant,category,subtotal,CGST,SGST,total\n".toByteArray())
        for (item in items) {
            val line = "${item.date},\"${item.merchant}\",${item.category},${item.subtotal},${item.cgst},${item.sgst},${item.amount}\n"
            out.write(line.toByteArray())
        }
    }
    return file
}

private fun shareCsvFile(context: Context, file: File) {
    try {
        val uri: Uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Export Expenses CSV"))
    } catch (e: Exception) {
        Toast.makeText(context, "Could not export CSV: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}
