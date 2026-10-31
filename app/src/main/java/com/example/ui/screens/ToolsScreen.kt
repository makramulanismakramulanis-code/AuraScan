package com.example.ui.screens

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ExpenseItem
import com.example.ui.screens.tools.ExpenseTrackerTool
import com.example.ui.screens.tools.HandwritingTool
import com.example.ui.screens.tools.IdCardTool
import com.example.ui.screens.tools.MarkSheetTool
import com.example.ui.screens.tools.QrStudioTool
import com.example.ui.screens.tools.RewardsTool
import com.example.ui.screens.tools.SignatureTool
import com.example.ui.screens.tools.TranslatorTool
import com.example.ui.theme.AuraPrimary

enum class ToolType(val emoji: String, val title: String, val subtitle: String, val color: Long) {
    TRANSLATOR("🌐", "Universal Language Translator", "Translate Indian & global languages with audio speech.", 0xFF4361EE),
    HANDWRITING("✍️", "Handwritten Notes Digitizer", "Turn messy handwritten pages into clean bullet points.", 0xFFF59E0B),
    MARKSHEET("🎓", "Mark Sheet Formatter", "Calculate totals, percentages, and export reference PDF.", 0xFF8B5CF6),
    EXPENSE("📊", "Automated Expense & GST Log", "Track spending, Indian GST breakdown & split bills.", 0xFF10B981),
    ID_CARD("🪪", "2-Sided ID Card Maker", "Merge Front & Back on A4 with Aadhaar number masking.", 0xFF3F37C9),
    SIGNATURE("🖋️", "Digital Signature Studio", "Draw high-res signature and export transparent PNG.", 0xFF0284C7),
    QR_STUDIO("📱", "QR Code Studio (UPI & URL)", "Generate UPI payment & website QR codes instantly.", 0xFFE11D48),
    REWARDS("🎁", "Daily Rewards & Bonus Coins", "Earn AuraCoins with daily streaks and rewarded videos.", 0xFFD97706)
}

@Composable
fun ToolsScreen(
    userCoins: Int,
    userStreak: Int,
    expenses: List<ExpenseItem>,
    onAddCoins: (Int) -> Unit,
    onAddExpense: (ExpenseItem) -> Unit,
    onDeleteExpense: (String) -> Unit
) {
    var activeTool by remember { mutableStateOf<ToolType?>(null) }
    var selectedFilterChip by remember { mutableStateOf("All") }

    val filterChips = listOf(
        "All", "Translator", "Handwriting", "Mark Sheet",
        "Expense & GST", "2-Sided ID Card", "Signature", "QR Studio", "Rewards"
    )

    // Render Active Tool Subscreen if one is selected
    when (activeTool) {
        ToolType.TRANSLATOR -> {
            TranslatorTool(onBack = { activeTool = null })
            return
        }
        ToolType.HANDWRITING -> {
            HandwritingTool(onBack = { activeTool = null })
            return
        }
        ToolType.MARKSHEET -> {
            MarkSheetTool(onBack = { activeTool = null })
            return
        }
        ToolType.EXPENSE -> {
            ExpenseTrackerTool(
                expenses = expenses,
                onAddExpense = onAddExpense,
                onDeleteExpense = onDeleteExpense,
                onBack = { activeTool = null }
            )
            return
        }
        ToolType.ID_CARD -> {
            IdCardTool(onBack = { activeTool = null })
            return
        }
        ToolType.SIGNATURE -> {
            SignatureTool(onBack = { activeTool = null })
            return
        }
        ToolType.QR_STUDIO -> {
            QrStudioTool(onBack = { activeTool = null })
            return
        }
        ToolType.REWARDS -> {
            RewardsTool(
                userCoins = userCoins,
                userStreak = userStreak,
                onAddCoins = onAddCoins,
                onBack = { activeTool = null }
            )
            return
        }
        null -> {
            // Render Tools List
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("tools_screen")
    ) {
        // Horizontal Header Chips
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 1.dp
        ) {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filterChips) { chip ->
                    val isSel = selectedFilterChip == chip
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (isSel) AuraPrimary else MaterialTheme.colorScheme.surfaceVariant)
                            .clickable {
                                selectedFilterChip = chip
                                if (chip != "All") {
                                    activeTool = when (chip) {
                                        "Translator" -> ToolType.TRANSLATOR
                                        "Handwriting" -> ToolType.HANDWRITING
                                        "Mark Sheet" -> ToolType.MARKSHEET
                                        "Expense & GST" -> ToolType.EXPENSE
                                        "2-Sided ID Card" -> ToolType.ID_CARD
                                        "Signature" -> ToolType.SIGNATURE
                                        "QR Studio" -> ToolType.QR_STUDIO
                                        "Rewards" -> ToolType.REWARDS
                                        else -> null
                                    }
                                }
                            }
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                            .testTag("tool_chip_${chip.lowercase().replace(" ", "_").replace("&", "")}")
                    ) {
                        Text(
                            text = chip,
                            fontSize = 12.sp,
                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSel) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Vertical List of 8 Tool Cards
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 90.dp, top = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(ToolType.entries) { tool ->
                ToolCardItem(
                    tool = tool,
                    onClick = { activeTool = tool }
                )
            }
        }
    }
}

@Composable
fun ToolCardItem(
    tool: ToolType,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .testTag("tool_card_${tool.name.lowercase()}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(tool.color).copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = tool.emoji, fontSize = 22.sp)
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = tool.title,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = tool.subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 16.sp
                    )
                }
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
