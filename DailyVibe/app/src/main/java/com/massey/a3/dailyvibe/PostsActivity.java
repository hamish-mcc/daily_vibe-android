package com.massey.a3.dailyvibe;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.massey.a3.dailyvibe.database.Post;
import com.massey.a3.dailyvibe.database.PostViewModel;
import com.massey.a3.tensorflow.lite.textclassification.Result;
import com.massey.a3.tensorflow.lite.textclassification.TextClassificationClient;

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
    private static PostViewModel mPostViewModel;
    private PostsAdapter mPostsAdapter;

    public static class PostsAdapter extends ListAdapter<Post, PostsAdapter.PostViewHolder> {
        // Adapter to display posts
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
            vh.itemView.setOnLongClickListener(v -> {
                TextView postText = v.findViewById(R.id.postText);
                int postId = Integer.parseInt((String) postText.getContentDescription());
                // Long clicking a post opens a dialog for deleting it
                AlertDialog.Builder builder = new AlertDialog.Builder(v.getContext());

                builder.setMessage(R.string.dialog_message)
                        .setCancelable(false)
                        .setPositiveButton("Yes", (dialog, id) -> {
                            // Delete the post
                            mPostViewModel.deletePost(postId);
                        })
                        .setNegativeButton("No", (dialog, id) -> dialog.cancel());
                // Create dialog box
                AlertDialog alert = builder.create();
                // Set title
                alert.setTitle(R.string.dialog_title);
                alert.show();
                return true;
            });
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
                postText.setContentDescription(String.valueOf(p.uid));
                postText.setText(p.text);
                posConfidence.setText(p.confidencePositive.toString());
                String pos = String.format("%s %.2f%%", getEmojiByUnicode(0x1F642), p.confidencePositive*100);
                String neg = String.format("%s %.2f%%", getEmojiByUnicode(0x1F641), p.confidenceNegative*100);
                posConfidence.setText(pos);
                negConfidence.setText(neg);
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

    public class OnSwipeTouchListener implements View.OnTouchListener {
        // User can swipe left and right to change the date open in the journal
        private final GestureDetector gestureDetector;

        public OnSwipeTouchListener (Context ctx){
            gestureDetector = new GestureDetector(ctx, new GestureListener());
        }

        @SuppressLint("ClickableViewAccessibility")
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            return gestureDetector.onTouchEvent(event);
        }

        private final class GestureListener extends GestureDetector.SimpleOnGestureListener {

            private static final int SWIPE_THRESHOLD = 100;
            private static final int SWIPE_VELOCITY_THRESHOLD = 100;

            @Override
            public boolean onDown(MotionEvent e) {
                return true;
            }

            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                boolean result = false;
                try {
                    float diffY = e2.getY() - e1.getY();
                    float diffX = e2.getX() - e1.getX();
                    if (Math.abs(diffX) > Math.abs(diffY)) {
                        if (Math.abs(diffX) > SWIPE_THRESHOLD && Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                            if (diffX > 0) {
                                onSwipeRight();
                            } else {
                                onSwipeLeft();
                            }
                            result = true;
                        }
                    }
                } catch (Exception exception) {
                    exception.printStackTrace();
                }
                return result;
            }
        }

        public void onSwipeRight() {
            // Go back 1 day
            Calendar cal = Calendar.getInstance();
            cal.setTime(mUseDate);
            cal.add(Calendar.DATE, -1);
            mUseDate = cal.getTime();
            mDateString = mDateFormat.format(mUseDate);
            mDateView.setText(mDateString);
            refreshPosts();
        }

        public void onSwipeLeft() {
            // Go forward 1 day
            Calendar cal = Calendar.getInstance();
            long today = cal.getTimeInMillis();
            cal.setTime(mUseDate);
            cal.add(Calendar.DATE, 1);
            // Can't select a date in the future
            if (cal.getTimeInMillis() <= today) {
                mUseDate = cal.getTime();
                mDateString = mDateFormat.format(mUseDate);
                mDateView.setText(mDateString);
                refreshPosts();
            }
        }

    }


    @SuppressLint("ClickableViewAccessibility")
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

        // Swipe left or right changes the date
        postsView.setOnTouchListener(new OnSwipeTouchListener(PostsActivity.this));

        // Show current date
        Date today = Calendar.getInstance().getTime();
        mUseDate = removeTime(today);
        Log.i(TAG, "Using date " + mUseDate.toString());
        mDateFormat = new SimpleDateFormat(USE_DATE_FORMAT, Locale.getDefault());
        mDateString = mDateFormat.format(mUseDate);

        mDateView = findViewById(R.id.dateTextView);
        mDateView.setText(mDateString);

        // Get data from DB
        refreshPosts();

        // Button to open a date picker dialog
        ImageButton dateButton = findViewById(R.id.buttonDateSelect);
        dateButton.setOnClickListener((View v) -> changeDate());

        // Set up sentiment analysis engine
        mClient = new TextClassificationClient(getApplicationContext());
        mHandler = new Handler(Looper.getMainLooper());

        // Input
        mInputPostText = findViewById(R.id.editPost);
        Button postButton = findViewById(R.id.buttonPost);
        // Clicking button creates new post and clears the input text view
        postButton.setOnClickListener((View v) -> {
            newPost(mInputPostText.getText().toString());
            mInputPostText.setText("");
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu){
        // Add tensorflow icon to action bar
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Display app info when icon is clicked
        if (item.getItemId() == R.id.tf_icon) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage(R.string.tf_info)
                    .setCancelable(false)
                    .setNeutralButton("Close", (dialog, id) -> dialog.cancel());
            // Create dialog box
            AlertDialog alert = builder.create();
            // Set title
            alert.setTitle(R.string.info_dialog_title);
            alert.show();
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
                });
        refreshPosts();
    }

    private void changeDate() {
        // Select a new date using a date picker dialog
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(mUseDate);
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(PostsActivity.this,
                (datePicker, newYear, newMonth, newDay) -> {
                    Calendar cal = Calendar.getInstance();
                    cal.setTimeInMillis(0);
                    cal.set(newYear, newMonth, newDay, 0, 0, 0);
                    mUseDate = cal.getTime();
                    mDateString = mDateFormat.format(mUseDate);
                    mDateView.setText(mDateString);
                    Log.i(TAG, "Using date " + mUseDate.toString());
                    refreshPosts();
                }, year, month, dayOfMonth);
        datePickerDialog.getDatePicker().setMaxDate(calendar.getTimeInMillis());
        datePickerDialog.show();
    }

    public static Date removeTime(Date date) {
        // Remove time of day, as posts are saved by date only
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
        // Get data from the database
        Log.i(TAG, "Get posts for " + mUseDate.toString());
        mPostViewModel = new PostViewModel(this.getApplication());
        mPostViewModel.getAllPostsByDate(mUseDate).observe(this, mPostsAdapter::submitList);
    }
}