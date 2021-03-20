package com.kamilwnek.pracainzynierskakamilwnek;

import android.graphics.Color;
import android.location.Location;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.PolylineOptions;
import com.parse.ParseUser;

import java.util.List;

public class Activity {

    private String username;
    private List<LatLng> userPath;
    private float distance;
    private int paceSec;
    private int timeSec;
    private int calories;
    private int activityType;
    private String date;
    private PolylineOptions polylineOptions;
    private boolean isLocationChanged = false;

    public Activity(String username, List<LatLng> userPath, float distance, int paceSec, int timeSec, int calories, int activityType, String date, PolylineOptions polylineOptions) {
        this.username = username;
        this.userPath = userPath;
        this.distance = distance;
        this.paceSec = paceSec;
        this.timeSec = timeSec;
        this.calories = calories;
        this.activityType = activityType;
        this.polylineOptions = polylineOptions;
        this.date = date;
        this.polylineOptions.color( Color.parseColor( "#CC0000FF" ) );
        this.polylineOptions.width( 10 );
        this.polylineOptions.visible( true );
    }


    public String getUsername() {
        return username;
    }

    public List<LatLng> getUserPath() {
        return userPath;
    }

    public float getDistance() {
        return distance;
    }

    public int getPaceSec() {
        return paceSec;
    }

    public int getTimeSec() {
        return timeSec;
    }

    public int getCalories() {
        return calories;
    }

    public int getActivityType(){
        return activityType;
    }

    public String getDate() {
        return date;
    }

    public PolylineOptions getPolylineOptions() {
        return polylineOptions;
    }

    public void saveLocation(LatLng userLocalization) {

        int userPathSize = userPath.size();
        if (userPathSize > 0){
            // if new location is different from previous
            if (userPath.get(userPathSize-1).latitude != userLocalization.latitude ||
                    userPath.get(userPathSize-1).longitude != userLocalization.longitude){
                userPath.add(userLocalization);

                polylineOptions.add( new LatLng( userPath.get(userPathSize-1).latitude,
                        userPath.get(userPathSize-1).longitude) );
                ActivityFragment.googleMap.addPolyline( polylineOptions );
                isLocationChanged = true;
            } else {
                isLocationChanged = false;
            }
        } else {
            userPath.add(userLocalization);
        }
    }


    public void countDistance() {
        int userPathSize = userPath.size();
        if (userPathSize > 1 && isLocationChanged){

            float[] currentDistance = new float[1];
            Location.distanceBetween(userPath.get(userPathSize-2).latitude,userPath.get(userPathSize-2).longitude,
                    userPath.get(userPathSize-1).latitude,userPath.get(userPathSize-1).longitude, currentDistance);
            distance = distance + currentDistance[0];

        }
    }

    public void countPace() {
        if (distance > 5){
            paceSec = (int)((timeSec)/(distance/1000));
        }
    }

    public void countCalories() {
        if (distance < 5)
            return;

        int weight = Integer.parseInt(ParseUser.getCurrentUser().getString("weight"));
        double caloriesFloat;
        switch (activityType){
            case 0:
                caloriesFloat = weight*0.53*distance/1000;
                calories = (int)caloriesFloat;
                break;
            case 1:
                caloriesFloat = weight*0.75*distance/1000;
                calories = (int)caloriesFloat;
                break;
            case 2:
                if (weight < 65)
                    calories = 500 * timeSec / 3600;
                else if (weight < 75)
                    calories = 550 * timeSec / 3600;
                else
                    calories = 600 * timeSec / 3600;
                break;
                default:
                    caloriesFloat = weight*distance/1000;
                    calories = (int)caloriesFloat;         }

    }

    public void addSecToClock() {
        timeSec ++;
    }

    public void drawPath() {
        int userPathSize = userPath.size();
        if (userPathSize > 0){
            for (LatLng latLng : userPath){
                polylineOptions.add(latLng);
            }
        }
    }
}