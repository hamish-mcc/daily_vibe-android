package com.massey.a3.dailyvibe;

import androidx.appcompat.app.AppCompatActivity;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ScrollView;
import android.widget.TextView;

import com.massey.a3.tensorflow.lite.textclassification.TextClassificationClient;
import com.massey.a3.tensorflow.lite.textclassification.Result;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class PostsActivity extends AppCompatActivity {
    private static final String TAG = "PostsActivity";
    private static final String USE_DATE_FORMAT = "dd MMM yyyy";

    private TextClassificationClient mClient;

    private TextView mPostsTextView;
    private EditText mInputPostText;
    private Handler mHandler;
    private ScrollView mScrollView;
    private TextView mDateView;
    private Date mUseDate;
    private SimpleDateFormat mDateFormat;
    private String mDateString;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_posts);
        Log.i(TAG, "onCreate");

        // Show current date
        mUseDate = Calendar.getInstance().getTime();
        mDateFormat = new SimpleDateFormat(USE_DATE_FORMAT, Locale.getDefault());
        mDateString = mDateFormat.format(mUseDate);

        mDateView = findViewById(R.id.dateTextView);
        mDateView.setText(mDateString);

        ImageButton dateButton = findViewById(R.id.buttonDateSelect);

        dateButton.setOnClickListener((View v) -> {
            changeDate();
        });

        mClient = new TextClassificationClient(getApplicationContext());
        mHandler = new Handler(Looper.getMainLooper());

        Button postButton = findViewById(R.id.buttonPost);

        postButton.setOnClickListener((View v) -> {
            newPost(mInputPostText.getText().toString());
        });

        mPostsTextView = findViewById(R.id.viewPosts);
        mInputPostText = findViewById(R.id.editPost);
        mScrollView = findViewById(R.id.scroll_view);
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.v(TAG, "onStart");
        mHandler.post(
                () -> {
                    mClient.load();
                });
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.v(TAG, "onStop");
        mHandler.post(
                () -> {
                    mClient.unload();
                });
    }

    private void newPost(final String text) {
        mHandler.post(
                () -> {
                    // Run text classification with TF Lite.
                    List<Result> results = mClient.classify(text);

                    // Show classification result on screen
                    //showResult(text, results);

                    Bundle resultBundle = bundleResult(text, results);
                    showResult(resultBundle);

                });
    }

    // TODO Create a method that adds the post to the display (however it is being displayed)
    private void showResult(Bundle resultBundle) {
        // Run on UI thread as we'll updating our app UI
        runOnUiThread(
                () -> {
                    // Append the result to the UI.
                    mPostsTextView.append(resultBundle.toString() + "\n\n");

                    // Clear the input text.
                    mInputPostText.getText().clear();

                    // Scroll to the bottom to show latest entry's classification result.
                    mScrollView.post(() -> mScrollView.fullScroll(View.FOCUS_DOWN));
                });
    }

    private Bundle bundleResult(final String inputText, final List<Result> resultList) {
        Bundle resultBundle = new Bundle();
        resultBundle.putSerializable("date", mUseDate);
        resultBundle.putString("post", inputText);
        for (int i = 0; i < resultList.size(); i++) {
            Result result = resultList.get(i);
            resultBundle.putFloat(result.getTitle(), result.getConfidence());
        }
        return resultBundle;
    }

    private void changeDate() {
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(PostsActivity.this,
                new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker datePicker, int year, int month, int day) {
                        Calendar cal = Calendar.getInstance();
                        cal.setTimeInMillis(0);
                        cal.set(year, month, day, 0, 0, 0);
                        mUseDate = cal.getTime();
                        mDateString = mDateFormat.format(mUseDate);
                        mDateView.setText(mDateString);
                    }
                }, year, month, dayOfMonth);
        datePickerDialog.show();
    }

}