package com.massey.a3.dailyvibe.database;

import androidx.lifecycle.LiveData;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;

import java.sql.Date;
import java.util.List;

public interface PostDao {
    @Query("SELECT * FROM post WHERE date = (:date)")
    LiveData<List<Post>> getAllByDate(Date date);

    @Query("SELECT * FROM post ORDER BY RANDOM() LIMIT 1")
    Post getRandom();

    @Insert
    void insertPost(Post post);

    @Delete
    void deletePost(Post post);

    // TODO Write queries for data analysis and visualisation here. Use LiveData
}
