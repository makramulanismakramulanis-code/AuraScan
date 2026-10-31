package com.example.model

import java.util.UUID

enum class DocumentType(val label: String, val badgeColor: Long) {
    DOCUMENT("Document", 0xFF4361EE),
    ID_CARD("ID Card", 0xFF3F37C9),
    BILL("Bill / GST", 0xFF10B981),
    NOTES("Notes", 0xFFF59E0B),
    MARKSHEET("Mark Sheet", 0xFF8B5CF6);

    companion object {
        fun fromString(type: String?): DocumentType {
            return when (type?.lowercase()) {
                "id_card", "id card" -> ID_CARD
                "bill", "gst", "receipt" -> BILL
                "notes", "handwritten" -> NOTES
                "marksheet", "mark sheet" -> MARKSHEET
                else -> DOCUMENT
            }
        }
    }
}

enum class ImageFilterType(val label: String) {
    ORIGINAL("Original"),
    MAGIC_COLOR("Magic Color"),
    BW("B&W"),
    DOCUMENT("Document"),
    GRAYSCALE("Grayscale")
}

data class BillBreakdown(
    val merchant: String = "",
    val date: String = "",
    val subtotal: Double = 0.0,
    val cgst: Double = 0.0,
    val sgst: Double = 0.0,
    val igst: Double = 0.0,
    val total: Double = 0.0,
    val lineItems: List<String> = emptyList()
)

data class ScanDocument(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val type: DocumentType = DocumentType.DOCUMENT,
    val dateAdded: Long = System.currentTimeMillis(),
    val imagePath: String, // internal file path
    val extractedText: String = "",
    val billData: BillBreakdown? = null,
    val filterApplied: ImageFilterType = ImageFilterType.ORIGINAL
)

data class ExpenseItem(
    val id: String = UUID.randomUUID().toString(),
    val merchant: String,
    val date: String,
    val amount: Double,
    val category: String,
    val subtotal: Double = 0.0,
    val cgst: Double = 0.0,
    val sgst: Double = 0.0,
    val notes: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

data class SubjectScore(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val maxMarks: Int = 100,
    val marksObtained: Int = 0
)

data class MarkSheetData(
    val studentName: String = "",
    val rollNumber: String = "",
    val boardCouncil: String = "",
    val subjects: List<SubjectScore> = emptyList()
)
