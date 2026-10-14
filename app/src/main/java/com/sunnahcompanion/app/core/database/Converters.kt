package com.sunnahcompanion.app.core.database

import androidx.room.TypeConverter
import com.sunnahcompanion.app.core.database.model.DeedPriority
import java.time.LocalDate
import kotlin.time.Instant

/** Other enums need no converter: Room stores them as their TEXT name. */
internal class Converters {

    /** Stored as epoch day so dates sort and range-compare as plain integers. */
    @TypeConverter
    fun localDateToEpochDay(date: LocalDate?): Long? = date?.toEpochDay()

    @TypeConverter
    fun epochDayToLocalDate(epochDay: Long?): LocalDate? = epochDay?.let(LocalDate::ofEpochDay)

    /** Stored as epoch millis, the same encoding schema v1 used for plain Long timestamps. */
    @TypeConverter
    fun instantToEpochMillis(instant: Instant?): Long? = instant?.toEpochMilliseconds()

    @TypeConverter
    fun epochMillisToInstant(millis: Long?): Instant? = millis?.let(Instant::fromEpochMilliseconds)

    /** Stored as its rank so that `ORDER BY priority` puts Fard first. */
    @TypeConverter
    fun deedPriorityToRank(priority: DeedPriority?): Int? = priority?.rank

    @TypeConverter
    fun rankToDeedPriority(rank: Int?): DeedPriority? = rank?.let(DeedPriority::fromRank)
}
