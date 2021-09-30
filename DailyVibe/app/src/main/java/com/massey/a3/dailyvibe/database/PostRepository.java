package com.massey.a3.dailyvibe.database;

import android.app.Application;
import android.util.Log;

import androidx.lifecycle.LiveData;

import java.util.Date;
import java.util.List;

public class PostRepository {
    private static final String TAG = "PostRepository";
    private final PostDao mPostDao;
    private LiveData<List<Post>> mAllPostsByDate;
    private LiveData<List<Post>> mAllPosts;

    PostRepository(Application application, Date date) {
        PostDatabase db = PostDatabase.getDatabase(application);
        mPostDao = db.postDao();
        mAllPostsByDate = mPostDao.getAllByDate(date);
        Log.i(TAG, "Got posts for " + date.toString());
    }

    PostRepository(Application application) {
        PostDatabase db = PostDatabase.getDatabase(application);
        mPostDao = db.postDao();
        Log.i(TAG, "Initialized DB");
    }

    // Room executes all queries on a separate thread.
    // Observed LiveData will notify the observer when the data has changed.
    LiveData<List<Post>> getAllPostsByDate() {
        return mAllPostsByDate;
    }

    void deleteAll() { mPostDao.deleteAllPosts(); }

    LiveData<Post> getRandom() { return mPostDao.getRandom(); }

    LiveData<List<Float>> getScores() { return mPostDao.getScores(); }

    // You must call this on a non-UI thread or your app will throw an exception. Room ensures
    // that you're not doing any long running operations on the main thread, blocking the UI.
    void insert(Post post) {
        PostDatabase.databaseWriteExecutor.execute(() -> {
            Log.i(TAG, "Inserted " + post.text + " on " + post.date.toString());
            mPostDao.insertPost(post);
        });
    }
}
