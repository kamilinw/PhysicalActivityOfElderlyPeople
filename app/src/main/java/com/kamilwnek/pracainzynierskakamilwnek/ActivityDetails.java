package com.kamilwnek.pracainzynierskakamilwnek;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.ViewTreeObserver;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.PolylineOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ActivityDetails extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private Activity activity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_details);
        getWindow().setStatusBarColor(getResources().getColor(R.color.colorPrimary));

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        List<LatLng> userPath = new ArrayList<>();

        Intent intent = getIntent();
        String coordinates = intent.getStringExtra("coordinatesJsonString");
        Log.i("Activity Details", coordinates);
        try {
            JSONArray jsonArray = new JSONArray(coordinates);

            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject jsonObject = jsonArray.getJSONObject(i);
                userPath.add(new LatLng(Float.parseFloat(jsonObject.getString("latitude")),
                        Float.parseFloat(jsonObject.getString("longitude"))));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        String username = intent.getStringExtra("username");
        int duration = intent.getIntExtra("duration",0);
        float distance = intent.getFloatExtra("distance",0);
        int paceSec = intent.getIntExtra("pace",0);
        int calories = intent.getIntExtra("calories",0);
        int activityType = intent.getIntExtra("activityType", 0);
        String startDate = intent.getStringExtra("startDate");

        setTitle(startDate);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        activity = new Activity(username, userPath, distance, paceSec, duration, calories, activityType, startDate, new PolylineOptions());

        updateLabels(activity.getDistance(),
                activity.getPaceSec(),
                activity.getCalories(),
                activity.getActivityType(),
                activity.getTimeSec(),
                activity.getDate());
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        Log.i("Activity Details","map is ready");

        activity.drawPath();
        mMap.addPolyline(activity.getPolylineOptions());

        ConstraintLayout constraintLayout = findViewById(R.id.constraintLayout);

        constraintLayout.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                LatLngBounds.Builder builder = new LatLngBounds.Builder();
                for (LatLng latLng : activity.getUserPath()) {
                    builder.include(latLng);
                }
                LatLngBounds bounds = builder.build();
                mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100));
            }
        });
    }

    private void updateLabels(float distance, int pace, int calories, int activity, int time, String date) {
        TextView timeTextView = findViewById(R.id.timeTextView);
        TextView distanceTextView = findViewById(R.id.distanceTextView);
        TextView distanceUnitTextView = findViewById(R.id.distanceUnitTextView);
        TextView paceTextView = findViewById(R.id.paceTextView);
        TextView caloriesTextView = findViewById(R.id.caloriesTextView);
        TextView speedTextView = findViewById(R.id.speedTextView);
        TextView dateTextView = findViewById(R.id.dateTextView);
        TextView activityTextView = findViewById(R.id.activityTextView);

        // update activity label
        activityTextView.setText(getResources().getStringArray(R.array.activities)[activity]);

        // update distance label
        if (distance<1000){
            distanceTextView.setText(String.format(Locale.getDefault(),"%.1f",distance));
            distanceUnitTextView.setText(R.string.distance_m);
        } else {
            distanceTextView.setText(String.format(Locale.getDefault(),"%.2f",distance/1000));
            distanceUnitTextView.setText(R.string.distance_km);
        }

        // update pace label
        String paceString = String.format(Locale.getDefault(),"%02d", pace/60) + ":" +
                String.format(Locale.getDefault(),"%02d", pace%60);
        paceTextView.setText(paceString);

        // update speed label
        speedTextView.setText(String.format(Locale.getDefault(), "%.1f", 3.6*distance/time));

        // update calories label
        caloriesTextView.setText(String.valueOf(calories));

        // update date label
        dateTextView.setText(date);

        // update clock label
        String timeString = String.format(Locale.getDefault(),"%02d", time/3600) + ":" +
                String.format(Locale.getDefault(),"%02d", time/60) + ":" +
                String.format(Locale.getDefault(),"%02d", time%60);
        timeTextView.setText(timeString);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        finish();
        return true;
    }
}