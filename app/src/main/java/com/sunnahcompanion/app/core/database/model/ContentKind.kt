package com.sunnahcompanion.app.core.database.model

/** Persisted by [name]; never rename an entry without a migration. */
enum class ContentKind {
    /** A supplication from the Sunnah. */
    DUA,

    /** Quranic recitation, with its verses stored in `ayahs`. */
    SURAH,

    /** A remembrance recited a set number of times at a set time (e.g. morning/evening). */
    ADHKAR,

    /** A short phrase counted on the Tasbih screen. */
    DHIKR,

    /** A prayer (salah): the five fard prayers, Witr and the rawatib. */
    PRAYER,
}
