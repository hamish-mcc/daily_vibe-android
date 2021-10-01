package com.massey.a3.dailyvibe;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import android.annotation.SuppressLint;
import android.app.DatePickerDialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.massey.a3.dailyvibe.database.Post;
import com.massey.a3.dailyvibe.database.PostViewModel;
import com.massey.a3.tensorflow.lite.textclassification.TextClassificationClient;
import com.massey.a3.tensorflow.lite.textclassification.Result;

import org.jetbrains.annotations.NotNull;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

public class PostsActivity extends AppCompatActivity {
    private static final String TAG = "PostsActivity";
    private static final String USE_DATE_FORMAT = "dd MMM yyyy";

    private TextClassificationClient mClient;

    private EditText mInputPostText;
    private Handler mHandler;
    private TextView mDateView;
    private Date mUseDate;
    private SimpleDateFormat mDateFormat;
    private String mDateString;
    private PostViewModel mPostViewModel;
    private PostsAdapter mPostsAdapter;

    public static class PostsAdapter extends ListAdapter<Post, PostsAdapter.PostViewHolder> {

        protected PostsAdapter(DiffUtil.ItemCallback<Post> diffCallback) {
            super(diffCallback);
        }

        @Override
        public @NotNull PostViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
            View postItem = layoutInflater.inflate(R.layout.post_item, parent, false);
            return new PostViewHolder(postItem);
        }

        @Override
        public void onBindViewHolder(PostViewHolder vh, int position) {
            Post current = getItem(position);
            vh.bind(current);
        }

        public static class PostViewHolder extends RecyclerView.ViewHolder {
            public TextView postText;
            public TextView posConfidence;
            public TextView negConfidence;
            public LinearLayout postLayout;

            public PostViewHolder(View postView) {
                super(postView);
                this.postText = postView.findViewById(R.id.postText);
                this.posConfidence = postView.findViewById(R.id.posView);
                this.negConfidence = postView.findViewById(R.id.negView);
                this.postLayout = postView.findViewById(R.id.postLayout);
            }

            @SuppressLint({"SetTextI18n", "DefaultLocale"})
            public void bind(Post p) {
                postText.setText(p.text);
                posConfidence.setText(p.confidencePositive.toString());
                // TODO Implement a range of emojis depending on score
                //  https://www.unicode.org/emoji/charts/full-emoji-list.html
                String pos = String.format("%s %.2f%%", getEmojiByUnicode(0x1F642), p.confidencePositive*100);
                String neg = String.format("%s %.2f%%", getEmojiByUnicode(0x1F641), p.confidenceNegative*100);
                posConfidence.setText(pos);
                negConfidence.setText(neg);
                // TODO Set an emoji based on confidence
            }
        }

        static class PostDiff extends DiffUtil.ItemCallback<Post> {

            @Override
            public boolean areItemsTheSame(@NotNull Post oldItem, @NotNull Post newItem) {
                return oldItem == newItem;
            }

            @Override
            public boolean areContentsTheSame(Post oldItem, Post newItem) {
                return oldItem.uid == newItem.uid;
            }
        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_posts);
        Log.i(TAG, "onCreate");

        // Set up recycler view and adapter
        RecyclerView postsView = findViewById(R.id.postsView);
        mPostsAdapter = new PostsAdapter(new PostsAdapter.PostDiff());
        postsView.setAdapter(mPostsAdapter);
        postsView.setLayoutManager(new LinearLayoutManager(this));

        // Show current date
        Date today = Calendar.getInstance().getTime();
        mUseDate = removeTime(today);
        Log.i(TAG, "Using date " + mUseDate.toString());
        mDateFormat = new SimpleDateFormat(USE_DATE_FORMAT, Locale.getDefault());
        mDateString = mDateFormat.format(mUseDate);

        mDateView = findViewById(R.id.dateTextView);
        mDateView.setText(mDateString);

        // Connect to DB
        refreshPosts();

        // Change the date
        ImageButton dateButton = findViewById(R.id.buttonDateSelect);

        dateButton.setOnClickListener((View v) -> changeDate());

        // Set up sentiment analysis
        mClient = new TextClassificationClient(getApplicationContext());
        mHandler = new Handler(Looper.getMainLooper());

        // Input
        mInputPostText = findViewById(R.id.editPost);
        Button postButton = findViewById(R.id.buttonPost);

        postButton.setOnClickListener((View v) -> {
            newPost(mInputPostText.getText().toString());
            mInputPostText.setText("");
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        Log.i(TAG, "Inflate menu");
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.posts_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.deletePosts) {
            mPostViewModel.deleteAll();
            return true;
        }
        return false;
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.v(TAG, "onStart");
        mHandler.post(
                () -> mClient.load());
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.v(TAG, "onStop");
        mHandler.post(
                () -> mClient.unload());
    }

    private void newPost(final String text) {
        mHandler.post(
                () -> {
                    // Run text classification with TF Lite.
                    List<Result> results = mClient.classify(text);
                    // Map results
                    HashMap<String, Float> confidence = new HashMap<>();
                    for (int i = 0; i < results.size(); i++) {
                        Result result = results.get(i);
                        confidence.put(result.getTitle(), result.getConfidence());
                    }
                    // Create post object and insert into db
                    Post post = new Post(mUseDate, text, confidence.get("positive"),
                            confidence.get("negative"));
                    mPostViewModel.insert(post);
                    refreshPosts();
                });
    }

    private void changeDate() {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(mUseDate);
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(PostsActivity.this,
                (datePicker, year1, month1, day) -> {
                    Calendar cal = Calendar.getInstance();
                    cal.setTimeInMillis(0);
                    cal.set(year1, month1, day, 0, 0, 0);
                    mUseDate = cal.getTime();
                    mDateString = mDateFormat.format(mUseDate);
                    mDateView.setText(mDateString);
                    Log.i(TAG, "Using date " + mUseDate.toString());
                    refreshPosts();
                }, year, month, dayOfMonth);
        datePickerDialog.show();
    }

    public static Date removeTime(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();
    }

    public static String getEmojiByUnicode(int unicode){
        return new String(Character.toChars(unicode));
    }

    public void refreshPosts() {
        Log.i(TAG, "getPosts" + mUseDate.toString());
        mPostViewModel = new PostViewModel(this.getApplication());
        mPostViewModel.getAllPostsByDate(mUseDate).observe(this, mPostsAdapter::submitList);
    }
}