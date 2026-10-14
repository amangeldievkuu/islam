package com.sunnahcompanion.app.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.sunnahcompanion.app.R

/**
 * Learning Mode: shows a "tap to reveal" placeholder instead of [text] until [revealed].
 * Used to self-test memorisation of the transliteration.
 */
@Composable
fun RevealableText(
    text: String,
    hidden: Boolean,
    onReveal: () -> Unit,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.bodyLarge,
) {
    AnimatedContent(
        targetState = hidden,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        modifier = modifier,
        label = "reveal",
    ) { isHidden ->
        if (isHidden) {
            Surface(
                onClick = onReveal,
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Rounded.VisibilityOff, contentDescription = null)
                    Text(
                        text = stringResource(R.string.learning_tap_to_reveal),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        } else {
            Text(text = text, style = style, modifier = Modifier.fillMaxWidth())
        }
    }
}
