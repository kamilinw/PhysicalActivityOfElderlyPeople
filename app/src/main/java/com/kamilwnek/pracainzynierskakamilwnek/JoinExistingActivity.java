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
import android.os.Handler;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.parse.FindCallback;
import com.parse.GetCallback;
import com.parse.ParseException;
import com.parse.ParseGeoPoint;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class JoinExistingActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    LocationManager locationManager;
    LocationListener locationListener;

    boolean isLocationKnown = false;
    boolean isActivityJoined = false;
    LatLng myCoordinates;
    int itemSelected;
    ParseObject activeActivity;

    ProgressBar progressBar;
    TextView textViewInfo;
    CustomAdapter customAdapter;
    List<ParseObject> nearbyPersons;
    ArrayList<Marker> markers;
    Button joinButton;
    ListView companionsListView;

    Runnable runnable;
    Runnable runnableActivity;
    Handler handler = new Handler();
    Handler handlerActivity = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_join_existing);
        getWindow().setStatusBarColor(getResources().getColor(R.color.colorPrimary));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(R.string.joinToActivity);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);


        myCoordinates = new LatLng(0,0);
        markers = new ArrayList<>();
        nearbyPersons = new ArrayList<>();
        joinButton = findViewById(R.id.joinButton);

        progressBar = findViewById(R.id.progressBar);
        textViewInfo = findViewById(R.id.textViewInfo);
        companionsListView = findViewById(R.id.companionsListView);
        textViewInfo.setText(R.string.searchingForGPS);
        textViewInfo.setVisibility(View.VISIBLE);

        ListView companionsListView = findViewById(R.id.companionsListView);
        customAdapter = new CustomAdapter();
        companionsListView.setAdapter(customAdapter);

        companionsListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                LatLng latLng = new LatLng(nearbyPersons.get(i).getParseGeoPoint("location").getLatitude(),
                        nearbyPersons.get(i).getParseGeoPoint("location").getLongitude());
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng,16));
                itemSelected = i;
                markers.get(i).showInfoWindow();
                handler.removeCallbacks(runnable);
                isActivityJoined = true;
                joinButton.setVisibility(View.VISIBLE);
            }
        });

        runnable = new Runnable() {
            @Override
            public void run() {
                getNearby();
                handler.postDelayed(this,30000);
            }
        };

        runnableActivity = new Runnable() {
            @Override
            public void run() {

                ParseQuery<ParseObject> query = ParseQuery.getQuery("JointActivity");
                query.getInBackground(nearbyPersons.get(itemSelected).getObjectId(), new GetCallback<ParseObject>() {
                    @Override
                    public void done(ParseObject object, ParseException e) {
                        if (e != null){
                            Log.i("runnableActivity", "Parse server error: " + e.toString());
                            textViewInfo.setText(R.string.noResoultsFound);
                            return;
                        }
                        textViewInfo.setText(R.string.joinToYourCompsnion);
                        object.put("companionLatitude", myCoordinates.latitude);
                        object.put("companionLongitude", myCoordinates.longitude);
                        object.saveInBackground();

                        activeActivity = object;

                        LatLng latLng = new LatLng(object.getParseGeoPoint("location").getLatitude(),
                                                    object.getParseGeoPoint("location").getLongitude());

                        mMap.clear();
                        markers.clear();
                        markers.add(mMap.addMarker(new MarkerOptions().position(latLng)));
                        markers.add(mMap.addMarker(new MarkerOptions().position(myCoordinates).visible(false)));

                        LatLngBounds.Builder builder = new LatLngBounds.Builder();
                        for (Marker marker : markers) {
                                builder.include(marker.getPosition());
                        }
                        LatLngBounds bounds = builder.build();
                        mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 150));

                        ParseGeoPoint myLocationPGP = new ParseGeoPoint(myCoordinates.latitude,myCoordinates.longitude);
                        ParseGeoPoint companionLocationPGP = object.getParseGeoPoint("location");

                        if (myLocationPGP.distanceInKilometersTo(companionLocationPGP) < 0.05){
                            Log.i("Distance", "less than 50 m");

                            deleteActivity();

                            new AlertDialog.Builder(JoinExistingActivity.this)
                                    .setMessage(R.string.companionIsHere)
                                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialogInterface, int i) {
                                            Intent returnIntent = new Intent();
                                            setResult(1,returnIntent);
                                            finish();
                                        }
                                    })
                                    .show();
                        }
                    }
                });
                handlerActivity.postDelayed(this,5000);
            }
        };
    }

    private void deleteActivity() {
        handlerActivity.removeCallbacks(runnableActivity);

        ParseQuery<ParseObject> query = ParseQuery.getQuery("JointActivity");
        query.getInBackground(nearbyPersons.get(itemSelected).getObjectId(), new GetCallback<ParseObject>() {
            @Override
            public void done(ParseObject object, ParseException e) {
                if (e == null){
                    try {
                        object.delete();
                        Log.i("DeleteActivity", "Successful");
                    } catch (ParseException e1) {
                        e1.printStackTrace();
                    }
                } else {
                    Log.i("DeleteActivity", "Nie udalo sie znalizc zapytania. Error: " + e.toString());
                }
            }
        });

    }

    public void onClickJoinButton(View view){
        handler.removeCallbacks(runnable);
        isActivityJoined = true;

        companionsListView.setVisibility(View.GONE);
        joinButton.setVisibility(View.GONE);
        textViewInfo.setText(R.string.joinToYourCompsnion);
        textViewInfo.setVisibility(View.VISIBLE);

        mMap.setOnMapClickListener(null);

        markers.clear();

        ParseQuery<ParseObject> query = ParseQuery.getQuery("JointActivity");
        query.getInBackground(nearbyPersons.get(itemSelected).getObjectId(), new GetCallback<ParseObject>() {
            @Override
            public void done(ParseObject object, ParseException e) {
                if (e != null){
                    handlerActivity.post(runnableActivity);
                    Log.i("runnableActivity", "Parse server error: " + e.toString());
                    return;
                }
                object.put("companionUsername", ParseUser.getCurrentUser().getUsername());
                object.put("companionLongitude", myCoordinates.longitude);
                object.put("companionLatitude", myCoordinates.latitude);
                object.saveInBackground();

                handlerActivity.post(runnableActivity);
            }
        });
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
                        myCoordinates = new LatLng(lastKnownLocation.getLatitude(),lastKnownLocation.getLongitude());
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(myCoordinates,12));
                    }
                }

            }
        }
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
                myCoordinates = new LatLng(location.getLatitude(),location.getLongitude());

                if ((location != null) && (!isLocationKnown)){

                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(location.getLatitude(),location.getLongitude()),12));
                    if (location.getAccuracy() < 50) {

                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(location.getLatitude(),location.getLongitude()),12));
                        isLocationKnown = true;

                        textViewInfo.setVisibility(View.GONE);
                        if (!isActivityJoined)
                            handler.post(runnable);
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
                myCoordinates = new LatLng(lastKnownLocation.getLatitude(),lastKnownLocation.getLongitude());
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(myCoordinates,12));

                // moveCamera(lastKnownLocation, 15, false, null);
            } else if (lastKnownApproximateLocation != null){
                mMap.setMyLocationEnabled(true);
                myCoordinates = new LatLng(lastKnownApproximateLocation.getLatitude(),lastKnownApproximateLocation.getLongitude());
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(myCoordinates,12));

            }
        }

        mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {
                joinButton.setVisibility(View.GONE);
                handler.post(runnable);
                isActivityJoined = false;
            }
        });

    }

    private void getNearby() {
        ParseGeoPoint myLocation = new ParseGeoPoint(myCoordinates.latitude,myCoordinates.longitude);
        ParseQuery<ParseObject> query = ParseQuery.getQuery("JointActivity");
        query.whereWithinKilometers("location",myLocation,4);
        query.whereDoesNotExist("companionUsername");

        nearbyPersons.clear();
        customAdapter.notifyDataSetChanged();
        textViewInfo.setText(R.string.searchingForNearby);
        textViewInfo.setVisibility(View.VISIBLE);
        progressBar.setVisibility(View.VISIBLE);

        query.findInBackground(new FindCallback<ParseObject>() {
            @Override
            public void done(List<ParseObject> objects, ParseException e) {
                if (e == null && objects != null && objects.size()>0){
                    textViewInfo.setVisibility(View.GONE);
                    for (ParseObject object:objects){
                        Date time = object.getDate("startTime");
                        Long timeLeft = Math.max((time.getTime() - System.currentTimeMillis())/1000,0);
                        if (timeLeft > 1)
                            nearbyPersons.add(object);
                    }
                    progressBar.setVisibility(View.GONE);
                    joinButton.setVisibility(View.GONE);
                    if (nearbyPersons.size() == 0){
                        textViewInfo.setVisibility(View.VISIBLE);
                        textViewInfo.setText(R.string.nobodyNear);
                    }
                    customAdapter.notifyDataSetChanged();
                    updateMap();
                }
            }
        });
    }

    @Override
    public void onBackPressed() {
        if (!isActivityJoined)
            handler.removeCallbacks(runnable);
        else
            handlerActivity.removeCallbacks(runnableActivity);
        super.onBackPressed();
    }

    private void updateMap() {
        mMap.setOnMapLoadedCallback(new GoogleMap.OnMapLoadedCallback() {
            @Override
            public void onMapLoaded() {
                mMap.clear();
                markers.clear();

                for (ParseObject object: nearbyPersons){
                    LatLng position = new LatLng(object.getParseGeoPoint("location").getLatitude(),
                            object.getParseGeoPoint("location").getLongitude());

                    String title = object.getString("username") + ", " +
                            String.format("%.3f",object.getParseGeoPoint("location")
                                    .distanceInKilometersTo(new ParseGeoPoint(myCoordinates.latitude,myCoordinates.longitude)))
                            + " km " + getResources().getString(R.string.away);
                    markers.add(mMap.addMarker(new MarkerOptions().position(position).title(title)));
                }
                markers.add(mMap.addMarker(new MarkerOptions().position(myCoordinates).visible(false)));

                if (markers.size() < 2)
                    return;

                LatLngBounds.Builder builder = new LatLngBounds.Builder();
                for (Marker marker : markers) {
                    builder.include(marker.getPosition());
                }
                LatLngBounds bounds = builder.build();
                mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 150));
            }
        });
    }

    public class CustomAdapter extends BaseAdapter {
        @Override
        public int getCount() {
            return nearbyPersons.size();
        }

        @Override
        public Object getItem(int i) {
            return null;
        }

        @Override
        public long getItemId(int i) {
            return 0;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            view = getLayoutInflater().inflate(R.layout.joint_activities_list_view_layout, null);
            TextView textViewListActivity = view.findViewById(R.id.textViewListActivity);
            TextView textViewListInfo = view.findViewById(R.id.textViewListInfo);
            TextView textViewTimeLeft = view.findViewById(R.id.textViewTimeLeft);

            ParseGeoPoint myLocation = new ParseGeoPoint(myCoordinates.latitude,myCoordinates.longitude);

            int activityID = nearbyPersons.get(i).getInt("activityID");
            textViewListActivity.setText(getResources().getStringArray(R.array.activities)[activityID]);

            String info = nearbyPersons.get(i).getString("username") + ", "
                    + String.format("%.3f",nearbyPersons.get(i).getParseGeoPoint("location").distanceInKilometersTo(myLocation))
                    + " km " + getResources().getString(R.string.away);
            textViewListInfo.setText(info);

            Date time = nearbyPersons.get(i).getDate("startTime");
            Long timeLeft = Math.max((time.getTime() - System.currentTimeMillis())/1000,0);

            textViewTimeLeft.setText(timeLeft/60 + " min");
            return view;
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        onBackPressed();
        return true;
    }
}