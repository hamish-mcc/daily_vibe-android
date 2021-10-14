package com.massey.a3.dailyvibe.database;

import android.app.Application;
import android.util.Log;

import androidx.lifecycle.LiveData;

import java.util.Date;
import java.util.List;

// Design pattern for interacting with the DB reference from Android Room tutorial
// https://developer.android.com/codelabs/android-room-with-a-view#0

public class PostRepository {
    private static final String TAG = "PostRepository";
    private final PostDao mPostDao;

    PostRepository(Application application) {
        PostDatabase db = PostDatabase.getDatabase(application);
        mPostDao = db.postDao();
        Log.i(TAG, "Initialized DB");
    }

    // Observed LiveData will notify the observer when the data has changed.
    LiveData<List<Post>> getAllPostsByDate(Date date) {
        return mPostDao.getAllByDate(date);
    }

    // Posts are deleted on a background thread
    void deletePost(int uid) {
        PostDatabase.databaseWriteExecutor.execute(() -> {
            Log.i(TAG, "Deleting post " + uid);
            mPostDao.deletePost(uid);
        });
    }

    LiveData<Post> getRandom() { return mPostDao.getRandom(); }

    LiveData<List<Post>> getPostsAfter(Date date) {
        Log.i(TAG, "Getting scores after " + date.toString());
        return mPostDao.getPostsAfter(date);
    }

    // Posts are inserted on a background thread
    void insert(Post post) {
        PostDatabase.databaseWriteExecutor.execute(() -> {
            Log.i(TAG, "Inserted " + post.text + " on " + post.date.toString());
            mPostDao.insertPost(post);
        });
    }
}
