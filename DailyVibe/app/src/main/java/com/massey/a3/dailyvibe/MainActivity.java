package com.massey.a3.dailyvibe;

import android.app.ActionBar;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button goToPosts = findViewById(R.id.postsButton);

        goToPosts.setOnClickListener((View v) -> {
            Intent openPosts = new Intent(MainActivity.this, PostsActivity.class);
            this.startActivity(openPosts);
        });
    }
}