package com.kamilwnek.pracainzynierskakamilwnek;

import android.Manifest;
import android.animation.ValueAnimator;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;

import static android.content.Context.LOCATION_SERVICE;

public class ActivityFragment extends Fragment {
    private MapView mMapView;
    private LocationManager locationManager;
    private LocationListener locationListener;
    private boolean isLocationKnown = false;
    private boolean isMapExtended = false;
    static GoogleMap googleMap;
    private ImageView imageViewMapSizeButton;
    static LatLng userLocalization;
    static boolean activityStarted = false;

    EditText editTextActivity;
    View rootView;
    static int activityID = 0;

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1){
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                try {
                    if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED){
                        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 3000, 0, locationListener);
                        locationManager.requestSingleUpdate(LocationManager.NETWORK_PROVIDER, locationListener, null);

                        googleMap.setMyLocationEnabled(true);
                        Location lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                        if (lastKnownLocation != null){
                            isLocationKnown = true;
                            LatLng latLng = new LatLng(lastKnownLocation.getLatitude(),lastKnownLocation.getLongitude());
                            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng,15));
                        }

                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_activity, container, false);

        userLocalization = new LatLng(0,0);
        mMapView = rootView.findViewById(R.id.mapView);
        mMapView.onCreate(savedInstanceState);
        imageViewMapSizeButton = rootView.findViewById(R.id.imageViewMapSizeButton);
        imageViewMapSizeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (isMapExtended){
                    animateHeight(mMapView,-200,300);
                    animateRotation(imageViewMapSizeButton,0,300);
                    isMapExtended = false;
                } else {
                    animateHeight(mMapView,200,300);
                    animateRotation(imageViewMapSizeButton,180,300);
                    isMapExtended = true;
                }
            }
        });

        editTextActivity = rootView.findViewById(R.id.editTextActivity);
        editTextActivity.setText(getResources().getStringArray(R.array.activities)[activityID]);
        editTextActivity.setShowSoftInputOnFocus(false);
        editTextActivity.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean b) {
                if (!b)
                    return;
                showActivityPickerDialog();
            }
        });
        editTextActivity.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showActivityPickerDialog();
            }
        });

        mMapView.onResume(); // needed to get the map to display immediately

        try {
            MapsInitializer.initialize(getActivity().getApplicationContext());
        } catch (Exception e) {
            e.printStackTrace();
        }

        mMapView.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(GoogleMap mMap) {
                googleMap = mMap;
                googleMap.setOnMapLoadedCallback(new GoogleMap.OnMapLoadedCallback() {
                    @Override
                    public void onMapLoaded() {
                        try {
                            if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_FINE_LOCATION)
                                    != PackageManager.PERMISSION_GRANTED){
                                ActivityCompat.requestPermissions(getActivity(),
                                        new String[] {Manifest.permission.ACCESS_FINE_LOCATION},
                                        1);
                            }else {
                                Location lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                                if (lastKnownLocation != null) {
                                    LatLng latLng = new LatLng(lastKnownLocation.getLatitude(),lastKnownLocation.getLongitude());
                                    googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng,15));
                                }
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });

                try {
                    locationManager = (LocationManager) getActivity().getSystemService(LOCATION_SERVICE);
                } catch (Exception e) {
                    e.printStackTrace();
                }

                locationListener = new LocationListener() {
                    @Override
                    public void onLocationChanged(Location location) {
                        userLocalization = new LatLng(location.getLatitude(),location.getLongitude());
                        if (location != null && activityStarted){
                            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLocalization,15));
                            Log.i("Changing location", location.getLatitude() + ", " + location.getLongitude());
                        }

                        if ((location != null) && (!isLocationKnown)){

                            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
                                    new LatLng(location.getLatitude(),location.getLongitude()),
                                    12));
                            if (location.getAccuracy() < 30) {

                                googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLocalization,15));
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

                try {
                    if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_FINE_LOCATION) !=
                            PackageManager.PERMISSION_GRANTED){
                        ActivityCompat.requestPermissions(getActivity(), new String[] {Manifest.permission.ACCESS_FINE_LOCATION},1);
                    } else {
                        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 3000, 0, locationListener);
                        locationManager.requestSingleUpdate(LocationManager.NETWORK_PROVIDER, locationListener, null);
                        googleMap.setMyLocationEnabled(true);

                        Location lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                        if (lastKnownLocation != null){
                            userLocalization = new LatLng(lastKnownLocation.getLatitude(),lastKnownLocation.getLongitude());
                            isLocationKnown = true;
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        return rootView;
    }

    private void showActivityPickerDialog() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(rootView.getContext());
        builder.setTitle(R.string.selectActivity)
                .setSingleChoiceItems(R.array.activities, 0, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        editTextActivity.setText(getResources().getStringArray(R.array.activities)[i]);
                        activityID = i;
                        dialogInterface.dismiss();
                    }
                });

        builder.create();
        AlertDialog mDialog = builder.create();
        mDialog.show();
    }

    private void animateHeight(View view, int dp, int duration) {
        final View v = view;
        ValueAnimator anim = ValueAnimator.ofInt(v.getMeasuredHeight(),
                (int) (v.getMeasuredHeight()+(dp * getActivity().getResources().getDisplayMetrics().density)));
        anim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                int val = (Integer) valueAnimator.getAnimatedValue();
                ViewGroup.LayoutParams layoutParams = v.getLayoutParams();
                layoutParams.height = val;
                v.setLayoutParams(layoutParams);
            }
        });
        anim.setDuration(duration);
        anim.start();
    }
    private void animateRotation(View view, int degree, int duration) {
        final View v = view;
        ValueAnimator anim = ValueAnimator.ofInt((int) v.getRotation(), degree);
        anim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                int val = (Integer) valueAnimator.getAnimatedValue();
                v.setRotation(val);
            }
        });
        anim.setDuration(duration);
        anim.start();
    }
}