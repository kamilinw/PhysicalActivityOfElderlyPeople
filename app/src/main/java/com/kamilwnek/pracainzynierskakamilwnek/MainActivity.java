package com.kamilwnek.pracainzynierskakamilwnek;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.kamilwnek.pracainzynierskakamilwnek.Pedometer.Database;
import com.kamilwnek.pracainzynierskakamilwnek.Pedometer.SensorListener;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseUser;
import com.parse.SaveCallback;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements BottomNavigationView.OnNavigationItemSelectedListener {

    BottomNavigationView navigationView;
    Handler handler = null;
    Runnable run = null;
    Date startActivityDate;

    Activity currentActivity;
    Intent serviceIntent = null;

    public void onClickJoinActivity(View view){
        Intent intent = new Intent(getApplicationContext(),JoinExistingActivity.class);
        startActivityForResult(intent,2);
    }

    public void onClickCreateJointActivity(View view){
        Intent intent = new Intent(getApplicationContext(),CreateJointActivityDetails.class);
        startActivityForResult(intent,0);
    }

    public void onClickStartActivity(View view){

        if (ActivityFragment.userLocalization.longitude == 0 && ActivityFragment.userLocalization.latitude == 0){
            Common.infoAlertDialog(this,R.string.gpsNotFound);
            return;
        }

        serviceIntent = new Intent(this,YourService.class);
        startService(serviceIntent);
        ActivityFragment.activityStarted = true;
        navigationView.setVisibility(View.GONE);

        String myFormat = "dd/MM/yyyy kk:mm";
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(myFormat, Locale.getDefault());
        startActivityDate = Calendar.getInstance().getTime();
        currentActivity = new Activity(
                ParseUser.getCurrentUser().getUsername(),
                new ArrayList<LatLng>(),
                0,
                0,
                0,
                0,
                ActivityFragment.activityID,
                simpleDateFormat.format(startActivityDate),
                new PolylineOptions());

        updateLabels(0,0,0,0);

        handler =new Handler();

        run = new Runnable() {
            @Override
            public void run() {
                currentActivity.addSecToClock();
                currentActivity.saveLocation(ActivityFragment.userLocalization);
                currentActivity.countDistance();
                currentActivity.countPace();
                currentActivity.countCalories();

                updateLabels(currentActivity.getDistance(),
                        currentActivity.getPaceSec(),
                        currentActivity.getCalories(),
                        currentActivity.getTimeSec());

                handler.postDelayed(this,1000);
            }
        };
        handler.post(run);
    }

    private void updateLabels(float distance, int pace, int calories, int time) {
        TextView timeTextView = findViewById(R.id.timeTextView);
        TextView distanceTextView = findViewById(R.id.distanceTextView);
        TextView distanceUnitTextView = findViewById(R.id.distanceUnitTextView);
        TextView paceTextView = findViewById(R.id.paceTextView);
        TextView caloriesTextView = findViewById(R.id.caloriesTextView);

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

        // update calories label
        caloriesTextView.setText(String.valueOf(calories));

        // update clock label
        String timeString = String.format(Locale.getDefault(),"%02d", time/3600) + ":" +
                String.format(Locale.getDefault(),"%02d", time/60) + ":" +
                String.format(Locale.getDefault(),"%02d", time%60);
        timeTextView.setText(timeString);
    }

    public void onClickStopActivity(View view){
        if (serviceIntent != null)
            stopService(serviceIntent);

        if (currentActivity == null)
            return;

        if (!ActivityFragment.activityStarted)
            return;

        ActivityFragment.googleMap.clear();
        updateLabels(0,0,0,0);
        ActivityFragment.activityStarted = false;

        ParseObject object = new ParseObject("Activity");
        object.add("username",ParseUser.getCurrentUser().getUsername());

        Gson gsonBuilder = new GsonBuilder().create();
        String coordinatesJsonString = gsonBuilder.toJson(currentActivity.getUserPath());
        ParseObject parseObject = new ParseObject("Activity");

        parseObject.put("username", currentActivity.getUsername());
        parseObject.put("coordinates", coordinatesJsonString);
        parseObject.put("duration", currentActivity.getTimeSec());
        parseObject.put("distance", currentActivity.getDistance());
        parseObject.put("calories", currentActivity.getCalories());
        parseObject.put("activityType", currentActivity.getActivityType());
        parseObject.put("pace", currentActivity.getPaceSec());
        parseObject.put("startActivityDate", startActivityDate);
        parseObject.pinInBackground();
        parseObject.saveEventually(new SaveCallback() {
            @Override
            public void done(ParseException e) {
                if (e == null){
                    Log.i("Saving eventually", "Success");
                } else {
                    Log.i("Saving eventually", "failed with error: " + e.toString());
                }
            }
        });

        if (handler != null) {
            navigationView.setVisibility(View.VISIBLE);
            handler.removeCallbacks(run);
        }

        Intent intent = new Intent(getApplicationContext(), ActivityDetails.class);
        intent.putExtra("coordinatesJsonString",coordinatesJsonString);
        intent.putExtra("username", currentActivity.getUsername());
        intent.putExtra("duration", currentActivity.getTimeSec());
        intent.putExtra("distance", currentActivity.getDistance());
        intent.putExtra("pace", currentActivity.getPaceSec());
        intent.putExtra("calories", currentActivity.getCalories());
        intent.putExtra("activityType", currentActivity.getActivityType());
        intent.putExtra("startDate", currentActivity.getDate());
        startActivity(intent);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {

        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 0 && resultCode == 1){
            loadFragment(new ActivityFragment());
            navigationView.setSelectedItemId(R.id.navigation_activity);
            Log.i("Result", "request code 0");
        } else if (requestCode == 2 && resultCode == 1){
            loadFragment(new ActivityFragment());
            navigationView.setSelectedItemId(R.id.navigation_activity);
            Log.i("Result", "request code 2");
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.main_menu,menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()){
            case R.id.logOut:
                Database db = Database.getInstance(getApplicationContext());
                db.updateOnServer(true, this);
                db.close();
                finish();

                return true;
            default:
                return false;
        }
    }

    private boolean loadFragment(Fragment fragment) {
        if (fragment != null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, fragment)
                    .commit();
            return true;
        }
        return false;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getSupportActionBar().hide();
        getWindow().setStatusBarColor(getResources().getColor(R.color.colorPrimary));
        loadFragment(new ProfileFragment());

        navigationView = findViewById(R.id.navigation);
        navigationView.setOnNavigationItemSelectedListener(this);

        if (Build.VERSION.SDK_INT >= 26) {
            Log.i("StartService", "Build version > 26");
            startForegroundService(new Intent(this, SensorListener.class));
        } else {
            Log.i("StartService", "Build version < 26");
            startService(new Intent(this, SensorListener.class));
        }
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
        Fragment fragment = null;

        switch (menuItem.getItemId()) {
            case R.id.navigation_profile:
                fragment = new ProfileFragment();
                getSupportActionBar().hide();
                break;
            case R.id.navigation_exercises:
                fragment = new JointActivityFragment();
                getSupportActionBar().show();
                getSupportActionBar().setTitle(R.string.titleJointActivityLong);
                break;
            case R.id.navigation_pedometer:
                fragment = new PedometerFragment();
                getSupportActionBar().show();
                getSupportActionBar().setTitle(R.string.title_pedometer);
                break;
            case R.id.navigation_activity:
                fragment = new ActivityFragment();
                getSupportActionBar().show();
                getSupportActionBar().setTitle(R.string.title_activity);
                break;
            case R.id.navigation_progress:
                fragment = new ProgressFragment();
                getSupportActionBar().show();
                getSupportActionBar().setTitle(R.string.title_progress_long);
                break;
        }
        return loadFragment(fragment);
    }
}