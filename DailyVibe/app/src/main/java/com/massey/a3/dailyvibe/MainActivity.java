package com.massey.a3.dailyvibe;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Observer;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
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

    private final String[] mPeriod = {"Week", "Month", "Year"};
    private Date mFromDate;

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

        // Set up spinner
        Spinner dateSpinnner = findViewById(R.id.dateSpinner);
        dateSpinnner.setOnItemSelectedListener(this);

        ArrayAdapter<String> aa = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, mPeriod);
        aa.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        dateSpinnner.setAdapter(aa);


        // Get data
        mPostViewModel = new PostViewModel(this.getApplication());

        final Observer<Post> postObserver = new Observer<Post>() {
            @Override
            public void onChanged(Post post) {
                // TODO Improve format and can have a button to get a different random post
                try {
                    DateFormat format = new SimpleDateFormat(USE_DATE_FORMAT, Locale.getDefault());
                    String dateString = format.format(post.date);
                    String display = String.format("%s\n\t%s", dateString, post.text);
                    postView.setText(display);
                } catch (NullPointerException e) {
                    Log.e(TAG, e.getMessage());
                }
            }
        };

        mPostViewModel.getRandom().observe(this, postObserver);

        // TODO Implement chart see https://github.com/PhilJay/MPAndroidChart
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
            Log.i(TAG, "Adding " + posts.size() + " posts");
            ArrayList<Entry> lineEntries = new ArrayList<>();
                for (int i = 0; i < posts.size(); i++) {
                    Post post = posts.get(i);
                    try {
                        lineEntries.add(new Entry(post.date.getTime(),
                                post.confidencePositive - post.confidenceNegative));
                    } catch (NullPointerException e) {
                        lineEntries.add(new Entry(post.date.getTime(), 0));
                    }
                }

                LineDataSet lineDataSet = new LineDataSet(lineEntries, "Score");
                lineDataSet.setDrawCircles(true);
                lineDataSet.setCircleRadius(4);
                lineDataSet.setDrawValues(false);
                lineDataSet.setLineWidth(3);
                lineDataSet.setColor(Color.GREEN);
                lineDataSet.setCircleColor(Color.GREEN);

                List<ILineDataSet> dataSets = new ArrayList<>();
                dataSets.add(lineDataSet);

                LineData lineData = new LineData(dataSets);

                mLineChart.setData(lineData);
                XAxis xAxis = mLineChart.getXAxis();
                xAxis.resetAxisMinimum();
                mLineChart.invalidate();

        }
    };

    private void configureLineChart() {
        Description desc = mLineChart.getDescription();
        desc.setEnabled(false);

        Legend leg = mLineChart.getLegend();
        leg.setEnabled(false);

        XAxis xAxis = mLineChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);

        xAxis.setValueFormatter(new ValueFormatter() {
            private final SimpleDateFormat mFormat = new SimpleDateFormat("dd MMM", Locale.ENGLISH);

            @Override
            public String getFormattedValue(float value) {
                return mFormat.format(new Date((long) value));
            }
        });
    }
}