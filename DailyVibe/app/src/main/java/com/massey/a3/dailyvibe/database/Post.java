package com.massey.a3.dailyvibe.database;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.util.Date;

@Entity(tableName = "posts")
public class Post {
    @PrimaryKey (autoGenerate = true)
    public int uid;

    @ColumnInfo(name = "date")
    public Date date;

    @ColumnInfo(name = "text")
    public String text;

    @ColumnInfo(name = "confidence_positive")
    public Float confidencePositive;

    @ColumnInfo(name = "confidence_negative")
    public Float confidenceNegative;

    public Post(Date date, String text, Float confidencePositive, Float confidenceNegative) {
        this.date = date;
        this.text = text;
        this.confidencePositive = confidencePositive;
        this.confidenceNegative = confidenceNegative;
    }
}
