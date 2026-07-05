package com.rbmr.simpletodos.data

import androidx.room.TypeConverter
import java.util.UUID

class Converters {
    @TypeConverter
    fun fromUuid(uuid: UUID): String = uuid.toString()

    @TypeConverter
    fun toUuid(value: String): UUID = UUID.fromString(value)
}
