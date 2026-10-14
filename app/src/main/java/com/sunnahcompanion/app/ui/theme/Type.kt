package com.sunnahcompanion.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.sunnahcompanion.app.R

/** Material 3 defaults for Latin text. */
val Typography = Typography()

/** Amiri Quran renders the Uthmani script (small high letters, pause marks) correctly. */
val ArabicFontFamily = FontFamily(Font(R.font.amiri_quran))

/**
 * Arabic text styles. Always right-aligned and RTL regardless of the UI locale, with generous
 * line height so stacked diacritics never clip.
 */
@Immutable
data class ArabicTypography(
    val display: TextStyle,
    val body: TextStyle,
    val compact: TextStyle,
)

internal fun arabicTypography(scale: Float): ArabicTypography {
    fun style(size: TextUnit, lineHeight: TextUnit) = TextStyle(
        fontFamily = ArabicFontFamily,
        fontSize = size * scale,
        lineHeight = lineHeight * scale,
        textDirection = TextDirection.Rtl,
        textAlign = TextAlign.Right,
        lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.None),
    )
    return ArabicTypography(
        display = style(40.sp, 80.sp),
        body = style(28.sp, 60.sp),
        compact = style(22.sp, 44.sp),
    )
}
