package com.massey.a3.dailyvibe.database;

import android.app.Application;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import java.util.Date;
import java.util.List;

public class PostViewModel extends AndroidViewModel {
    private PostRepository mRepository;

    private final LiveData<List<Post>> mAllPostsByDate;

    public PostViewModel(Application application, Date date) {
        super(application);
        mRepository = new PostRepository(application, date);
        mAllPostsByDate = mRepository.getAllPostsByDate();
    }

    public LiveData<List<Post>> getAllPostsByDate() {
        return mAllPostsByDate;
    }

    public void insert(Post post) {
        mRepository.insert(post);
    }
}
