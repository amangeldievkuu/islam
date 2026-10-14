package com.sunnahcompanion.app.notification

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.sunnahcompanion.app.MainActivity
import com.sunnahcompanion.app.core.database.model.LifeSituation

/** Intents that open the app at a specific item or category (notification taps). */
object AppIntents {
    const val EXTRA_ITEM_ID = "com.sunnahcompanion.app.extra.ITEM_ID"
    const val EXTRA_CATEGORY = "com.sunnahcompanion.app.extra.CATEGORY"

    fun openItem(context: Context, itemId: String): PendingIntent = PendingIntent.getActivity(
        context,
        itemId.hashCode(),
        Intent(context, MainActivity::class.java)
            .putExtra(EXTRA_ITEM_ID, itemId)
            .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    fun openCategory(context: Context, category: LifeSituation): PendingIntent = PendingIntent.getActivity(
        context,
        category.name.hashCode(),
        Intent(context, MainActivity::class.java)
            .putExtra(EXTRA_CATEGORY, category.name)
            .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
}
