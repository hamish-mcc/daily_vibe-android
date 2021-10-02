package com.massey.a3.dailyvibe;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Observer;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.massey.a3.dailyvibe.database.Post;
import com.massey.a3.dailyvibe.database.PostViewModel;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener {
    private static final String TAG = "MainActivity";
    private static final String USE_DATE_FORMAT = "dd/MM/yy";

    private LineChart mLineChart;
    private PostViewModel mPostViewModel;

    private final String[] mPeriod = {"This Week", "This Month", "This Year"};
    private Date mFromDate;

    private Observer<Post> mPostObserver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //UI
        Button goToPosts = findViewById(R.id.postsButton);
        ImageButton refreshRandomPost = findViewById(R.id.refreshButton);
        TextView postDateView = findViewById(R.id.postDateView);
        TextView postTextView = findViewById(R.id.postTextView);

        goToPosts.setOnClickListener((View v) -> {
            Intent openPosts = new Intent(MainActivity.this, PostsActivity.class);
            this.startActivity(openPosts);
        });

        refreshRandomPost.setOnClickListener((View v) -> {
            mPostViewModel.getRandom().observe(this, mPostObserver);
        });

        // Set up spinner
        Spinner dateSpinner = findViewById(R.id.dateSpinner);
        dateSpinner.setOnItemSelectedListener(this);

        ArrayAdapter<String> aa = new ArrayAdapter<>(this,
                R.layout.spinner_item, mPeriod);
        aa.setDropDownViewResource(R.layout.spinner_item);
        dateSpinner.setAdapter(aa);


        // Get data
        mPostViewModel = new PostViewModel(this.getApplication());

        mPostObserver = new Observer<Post>() {
            @Override
            public void onChanged(Post post) {
                try {
                    DateFormat format = new SimpleDateFormat(USE_DATE_FORMAT, Locale.getDefault());
                    String dateString = format.format(post.date);
                    String displayDate = String.format("A reflection from %s", dateString);
                    postDateView.setText(displayDate);
                    String displayText = String.format("\"%s\"", post.text);
                    postTextView.setText(displayText);
                } catch (NullPointerException e) {
                    Log.e(TAG, e.getMessage());
                }
            }
        };

        mPostViewModel.getRandom().observe(this, mPostObserver);

        mLineChart = findViewById(R.id.activity_main_linechart);
        configureLineChart();
    }

    //Performing action onItemSelected and onNothing selected
    @Override
    public void onItemSelected(AdapterView<?> arg0, View arg1, int position, long id) {
        // Update the history interval and retrieve posts
        Calendar calendar = Calendar.getInstance();
        calendar.getTime();
        switch (position) {
            case 0:
                calendar.add(Calendar.DATE, -7);
                break;
            case 1:
                calendar.add(Calendar.DATE, -30);
                break;
            case 2:
                calendar.add(Calendar.DATE, -365);
                break;
            default:
                break;
        }
        mFromDate = calendar.getTime();
        Log.i(TAG, "From date " + mFromDate.toString());
        mPostViewModel.getPostsAfter(mFromDate).observe(this, dataObserver);
    }

    @Override
    public void onNothingSelected(AdapterView<?> arg0) {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DATE, -7);
        mFromDate = calendar.getTime();
        Log.i(TAG, "From date " + mFromDate.toString());
        mPostViewModel.getPostsAfter(mFromDate).observe(this, dataObserver);
    }

    public final Observer<List<Post>> dataObserver = new Observer<List<Post>>() {
        @Override
        public void onChanged(List<Post> posts) {
            updateLineChart(posts);
        }
    };

    private void configureLineChart() {
        mLineChart.getDescription().setEnabled(false);

        Legend leg = mLineChart.getLegend();
        leg.setEnabled(true);
        leg.setTextSize(13);
        leg.setHorizontalAlignment(Legend.LegendHorizontalAlignment.RIGHT);
        leg.setVerticalAlignment(Legend.LegendVerticalAlignment.TOP);

        YAxis left = mLineChart.getAxisLeft();
        left.setDrawZeroLine(true);
        left.setZeroLineWidth(2);
        left.setAxisMinimum(-1);
        left.setAxisMaximum(1);
        left.setTextSize(13);

        mLineChart.getAxisRight().setEnabled(false);

        XAxis xAxis = mLineChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setTextSize(13);
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM_INSIDE);

        xAxis.setValueFormatter(new ValueFormatter() {
            private final SimpleDateFormat mFormat = new SimpleDateFormat("dd MMM", Locale.ENGLISH);

            @Override
            public String getFormattedValue(float value) {
                return mFormat.format(new Date((long) value));
            }
        });
    }

    private void updateLineChart(List<Post> posts) {
        Log.i(TAG, "Adding " + posts.size() + " posts");
        ArrayList<Entry> lineEntries = new ArrayList<>();
        for (int i = 0; i < posts.size(); i++) {
            Post post = posts.get(i);
            try {
                lineEntries.add(new Entry(post.date.getTime(),
                        post.confidencePositive - post.confidenceNegative));
                Log.i(TAG, post.confidencePositive.toString() + " " + post.confidenceNegative.toString());
            } catch (NullPointerException e) {
                Log.e(TAG, e.getMessage());
                lineEntries.add(new Entry(post.date.getTime(), 0));
            }
        }

        LineDataSet lineDataSet = new LineDataSet(lineEntries, "Net Sentiment");
        lineDataSet.setDrawCircles(false);
        lineDataSet.setDrawValues(false);
        lineDataSet.setLineWidth(2);
        lineDataSet.setColor(Color.RED);
        lineDataSet.setCircleColor(Color.RED);

        List<ILineDataSet> dataSets = new ArrayList<>();
        dataSets.add(lineDataSet);

        LineData lineData = new LineData(dataSets);

        mLineChart.setData(lineData);

        XAxis xAxis = mLineChart.getXAxis();
        xAxis.resetAxisMinimum();
        xAxis.resetAxisMaximum();

        mLineChart.invalidate();
    }
}