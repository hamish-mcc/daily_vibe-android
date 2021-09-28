package com.massey.a3.dailyvibe.database;

import android.app.Application;
import android.util.Log;

import androidx.lifecycle.LiveData;

import java.util.Date;
import java.util.List;

public class PostRepository {
    private static final String TAG = "PostRepository";
    private PostDao mPostDao;
    private LiveData<List<Post>> mAllPostsByDate;
    private Post randomPost;

    PostRepository(Application application, Date date) {
        PostDatabase db = PostDatabase.getDatabase(application);
        mPostDao = db.postDao();
        mAllPostsByDate = mPostDao.getAllByDate(date);
        Log.i(TAG, "Got posts for " + date.toString());
    }

    // Room executes all queries on a separate thread.
    // Observed LiveData will notify the observer when the data has changed.
    LiveData<List<Post>> getAllPostsByDate() {
        return mAllPostsByDate;
    }

    // You must call this on a non-UI thread or your app will throw an exception. Room ensures
    // that you're not doing any long running operations on the main thread, blocking the UI.
    void insert(Post post) {
        PostDatabase.databaseWriteExecutor.execute(() -> {
            Log.i(TAG, "Inserted " + post.text + " on " + post.date.toString());
            mPostDao.insertPost(post);
        });
    }
}
