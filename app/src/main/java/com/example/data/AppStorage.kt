package com.example.data

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.example.model.BillBreakdown
import com.example.model.DocumentType
import com.example.model.ExpenseItem
import com.example.model.ImageFilterType
import com.example.model.ScanDocument
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class UserSettings(
    val isDarkMode: Boolean = false,
    val askBeforeAi: Boolean = true,
    val aiFreeMode: Boolean = false,
    val personalizedAds: Boolean = true,
    val consentAccepted: Boolean = false
)

class AppStorage(private val context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("aurascan_db", Context.MODE_PRIVATE)

    private val _documents = MutableStateFlow<List<ScanDocument>>(emptyList())
    val documents: StateFlow<List<ScanDocument>> = _documents.asStateFlow()

    private val _expenses = MutableStateFlow<List<ExpenseItem>>(emptyList())
    val expenses: StateFlow<List<ExpenseItem>> = _expenses.asStateFlow()

    private val _coins = MutableStateFlow(350)
    val coins: StateFlow<Int> = _coins.asStateFlow()

    private val _streak = MutableStateFlow(7)
    val streak: StateFlow<Int> = _streak.asStateFlow()

    private val _settings = MutableStateFlow(UserSettings())
    val settings: StateFlow<UserSettings> = _settings.asStateFlow()

    init {
        loadAll()
    }

    fun loadAll() {
        try {
            // Load coins & streak
            _coins.value = prefs.getInt("key_coins", 350)
            _streak.value = prefs.getInt("key_streak", 7)

            // Check daily streak
            checkDailyStreak()

            // Load settings
            _settings.value = UserSettings(
                isDarkMode = prefs.getBoolean("set_dark_mode", false),
                askBeforeAi = prefs.getBoolean("set_ask_ai", true),
                aiFreeMode = prefs.getBoolean("set_ai_free", false),
                personalizedAds = prefs.getBoolean("set_personalized_ads", true),
                consentAccepted = prefs.getBoolean("set_consent_accepted", false)
            )

            // Load documents
            val docsJson = prefs.getString("key_documents", "[]") ?: "[]"
            val docArray = JSONArray(docsJson)
            val docList = mutableListOf<ScanDocument>()
            for (i in 0 until docArray.length()) {
                val obj = docArray.getJSONObject(i)
                val billObj = obj.optJSONObject("billData")
                val bill = billObj?.let {
                    val lineItemsArr = it.optJSONArray("lineItems")
                    val items = mutableListOf<String>()
                    if (lineItemsArr != null) {
                        for (j in 0 until lineItemsArr.length()) {
                            items.add(lineItemsArr.getString(j))
                        }
                    }
                    BillBreakdown(
                        merchant = it.optString("merchant", ""),
                        date = it.optString("date", ""),
                        subtotal = it.optDouble("subtotal", 0.0),
                        cgst = it.optDouble("cgst", 0.0),
                        sgst = it.optDouble("sgst", 0.0),
                        igst = it.optDouble("igst", 0.0),
                        total = it.optDouble("total", 0.0),
                        lineItems = items
                    )
                }

                docList.add(
                    ScanDocument(
                        id = obj.getString("id"),
                        title = obj.getString("title"),
                        type = DocumentType.valueOf(obj.optString("type", DocumentType.DOCUMENT.name)),
                        dateAdded = obj.optLong("dateAdded", System.currentTimeMillis()),
                        imagePath = obj.getString("imagePath"),
                        extractedText = obj.optString("extractedText", ""),
                        billData = bill,
                        filterApplied = ImageFilterType.valueOf(obj.optString("filterApplied", ImageFilterType.ORIGINAL.name))
                    )
                )
            }
            _documents.value = docList

            // Load expenses
            val expJson = prefs.getString("key_expenses", "[]") ?: "[]"
            val expArray = JSONArray(expJson)
            val expList = mutableListOf<ExpenseItem>()
            for (i in 0 until expArray.length()) {
                val obj = expArray.getJSONObject(i)
                expList.add(
                    ExpenseItem(
                        id = obj.getString("id"),
                        merchant = obj.getString("merchant"),
                        date = obj.optString("date", ""),
                        amount = obj.optDouble("amount", 0.0),
                        category = obj.optString("category", "Other"),
                        subtotal = obj.optDouble("subtotal", 0.0),
                        cgst = obj.optDouble("cgst", 0.0),
                        sgst = obj.optDouble("sgst", 0.0),
                        notes = obj.optString("notes", ""),
                        timestamp = obj.optLong("timestamp", System.currentTimeMillis())
                    )
                )
            }
            _expenses.value = expList
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun checkDailyStreak() {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val lastCheckin = prefs.getString("key_last_checkin", "")
        if (lastCheckin != today) {
            // First time today
            val currentStreak = prefs.getInt("key_streak", 7)
            val newStreak = currentStreak + 1
            _streak.value = newStreak
            prefs.edit()
                .putString("key_last_checkin", today)
                .putInt("key_streak", newStreak)
                .apply()
        }
    }

    fun addCoins(amount: Int) {
        val newCoins = _coins.value + amount
        _coins.value = newCoins
        prefs.edit().putInt("key_coins", newCoins).apply()
    }

    fun spendCoins(amount: Int): Boolean {
        if (_coins.value >= amount) {
            val newCoins = _coins.value - amount
            _coins.value = newCoins
            prefs.edit().putInt("key_coins", newCoins).apply()
            return true
        }
        return false
    }

    fun updateSettings(newSettings: UserSettings) {
        _settings.value = newSettings
        prefs.edit()
            .putBoolean("set_dark_mode", newSettings.isDarkMode)
            .putBoolean("set_ask_ai", newSettings.askBeforeAi)
            .putBoolean("set_ai_free", newSettings.aiFreeMode)
            .putBoolean("set_personalized_ads", newSettings.personalizedAds)
            .putBoolean("set_consent_accepted", newSettings.consentAccepted)
            .apply()
    }

    fun saveDocument(doc: ScanDocument) {
        val list = _documents.value.toMutableList()
        val existingIndex = list.indexOfFirst { it.id == doc.id }
        if (existingIndex >= 0) {
            list[existingIndex] = doc
        } else {
            list.add(0, doc)
        }
        _documents.value = list
        persistDocuments()
    }

    fun deleteDocument(id: String): ScanDocument? {
        val list = _documents.value.toMutableList()
        val index = list.indexOfFirst { it.id == id }
        if (index >= 0) {
            val removed = list.removeAt(index)
            _documents.value = list
            persistDocuments()
            return removed
        }
        return null
    }

    fun restoreDocument(doc: ScanDocument) {
        saveDocument(doc)
    }

    private fun persistDocuments() {
        try {
            val array = JSONArray()
            for (doc in _documents.value) {
                val obj = JSONObject()
                obj.put("id", doc.id)
                obj.put("title", doc.title)
                obj.put("type", doc.type.name)
                obj.put("dateAdded", doc.dateAdded)
                obj.put("imagePath", doc.imagePath)
                obj.put("extractedText", doc.extractedText)
                obj.put("filterApplied", doc.filterApplied.name)
                doc.billData?.let {
                    val billObj = JSONObject()
                    billObj.put("merchant", it.merchant)
                    billObj.put("date", it.date)
                    billObj.put("subtotal", it.subtotal)
                    billObj.put("cgst", it.cgst)
                    billObj.put("sgst", it.sgst)
                    billObj.put("igst", it.igst)
                    billObj.put("total", it.total)
                    val itemsArr = JSONArray()
                    it.lineItems.forEach { item -> itemsArr.put(item) }
                    billObj.put("lineItems", itemsArr)
                    obj.put("billData", billObj)
                }
                array.put(obj)
            }
            prefs.edit().putString("key_documents", array.toString()).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun addExpense(item: ExpenseItem) {
        val list = _expenses.value.toMutableList()
        list.add(0, item)
        _expenses.value = list
        persistExpenses()
    }

    fun updateExpense(item: ExpenseItem) {
        val list = _expenses.value.toMutableList()
        val idx = list.indexOfFirst { it.id == item.id }
        if (idx >= 0) {
            list[idx] = item
            _expenses.value = list
            persistExpenses()
        }
    }

    fun deleteExpense(id: String) {
        val list = _expenses.value.toMutableList()
        list.removeAll { it.id == id }
        _expenses.value = list
        persistExpenses()
    }

    private fun persistExpenses() {
        try {
            val array = JSONArray()
            for (item in _expenses.value) {
                val obj = JSONObject()
                obj.put("id", item.id)
                obj.put("merchant", item.merchant)
                obj.put("date", item.date)
                obj.put("amount", item.amount)
                obj.put("category", item.category)
                obj.put("subtotal", item.subtotal)
                obj.put("cgst", item.cgst)
                obj.put("sgst", item.sgst)
                obj.put("notes", item.notes)
                obj.put("timestamp", item.timestamp)
                array.put(obj)
            }
            prefs.edit().putString("key_expenses", array.toString()).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun clearAllData() {
        // Delete all images in internal storage
        for (doc in _documents.value) {
            try {
                File(doc.imagePath).delete()
            } catch (_: Exception) {}
        }
        _documents.value = emptyList()
        _expenses.value = emptyList()
        prefs.edit()
            .remove("key_documents")
            .remove("key_expenses")
            .apply()
    }

    fun saveBitmapToInternalStorage(bitmap: Bitmap, prefix: String = "scan"): String {
        val filename = "${prefix}_${System.currentTimeMillis()}.jpg"
        val file = File(context.filesDir, filename)
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
        }
        return file.absolutePath
    }

    fun loadBitmapFromPath(path: String): Bitmap? {
        return try {
            BitmapFactory.decodeFile(path)
        } catch (e: Exception) {
            null
        }
    }
}
