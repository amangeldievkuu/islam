package com.sunnahcompanion.app.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.sunnahcompanion.app.ui.theme.SunnahTheme

/** Arabic text: RTL, right-aligned, Uthmani font, tagged `ar` so TalkBack reads it in Arabic. */
@Composable
fun ArabicText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = SunnahTheme.arabic.body,
    maxLines: Int = Int.MAX_VALUE,
    textAlign: TextAlign = TextAlign.Right,
) {
    Text(
        text = text,
        modifier = modifier.fillMaxWidth(),
        style = style.copy(localeList = LocaleList("ar"), textAlign = textAlign),
        color = LocalContentColor.current,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
    )
}
