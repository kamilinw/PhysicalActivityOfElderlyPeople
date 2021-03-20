package com.kamilwnek.pracainzynierskakamilwnek;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.widget.TextView;

import com.kamilwnek.pracainzynierskakamilwnek.Pedometer.Database;
import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;

import java.util.List;

public class Statistics extends AppCompatActivity {

    TextView textViewStepsRecordValue;
    TextView textViewAllStepsValue;
    TextView textViewAverageStepsValue;
    TextView textViewActivitiesCountValue;
    TextView textViewLongestActivityValue;
    TextView textViewLongestActivityTimeValue;
    TextView textViewPaceRecordValue;
    TextView textViewTotalDistanceValue;
    TextView textViewTotalTimeValue;
    TextView textViewCaloriesBurnedValue;

    int activitiesCount;
    double longestDistance;
    int longestTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_statistics);
        getSupportActionBar().setTitle(R.string.stats);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getWindow().setStatusBarColor(getResources().getColor(R.color.colorPrimary));

        textViewStepsRecordValue = findViewById(R.id.textViewStepsRecordValue);
        textViewAllStepsValue = findViewById(R.id.textViewAllStepsValue);
        textViewAverageStepsValue = findViewById(R.id.textViewAverageStepsValue);
        textViewActivitiesCountValue = findViewById(R.id.textViewActivitiesCountValue);
        textViewLongestActivityValue = findViewById(R.id.textViewLongestActivityValue);
        textViewLongestActivityTimeValue = findViewById(R.id.textViewLongestActivityTimeValue);
        textViewPaceRecordValue = findViewById(R.id.textViewPaceRecordValue);
        textViewTotalDistanceValue = findViewById(R.id.textViewTotalDistanceValue);
        textViewTotalTimeValue = findViewById(R.id.textViewTotalTimeValue);
        textViewCaloriesBurnedValue = findViewById(R.id.textViewCaloriesBurnedValue);

        Database database = Database.getInstance(this);
        int stepsRecord = database.getRecord();
        textViewStepsRecordValue.setText(String.valueOf(stepsRecord));

        long allSteps = database.getAllSteps();
        textViewAllStepsValue.setText(String.valueOf(allSteps));

        int averageSteps = database.getAverageSteps();
        textViewAverageStepsValue.setText(String.valueOf(averageSteps));

        ParseQuery<ParseObject> query = new ParseQuery<>("Activity");
        query.whereEqualTo("username", ParseUser.getCurrentUser().getUsername());
        query.fromLocalDatastore();
        query.orderByDescending("distance");
        query.findInBackground(new FindCallback<ParseObject>() {
            @Override
            public void done(List<ParseObject> objects, ParseException e) {
                if (e == null){
                    if (objects.size() > 0){
                        activitiesCount = objects.size();
                        longestDistance = objects.get(0).getDouble("distance");

                        textViewLongestActivityValue.setText(Common.distanceFormatter(longestDistance));
                        textViewActivitiesCountValue.setText(String.valueOf(activitiesCount));

                        double totalDistance = 0;
                        for (ParseObject object:objects){
                            totalDistance = totalDistance + object.getDouble("distance");
                        }
                        textViewTotalDistanceValue.setText(Common.distanceFormatter(totalDistance));
                    }
                } else {
                    Log.i("Activities query", "Failed with error: " + e.toString());
                }
            }
        });

        query.orderByDescending("duration");
        query.findInBackground(new FindCallback<ParseObject>() {
            @Override
            public void done(List<ParseObject> objects, ParseException e) {
                if (e == null){
                    if (objects.size() > 0){
                        longestTime = objects.get(0).getInt("duration");
                        textViewLongestActivityTimeValue.setText(Common.timeFormatter(longestTime));

                        int totalDuration = 0;
                        for (ParseObject object:objects){
                            totalDuration = totalDuration + object.getInt("duration");
                        }
                        textViewTotalTimeValue.setText(Common.timeFormatter(totalDuration));
                    }
                } else {
                    Log.i("Activities Query", "Failed with error: " + e.toString());
                }
            }
        });

        query.orderByAscending("pace");
        query.findInBackground(new FindCallback<ParseObject>() {
            @Override
            public void done(List<ParseObject> objects, ParseException e) {
                if (e == null){
                    if (objects.size() > 0){
                        int pace = objects.get(0).getInt("pace");
                        textViewPaceRecordValue.setText(Common.timeFormatter(pace));

                        int calories = 0;
                        for (ParseObject object:objects){
                            calories = calories + object.getInt("calories");
                        }
                        textViewCaloriesBurnedValue.setText(calories + " kcal");
                    }
                } else {
                    Log.i("Activities Query", "Failed with error: " + e.toString());
                }
            }
        });

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        finish();
        return true;
    }
}