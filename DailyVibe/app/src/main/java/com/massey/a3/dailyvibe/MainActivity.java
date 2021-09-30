package com.massey.a3.dailyvibe;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;

import com.massey.a3.dailyvibe.database.Post;
import com.massey.a3.dailyvibe.database.PostViewModel;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private static final String USE_DATE_FORMAT = "dd/MM/yy";

    private LiveData<List<Float>> mScores;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //UI
        Button goToPosts = findViewById(R.id.postsButton);
        TextView postView = findViewById(R.id.postView);

        goToPosts.setOnClickListener((View v) -> {
            Intent openPosts = new Intent(MainActivity.this, PostsActivity.class);
            this.startActivity(openPosts);
        });

        PostViewModel postViewModel = new PostViewModel(this.getApplication());

        final Observer<Post> postObserver = new Observer<Post>() {
            @Override
            public void onChanged(Post post) {
                // TODO Improve format and can have a button to get a different random post
                DateFormat format = new SimpleDateFormat(USE_DATE_FORMAT, Locale.getDefault());
                String dateString = format.format(post.date);
                String display = String.format("%s\n\t%s", dateString, post.text);
                postView.setText(display);
            }
        };

        postViewModel.getRandom().observe(this, postObserver);

        // TODO Implement chart see https://github.com/PhilJay/MPAndroidChart
    }
}