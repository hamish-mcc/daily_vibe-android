package com.massey.a3.dailyvibe.database;

import androidx.room.TypeConverter;

import java.util.Date;

// The converter is used to convert Date to Long (and vice-versa)
// SQLite cannot store Date types, so Long is used instead

public class Converters {
    @TypeConverter
    public static Date fromTimestamp(Long value) {
        return value == null ? null : new Date(value);
    }

    @TypeConverter
    public static Long dateToTimestamp(Date date) {
        return date == null ? null : date.getTime();
    }
}
