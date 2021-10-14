package com.massey.a3.dailyvibe.database;

import android.app.Application;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import java.util.Date;
import java.util.List;

// Design pattern for interacting with the DB reference from Android Room tutorial
// https://developer.android.com/codelabs/android-room-with-a-view#0

public class PostViewModel extends AndroidViewModel {
    private final PostRepository mRepository;

    public PostViewModel(Application application) {
        super(application);
        mRepository = new PostRepository(application);
    }

    public LiveData<List<Post>> getAllPostsByDate(Date date) {
        return mRepository.getAllPostsByDate(date);
    }

    public LiveData<Post> getRandom() { return mRepository.getRandom(); }

    public LiveData<List<Post>> getPostsAfter(Date date) { return mRepository.getPostsAfter(date); }

    public void insert(Post post) {
        mRepository.insert(post);
    }

    public void deletePost(int uid) {mRepository.deletePost(uid);}

}
