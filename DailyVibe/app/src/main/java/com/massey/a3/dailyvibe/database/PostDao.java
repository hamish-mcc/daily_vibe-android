package com.massey.a3.dailyvibe.database;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.Date;
import java.util.HashMap;
import java.util.List;

@Dao
public interface PostDao {
    @Query("SELECT * FROM posts WHERE date = (:date)")
    LiveData<List<Post>> getAllByDate(Date date);

    @Query("SELECT * FROM posts ORDER BY RANDOM() LIMIT 1")
    LiveData<Post> getRandom();

    @Insert
    void insertPost(Post post);

    @Delete
    void deletePost(Post post);

    @Query("DELETE FROM posts")
    void deleteAllPosts();

    @Query("SELECT * FROM posts WHERE date > :from")
    LiveData<List<Post>> getPostsAfter(Date from);

    // TODO Querying the variance might also be useful?
}
