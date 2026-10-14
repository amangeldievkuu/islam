package com.sunnahcompanion.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavKey
import com.sunnahcompanion.app.core.database.model.LifeSituation
import com.sunnahcompanion.app.navigation.CategoryKey
import com.sunnahcompanion.app.navigation.ReaderKey
import com.sunnahcompanion.app.navigation.SunnahApp
import com.sunnahcompanion.app.notification.AppIntents
import com.sunnahcompanion.app.ui.theme.SunnahTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    /** Destination requested by a notification tap, consumed once by the nav host. */
    private var pendingDeepLink by mutableStateOf<NavKey?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (savedInstanceState == null) pendingDeepLink = intent.toDeepLink()
        setContent {
            val theme by viewModel.themeSettings.collectAsStateWithLifecycle()
            SunnahTheme(dynamicColor = theme.dynamicColor, arabicTextScale = theme.arabicTextScale) {
                SunnahApp(deepLink = pendingDeepLink, onDeepLinkHandled = { pendingDeepLink = null })
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        intent.toDeepLink()?.let { pendingDeepLink = it }
    }

    private fun Intent.toDeepLink(): NavKey? {
        getStringExtra(AppIntents.EXTRA_ITEM_ID)?.let { return ReaderKey(it) }
        val category = getStringExtra(AppIntents.EXTRA_CATEGORY) ?: return null
        return LifeSituation.entries.firstOrNull { it.name == category }?.let(::CategoryKey)
    }
}
