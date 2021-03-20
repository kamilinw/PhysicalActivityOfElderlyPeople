package com.kamilwnek.pracainzynierskakamilwnek;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.parse.DeleteCallback;
import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import static com.parse.Parse.getApplicationContext;

public class ProgressFragment extends Fragment {

    List<Activity> activities;
    CustomAdapter customAdapter;
    String date;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_progress, container, false);

        activities = new ArrayList<>();

        ListView listView = rootView.findViewById(R.id.activitiesListView);
        customAdapter = new CustomAdapter();
        listView.setAdapter(customAdapter);

        loadActivities(true);
        loadActivities(false);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Gson gsonBuilder = new GsonBuilder().create();
                String coordinatesJsonString = gsonBuilder.toJson(activities.get(i).getUserPath());

                Intent intent = new Intent(getApplicationContext(), ActivityDetails.class);
                intent.putExtra("coordinatesJsonString",coordinatesJsonString);
                intent.putExtra("username", activities.get(i).getUsername());
                intent.putExtra("duration", activities.get(i).getTimeSec());
                intent.putExtra("distance", activities.get(i).getDistance());
                intent.putExtra("pace", activities.get(i).getPaceSec());
                intent.putExtra("calories", activities.get(i).getCalories());
                intent.putExtra("activityType", activities.get(i).getActivityType());
                intent.putExtra("startDate", activities.get(i).getDate());
                startActivity(intent);
            }
        });

        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> adapterView, View view, int i, long l) {
                final int activityNumber = i;
                new AlertDialog.Builder(getContext())
                        .setIcon(android.R.drawable.ic_delete)
                        .setTitle(R.string.deleteActivityTitle)
                        .setMessage(R.string.deleteActivityMessage)
                        .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                ParseQuery<ParseObject> query = new ParseQuery<>("Activity");
                                query.whereEqualTo("username", ParseUser.getCurrentUser().getUsername());
                                query.orderByDescending("createdAt");
                                query.findInBackground(new FindCallback<ParseObject>() {
                                    @Override
                                    public void done(List<ParseObject> objects, ParseException e) {
                                        if (e == null){
                                            if (objects.size()>0){
                                                objects.get(activityNumber).deleteEventually(new DeleteCallback() {
                                                    @Override
                                                    public void done(ParseException e) {
                                                        Log.i("Removing from parse", "success, object: " + activityNumber);
                                                    }
                                                });
                                            }
                                        } else {
                                            Log.i("Find in background", "failed with error: " + e.toString());
                                        }
                                    }
                                });
                                activities.remove(activityNumber);
                                customAdapter.notifyDataSetChanged();
                            }
                        })
                        .setNegativeButton(R.string.no,null)
                        .show();
                return true;
            }
        });
        return rootView;
    }

    private void loadActivities(final boolean isLocalDataStorage) {
        ParseQuery<ParseObject> query = new ParseQuery<>("Activity");
        query.whereEqualTo("username", ParseUser.getCurrentUser().getUsername());
        if (isLocalDataStorage)
            query.fromLocalDatastore();
        query.orderByDescending("createdAt");
        query.findInBackground(new FindCallback<ParseObject>() {
            @Override
            public void done(List<ParseObject> objects, ParseException e) {
                if (e == null){
                    if (objects.size()>0){
                        if (!isLocalDataStorage){
                            try {
                                ParseObject.unpinAllInBackground();
                                ParseObject.pinAll(objects);
                                activities.clear();
                            } catch (ParseException e1) {
                                e1.printStackTrace();
                            }
                        }

                        for (ParseObject object : objects){

                            String coordinates = object.getString("coordinates");
                            List<LatLng> userPath = new ArrayList<>();
                            try {
                                JSONArray jsonArray = new JSONArray(coordinates);

                                for (int i = 0; i < jsonArray.length(); i++) {
                                    JSONObject jsonObject = jsonArray.getJSONObject(i);
                                    userPath.add(new LatLng(Float.parseFloat(jsonObject.getString("latitude")),Float.parseFloat(jsonObject.getString("longitude"))));
                                }
                            } catch (JSONException e1) {
                                e1.printStackTrace();
                            }
                            String myFormat = "dd/MM/yyyy kk:mm";
                            SimpleDateFormat simpleDateFormat = new SimpleDateFormat(myFormat, Locale.getDefault());
                            Date startActivityDate = object.getDate("startActivityDate");

                            date = simpleDateFormat.format(startActivityDate);

                            activities.add(new Activity(
                                    object.getString("username"),
                                    userPath,
                                    (float) object.getDouble("distance"),
                                    object.getInt("pace"),
                                    object.getInt("duration"),
                                    object.getInt("calories"),
                                    object.getInt("activityType"),
                                    date,
                                    new PolylineOptions()));
                        }
                        customAdapter.notifyDataSetChanged();
                    }
                }
            }
        });
    }

    public class CustomAdapter extends BaseAdapter {
        @Override
        public int getCount() {
            return activities.size();
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
            view = getLayoutInflater().inflate(R.layout.activities_list_view_layout, null);
            TextView textViewListDistance = view.findViewById(R.id.textViewListActivity);
            TextView textViewListDuration = view.findViewById(R.id.textViewListInfo);
            TextView textViewListDate = view.findViewById(R.id.textViewListDate);

            if (activities.get(i).getDistance()<1000){
                textViewListDistance.setText(String.format(Locale.getDefault(),"%.1f",activities.get(i).getDistance()) + " m");
            } else {
                textViewListDistance.setText(String.format(Locale.getDefault(),"%.2f",activities.get(i).getDistance()/1000) + " km");
            }

            String timeString = String.format(Locale.getDefault(),"%02d", activities.get(i).getTimeSec()/3600) + ":" +
                    String.format(Locale.getDefault(),"%02d", activities.get(i).getTimeSec()/60) + ":" +
                    String.format(Locale.getDefault(),"%02d", activities.get(i).getTimeSec()%60);
            textViewListDuration.setText(timeString);

            textViewListDate.setText(activities.get(i).getDate());
            return view;
        }
    }
}