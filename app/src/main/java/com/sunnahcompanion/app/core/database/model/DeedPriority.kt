package com.sunnahcompanion.app.core.database.model

/**
 * The hierarchy of deeds: obligations first, then what the Prophet ﷺ was constant upon,
 * then recommended acts and adab. Persisted as [rank] (see `Converters`) so that
 * `ORDER BY priority` in SQL follows this order. Never change an existing rank.
 */
enum class DeedPriority(val rank: Int) {
    /** Парз: obligatory, e.g. the five daily prayers. */
    FARD(10),

    /** Важиб: necessary in the Hanafi school, e.g. Witr. */
    WAJIB(20),

    /** Бекитилген сүннөт: emphasised Sunnah, e.g. the two rak'ahs before Fajr. */
    SUNNAH_MUAKKADAH(30),

    /** Адеп: recommended acts, adhkar and duas. */
    MUSTAHAB(40),
    ;

    companion object {
        fun fromRank(rank: Int): DeedPriority =
            entries.firstOrNull { it.rank == rank } ?: error("Unknown DeedPriority rank $rank")
    }
}
