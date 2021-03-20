package com.kamilwnek.pracainzynierskakamilwnek;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseGeoPoint;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.parse.SaveCallback;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class CreateJointActivityMap extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    LocationManager locationManager;
    LocationListener locationListener;

    boolean isActivityCreated;
    boolean isLocationKnown = false;
    LatLng coordinates;
    Calendar calendar;
    int activityID;
    int date;

    LinearLayout linearLayoutDetails;
    Button buttonCreateActivity;
    TextView textViewTime;
    TextView textViewActivity;
    TextView textViewInfo;

    CountDownTimer countDownTimer;

    public void onClickCreateActivity(View view){
        if (isActivityCreated){
            deleteActivity();
        } else {
            // activity not created
            if (coordinates.longitude != 0 && coordinates.latitude != 0){
                ParseObject parseObject = new ParseObject("JointActivity");
                ParseGeoPoint parseGeoPoint = new ParseGeoPoint();
                parseGeoPoint.setLatitude(coordinates.latitude);
                parseGeoPoint.setLongitude(coordinates.longitude);
                parseObject.put("location", parseGeoPoint);
                parseObject.put("username", ParseUser.getCurrentUser().getUsername());

                calendar.setTimeInMillis(System.currentTimeMillis());
                calendar.add(Calendar.MINUTE, date);

                parseObject.put("startTime", calendar.getTime());
                parseObject.put("activityID", activityID);
                parseObject.saveInBackground(new SaveCallback() {
                    @Override
                    public void done(ParseException e) {
                        if (e == null){
                            Log.i("Save","Successful!");
                            startTimer();
                            isActivityCreated = true;
                            linearLayoutDetails.setVisibility(View.VISIBLE);
                            buttonCreateActivity.setText(R.string.deleteActivity);
                            textViewActivity.setText(getResources().getStringArray(R.array.activities)[activityID]);
                            textViewInfo.setText(R.string.activityInfo);
                        }else {
                            Log.i("Save","Failed! " + e.toString());
                        }
                    }
                });

            } else {
                Toast.makeText(this, R.string.gpsNotFound, Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void startTimer() {
        long time = Math.max(calendar.getTimeInMillis() - System.currentTimeMillis(),1000);
        countDownTimer = new CountDownTimer(time, 1000) {
            @Override
            public void onTick(long l) {
                updateActivityDetails(l);
            }

            @Override
            public void onFinish() {
                ParseQuery<ParseObject> query = ParseQuery.getQuery("JointActivity");
                query.whereEqualTo("username", ParseUser.getCurrentUser().getUsername());
                query.findInBackground(new FindCallback<ParseObject>() {
                    @Override
                    public void done(List<ParseObject> objects, ParseException e) {
                        if (e == null){
                            for (ParseObject object: objects){
                                try {
                                    object.delete();
                                } catch (ParseException e1) {
                                    e1.printStackTrace();
                                }
                            }
                            mMap.clear();
                            buttonCreateActivity.setText(R.string.createActivity);
                            isActivityCreated = false;
                            linearLayoutDetails.setVisibility(View.GONE);
                            Log.i("delete activity", "Activity deleted successfully!");
                        } else {
                            Log.i("delete activity", "Nie udalo sie znalizc zapytania. Error: " + e.toString());
                        }
                    }
                });

                new AlertDialog.Builder(CreateJointActivityMap.this)
                        .setMessage(R.string.noCompanionFound)
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                finish();
                            }
                        })
                        .show();
            }
        };
        countDownTimer.start();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 3000, 0, locationListener);
                    locationManager.requestSingleUpdate(LocationManager.NETWORK_PROVIDER, locationListener, null);

                    mMap.setMyLocationEnabled(true);
                    Location lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                    if (lastKnownLocation != null){
                        isLocationKnown = true;
                        coordinates = new LatLng(lastKnownLocation.getLatitude(),lastKnownLocation.getLongitude());
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(coordinates,12));
                    }
                }
            }
        }
    }

    @Override
    public void onBackPressed() {
        if (isActivityCreated)
            deleteActivity();
        super.onBackPressed();
    }

    private void deleteActivity() {
        countDownTimer.cancel();

        ParseQuery<ParseObject> query = ParseQuery.getQuery("JointActivity");
        query.whereEqualTo("username", ParseUser.getCurrentUser().getUsername());
        query.findInBackground(new FindCallback<ParseObject>() {
            @Override
            public void done(List<ParseObject> objects, ParseException e) {
                if (e == null){
                    for (ParseObject object: objects){
                        try {
                            object.delete();
                        } catch (ParseException e1) {
                            e1.printStackTrace();
                        }
                    }
                    mMap.clear();
                    buttonCreateActivity.setText(R.string.createActivity);
                    isActivityCreated = false;
                    linearLayoutDetails.setVisibility(View.GONE);
                    Log.i("delete activity", "Activity deleted successfully!");
                } else {
                    Log.i("delete activity", "Nie udalo sie znalizc zapytania. Error: " + e.toString());
                }
            }
        });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_create_joint);
        getWindow().setStatusBarColor(getResources().getColor(R.color.colorPrimary));

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        getSupportActionBar().hide();

        Intent intent = getIntent();
        activityID = intent.getIntExtra("activityID",0);
        date = intent.getIntExtra("date", 5);

        calendar = Calendar.getInstance();

        coordinates = new LatLng(0,0);
        linearLayoutDetails = findViewById(R.id.linearLayoutDetails);
        buttonCreateActivity = findViewById(R.id.buttonCreateActivity);
        linearLayoutDetails.setVisibility(View.GONE);
        textViewInfo = findViewById(R.id.textViewInfo);
        textViewActivity = findViewById(R.id.textViewActivity);
        textViewTime = findViewById(R.id.textViewTime);
    }

    private void updateActivityDetails(long milis) {
        int sec = (int)milis/1000;
        textViewTime.setText(Common.timeFormatter(sec));

        ParseQuery<ParseObject> query = ParseQuery.getQuery("JointActivity");
        query.whereEqualTo("username", ParseUser.getCurrentUser().getUsername());
        query.findInBackground(new FindCallback<ParseObject>() {
            @Override
            public void done(List<ParseObject> objects, ParseException e) {
                if (e == null){
                    if (objects.size()>0
                            && objects.get(0).getString("companionUsername") != null
                            && objects.get(0).getDouble("companionLongitude") != 0
                            && objects.get(0).getDouble("companionLatitude") != 0){

                        Double latitude = objects.get(0).getDouble("companionLatitude");
                        Double longitude = objects.get(0).getDouble("companionLongitude");

                        LatLng companionLocation = new LatLng(latitude,longitude);
                        String companionUsername = objects.get(0).getString("companionUsername");
                        moveCamera(companionLocation, coordinates);

                        textViewInfo.setText(companionUsername);

                        ParseGeoPoint myLocationPGP = new ParseGeoPoint(coordinates.latitude,coordinates.longitude);
                        ParseGeoPoint companionLocationPGP = new ParseGeoPoint(latitude,longitude);

                        objects.get(0).put("location", myLocationPGP);
                        objects.get(0).saveInBackground();

                        if (myLocationPGP.distanceInKilometersTo(companionLocationPGP) < 0.05) {
                            Log.i("Distance", "less than 50 m");
                            deleteActivity();
                            new AlertDialog.Builder(CreateJointActivityMap.this)
                                    .setMessage(R.string.companionIsHere)
                                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialogInterface, int i) {

                                            Intent returnIntent = new Intent();
                                            setResult(1, returnIntent);
                                            finish();
                                        }
                                    })
                                    .show();
                        }
                    }
                } else {
                    Log.i("Error",e.toString());
                }
            }
        });
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    return;
                }
                mMap.setMyLocationEnabled(true);
                coordinates = new LatLng(location.getLatitude(),location.getLongitude());

                if ((location != null) && (!isLocationKnown)){

                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(location.getLatitude(),location.getLongitude()),12));
                    if (location.getAccuracy() < 30) {

                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(location.getLatitude(),location.getLongitude()),15));
                        isLocationKnown = true;
                    }
                } else if (location == null){
                    // lost location
                    isLocationKnown = false;
                }
            }

            @Override
            public void onStatusChanged(String s, int i, Bundle bundle) {

            }

            @Override
            public void onProviderEnabled(String s) {

            }

            @Override
            public void onProviderDisabled(String s) {

            }
        };

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);

        } else {

            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 0, locationListener);
            Location lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            Location lastKnownApproximateLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);

            if (lastKnownLocation != null) {
                mMap.setMyLocationEnabled(true);
                coordinates = new LatLng(lastKnownLocation.getLatitude(),lastKnownLocation.getLongitude());
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(coordinates,12));
            } else if (lastKnownApproximateLocation != null){
                mMap.setMyLocationEnabled(true);
                coordinates = new LatLng(lastKnownApproximateLocation.getLatitude(),lastKnownApproximateLocation.getLongitude());
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(coordinates,12));
            }
        }
    }

    void moveCamera(final LatLng driverPosition, final LatLng riderPosition){

        if (driverPosition.latitude != 0 && driverPosition.longitude != 0 && riderPosition.latitude != 0 && riderPosition.longitude != 0) {
            if (true) {

                mMap.setOnMapLoadedCallback(new GoogleMap.OnMapLoadedCallback() {
                    @Override
                    public void onMapLoaded() {
                        mMap.clear();
                        ArrayList<Marker> markers = new ArrayList<>();

                        markers.add(mMap.addMarker(new MarkerOptions().position(driverPosition).title("Driver position").icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE))));
                        markers.add(mMap.addMarker(new MarkerOptions().position(riderPosition).title("Rider position")));

                        LatLngBounds.Builder builder = new LatLngBounds.Builder();
                        for (Marker marker : markers) {
                            builder.include(marker.getPosition());
                        }
                        LatLngBounds bounds = builder.build();
                        mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 150));
                    }
                });

            }else {
                mMap.clear();
                mMap.addMarker(new MarkerOptions().position(driverPosition).title("Driver position").icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)));
                mMap.addMarker(new MarkerOptions().position(riderPosition).title("Rider position"));
            }
        }
    }
}