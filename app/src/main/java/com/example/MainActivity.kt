package com.example

import android.graphics.Bitmap
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import com.example.ads.AdService
import com.example.ads.PremiumPlan
import com.example.ads.PremiumService
import com.example.ai.ExtractedDocumentResult
import com.example.data.AppStorage
import com.example.model.DocumentType
import com.example.model.ImageFilterType
import com.example.model.ScanDocument
import com.example.ui.components.AppBottomNav
import com.example.ui.components.AppHeader
import com.example.ui.components.AppTab
import com.example.ui.components.ConsentDialog
import com.example.ui.components.PinnedAdBanner
import com.example.ui.components.UpsellDialog
import com.example.ui.screens.CameraScreen
import com.example.ui.screens.EditorScreen
import com.example.ui.screens.ExtractSheet
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LibraryScreen
import com.example.ui.screens.PrivacyPolicyScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.ToolsScreen
import com.example.ui.theme.AuraScanTheme
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

import com.google.android.gms.ads.MobileAds

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        MobileAds.initialize(this) {}
        val storage = AppStorage(applicationContext)
        AdService.init(applicationContext)

        setContent {
            val settings by storage.settings.collectAsState()
            val documents by storage.documents.collectAsState()
            val expenses by storage.expenses.collectAsState()
            val coins by storage.coins.collectAsState()
            val streak by storage.streak.collectAsState()

            AuraScanTheme(darkTheme = settings.isDarkMode) {
                AuraScanApp(
                    storage = storage,
                    documents = documents,
                    expenses = expenses,
                    coins = coins,
                    streak = streak,
                    settings = settings
                )
            }
        }
    }
}

enum class ScreenState {
    MAIN_TABS,
    CAMERA,
    EDITOR,
    PRIVACY_POLICY
}

@Composable
fun AuraScanApp(
    storage: AppStorage,
    documents: List<ScanDocument>,
    expenses: List<com.example.model.ExpenseItem>,
    coins: Int,
    streak: Int,
    settings: com.example.data.UserSettings
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var currentScreen by remember { mutableStateOf(ScreenState.MAIN_TABS) }
    var currentTab by remember { mutableStateOf(AppTab.HOME) }
    var cameraMode by remember { mutableStateOf(DocumentType.DOCUMENT) }

    // Editor & Extraction States
    var capturedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var showExtractSheet by remember { mutableStateOf(false) }
    var latestExtractedResult by remember { mutableStateOf<ExtractedDocumentResult?>(null) }

    // Dialogs
    var showUpsellDialog by remember { mutableStateOf(false) }
    var showConsentDialog by remember { mutableStateOf(!settings.consentAccepted) }

    // Handle Hardware Back Button
    BackHandler(enabled = currentScreen != ScreenState.MAIN_TABS || currentTab != AppTab.HOME) {
        if (currentScreen != ScreenState.MAIN_TABS) {
            currentScreen = ScreenState.MAIN_TABS
        } else if (currentTab != AppTab.HOME) {
            currentTab = AppTab.HOME
        }
    }

    when (currentScreen) {
        ScreenState.CAMERA -> {
            CameraScreen(
                initialMode = cameraMode,
                onClose = { currentScreen = ScreenState.MAIN_TABS },
                onImageCaptured = { bitmap, mode ->
                    capturedBitmap = bitmap
                    cameraMode = mode
                    currentScreen = ScreenState.EDITOR
                }
            )
        }

        ScreenState.EDITOR -> {
            capturedBitmap?.let { bitmap ->
                EditorScreen(
                    initialBitmap = bitmap,
                    documentType = cameraMode,
                    onClose = { currentScreen = ScreenState.MAIN_TABS },
                    onExtractTextClick = { processedBmp, _ ->
                        capturedBitmap = processedBmp
                        showExtractSheet = true
                    },
                    onSaveScanClick = { processedBmp, filter ->
                        val path = storage.saveBitmapToInternalStorage(processedBmp)
                        val title = "${cameraMode.label} Scan ${SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date())}"
                        val newDoc = ScanDocument(
                            title = title,
                            type = cameraMode,
                            imagePath = path,
                            filterApplied = filter,
                            extractedText = latestExtractedResult?.extractedText ?: "",
                            billData = latestExtractedResult?.billData
                        )
                        storage.saveDocument(newDoc)
                        Toast.makeText(context, "Scan saved to Library!", Toast.LENGTH_SHORT).show()

                        // Trigger export check for pro upsell
                        if (PremiumService.recordExportAndCheckUpsell(context)) {
                            showUpsellDialog = true
                        }

                        currentScreen = ScreenState.MAIN_TABS
                        currentTab = AppTab.LIBRARY
                    }
                )
            }
        }

        ScreenState.PRIVACY_POLICY -> {
            PrivacyPolicyScreen(
                onBack = { currentScreen = ScreenState.MAIN_TABS }
            )
        }

        ScreenState.MAIN_TABS -> {
            Scaffold(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("aura_scan_main_scaffold"),
                topBar = {
                    AppHeader(
                        coins = coins,
                        isDarkMode = settings.isDarkMode,
                        onToggleDarkMode = {
                            storage.updateSettings(settings.copy(isDarkMode = !settings.isDarkMode))
                        },
                        onLibraryClick = { currentTab = AppTab.LIBRARY },
                        onCoinsClick = { currentTab = AppTab.TOOLS }
                    )
                },
                bottomBar = {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        PinnedAdBanner()
                        AppBottomNav(
                            currentTab = currentTab,
                            onTabSelected = { currentTab = it },
                            onCameraClick = {
                                cameraMode = DocumentType.DOCUMENT
                                currentScreen = ScreenState.CAMERA
                            }
                        )
                    }
                }
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .background(MaterialTheme.colorScheme.background)
                ) {
                    when (currentTab) {
                        AppTab.HOME -> {
                            HomeScreen(
                                recentScans = documents,
                                expenses = expenses,
                                onScanClick = { mode ->
                                    cameraMode = mode
                                    currentScreen = ScreenState.CAMERA
                                },
                                onViewExpenseReport = { currentTab = AppTab.TOOLS },
                                onDocumentClick = { currentTab = AppTab.LIBRARY },
                                onViewAllScans = { currentTab = AppTab.LIBRARY }
                            )
                        }

                        AppTab.LIBRARY -> {
                            LibraryScreen(
                                documents = documents,
                                userCoins = coins,
                                onSpendCoins = { storage.spendCoins(it) },
                                onRenameDocument = { id, newName ->
                                    val doc = documents.find { it.id == id }
                                    if (doc != null) {
                                        storage.saveDocument(doc.copy(title = newName))
                                    }
                                },
                                onDeleteDocument = { id ->
                                    storage.deleteDocument(id)
                                }
                            )
                        }

                        AppTab.TOOLS -> {
                            ToolsScreen(
                                userCoins = coins,
                                userStreak = streak,
                                expenses = expenses,
                                onAddCoins = { storage.addCoins(it) },
                                onAddExpense = { storage.addExpense(it) },
                                onDeleteExpense = { storage.deleteExpense(it) }
                            )
                        }

                        AppTab.SETTINGS -> {
                            SettingsScreen(
                                settings = settings,
                                onUpdateSettings = { storage.updateSettings(it) },
                                onClearAllData = { storage.clearAllData() },
                                onOpenPrivacyPolicy = { currentScreen = ScreenState.PRIVACY_POLICY },
                                onOpenUpsell = { showUpsellDialog = true },
                                onOpenConsent = { showConsentDialog = true }
                            )
                        }
                    }
                }
            }
        }
    }

    // Extracted Document Data Bottom Sheet / Modal
    if (showExtractSheet && capturedBitmap != null) {
        ExtractSheet(
            bitmap = capturedBitmap!!,
            onDismiss = { showExtractSheet = false },
            onAddExpense = { item ->
                storage.addExpense(item)
            },
            onSaveExtracted = { result ->
                latestExtractedResult = result
                showExtractSheet = false
            }
        )
    }

    // AuraScan Premium Upsell Dialog
    if (showUpsellDialog) {
        UpsellDialog(
            onDismiss = { showUpsellDialog = false },
            onPlanSelected = { plan ->
                showUpsellDialog = false
                Toast.makeText(context, "Pro activated: ${plan.name}!", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // First Launch Ads & Privacy Consent Dialog
    if (showConsentDialog) {
        ConsentDialog(
            onAccept = {
                storage.updateSettings(settings.copy(consentAccepted = true, personalizedAds = true))
                showConsentDialog = false
            },
            onManage = {
                storage.updateSettings(settings.copy(consentAccepted = true))
                showConsentDialog = false
                currentTab = AppTab.SETTINGS
            }
        )
    }
}
