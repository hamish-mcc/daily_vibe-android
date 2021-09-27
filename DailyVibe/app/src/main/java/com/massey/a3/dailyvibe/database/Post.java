package com.massey.a3.dailyvibe.database;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.sql.Date;

@Entity
public class Post {
    @PrimaryKey
    public int uid;

    // TODO Need to solve Cannot figure out how to save this field into database. You can consider adding a type converter for it.
    @ColumnInfo(name = "date")
    public Date date;

    @ColumnInfo(name = "text")
    public String text;

    @ColumnInfo(name = "confidence_positive")
    public Float confidencePositive;

    @ColumnInfo(name = "confidence_negatuve")
    public Float confidenceNegative;
}
