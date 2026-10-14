package com.sunnahcompanion.app.ui.components

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Mosque
import androidx.compose.material.icons.rounded.SelfImprovement
import androidx.compose.material.icons.rounded.Terrain
import androidx.compose.material.icons.rounded.Umbrella
import androidx.compose.material.icons.rounded.VolunteerActivism
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material.icons.rounded.WbTwilight
import androidx.compose.ui.graphics.vector.ImageVector
import com.sunnahcompanion.app.R
import com.sunnahcompanion.app.core.database.model.LifeSituation

@get:StringRes
val LifeSituation.titleRes: Int
    get() = when (this) {
        LifeSituation.LEAVING_HOME -> R.string.situation_leaving_home
        LifeSituation.ENTERING_HOME -> R.string.situation_entering_home
        LifeSituation.DRIVING -> R.string.situation_driving
        LifeSituation.ANXIOUS -> R.string.situation_anxious
        LifeSituation.RAIN -> R.string.situation_rain
        LifeSituation.HARDSHIP -> R.string.situation_hardship
        LifeSituation.GRATITUDE -> R.string.situation_gratitude
        LifeSituation.BEFORE_SLEEP -> R.string.situation_before_sleep
        LifeSituation.MORNING -> R.string.situation_morning
        LifeSituation.EVENING -> R.string.situation_evening
        LifeSituation.AFTER_FARD_PRAYER -> R.string.situation_after_fard_prayer
        LifeSituation.FRIDAY -> R.string.situation_friday
    }

val LifeSituation.icon: ImageVector
    get() = when (this) {
        LifeSituation.LEAVING_HOME -> Icons.AutoMirrored.Rounded.Logout
        LifeSituation.ENTERING_HOME -> Icons.Rounded.Home
        LifeSituation.DRIVING -> Icons.Rounded.DirectionsCar
        LifeSituation.ANXIOUS -> Icons.Rounded.Favorite
        LifeSituation.RAIN -> Icons.Rounded.Umbrella
        LifeSituation.HARDSHIP -> Icons.Rounded.Terrain
        LifeSituation.GRATITUDE -> Icons.Rounded.VolunteerActivism
        LifeSituation.BEFORE_SLEEP -> Icons.Rounded.Bedtime
        LifeSituation.MORNING -> Icons.Rounded.WbSunny
        LifeSituation.EVENING -> Icons.Rounded.WbTwilight
        LifeSituation.AFTER_FARD_PRAYER -> Icons.Rounded.SelfImprovement
        LifeSituation.FRIDAY -> Icons.Rounded.Mosque
    }
