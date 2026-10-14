package com.sunnahcompanion.app.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Spa
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Spa
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation3.runtime.NavKey
import com.sunnahcompanion.app.R
import com.sunnahcompanion.app.core.database.model.LifeSituation
import kotlinx.serialization.Serializable

@Serializable
data object HomeKey : NavKey

@Serializable
data object TasbihKey : NavKey

@Serializable
data object SettingsKey : NavKey

@Serializable
data class CategoryKey(val category: LifeSituation) : NavKey

@Serializable
data class ReaderKey(val itemId: String) : NavKey

enum class TopLevelDestination(
    val key: NavKey,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
) {
    HOME(HomeKey, R.string.nav_home, Icons.Rounded.WbSunny, Icons.Outlined.WbSunny),
    TASBIH(TasbihKey, R.string.nav_tasbih, Icons.Rounded.Spa, Icons.Outlined.Spa),
    SETTINGS(SettingsKey, R.string.nav_settings, Icons.Rounded.Settings, Icons.Outlined.Settings),
}
