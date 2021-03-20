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
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.kamilwnek.pracainzynierskakamilwnek.Pedometer.Database;
import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static com.parse.Parse.getApplicationContext;

public class ProfileFragment extends Fragment {
    View rootView;
    TextView textViewSignUpDate;
    ArrayList<String> menuList;

    @Nullable
    @Override
    public View onCreateView(@NonNull final LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_profile, container, false);

        TextView profileNameTextView = rootView.findViewById(R.id.profileNameTextView);
        textViewSignUpDate = rootView.findViewById(R.id.textViewSignUpDate);
        ImageView imageViewAvatar = rootView.findViewById(R.id.imageViewAvatar);

        // updating username and age on UI
        profileNameTextView.setText(ParseUser.getCurrentUser().getUsername());

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            String birthday = ParseUser.getCurrentUser().getString("birthday");
            String[] parts = birthday.split("/");
            long years = 0;
            LocalDate start = LocalDate.of(Integer.parseInt(parts[2]),
                                            Integer.parseInt(parts[1]),
                                            Integer.parseInt(parts[0]));
            LocalDate end = LocalDate.now(ZoneId.systemDefault());
            years = ChronoUnit.YEARS.between(start,end);
            profileNameTextView.setText(ParseUser.getCurrentUser().getUsername()+ ", " + years + " " + getResources().getString(R.string.years));
        } else {
            Log.i("Age counting", "API is less than 26");
        }

        // updating sign up date
        Date signUpDate = ParseUser.getCurrentUser().getCreatedAt();
        SimpleDateFormat format = new SimpleDateFormat("dd-MM-yyyy");
        String dateString = format.format(signUpDate);
        textViewSignUpDate.setText(getResources().getString(R.string.registered) + " " + dateString);

        // image update
        String sex = ParseUser.getCurrentUser().getString("sex");
        if (sex.matches("male")){
            imageViewAvatar.setImageResource(R.drawable.awatar_male);
        }

        ListView profileListView = rootView.findViewById(R.id.profileListView);
        String[] strings = getResources().getStringArray(R.array.menu);

        menuList = new ArrayList<>(Arrays.asList(strings));

        ArrayAdapter arrayAdapter = new ArrayAdapter(rootView.getContext(),android.R.layout.simple_list_item_1, menuList);
        profileListView.setAdapter(arrayAdapter);

        profileListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                switch (i){
                    case 0:
                        // edit profile
                        Intent intent = new Intent(rootView.getContext(), EditProfile.class);
                        startActivity(intent);
                        break;
                    case 1:
                        // stats
                        intent = new Intent(rootView.getContext(), Statistics.class);
                        startActivity(intent);
                        break;
                    case 2:
                        // app info
                        intent = new Intent(rootView.getContext(), ApplicationInfo.class);
                        startActivity(intent);
                        break;
                    case 3:
                        logOut();
                        break;
                    case 4:
                        deleteProfile();
                        break;

                        default:
                            Toast.makeText(rootView.getContext(),"Error",Toast.LENGTH_SHORT).show();
                }
            }
        });
        return rootView;
    }

    private void logOut(){
        new AlertDialog.Builder(rootView.getContext())
                .setTitle(R.string.logOutTitle)
                .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        if (Common.checkInternetConnection(getApplicationContext())){
                            Database db = Database.getInstance(getApplicationContext());
                            db.updateOnServer(true, getApplicationContext());
                            db.close();
                        }
                    }
                })
                .setNegativeButton(R.string.camcel,null)
                .show();
    }

    private void deleteProfile() {
        new AlertDialog.Builder(rootView.getContext())
                .setTitle(R.string.deleteProfileTitle)
                .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        if (Common.checkInternetConnection(getApplicationContext())){
                            deleteDataFromServer();
                            Database db2 = Database.getInstance(getApplicationContext());
                            db2.deleteData();
                            getActivity().finish();
                            Intent intent = new Intent(getApplicationContext(), WelcomeActivity.class);
                            startActivity(intent);
                        }
                    }
                })
                .setNegativeButton(R.string.camcel,null)
                .show();
    }

    private void deleteDataFromServer() {
        ParseQuery<ParseObject> query = new ParseQuery<>("Pedometer");
        query.whereEqualTo("username", ParseUser.getCurrentUser().getUsername());
        query.findInBackground(new FindCallback<ParseObject>() {
            @Override
            public void done(List<ParseObject> objects, ParseException e) {
                Log.i("deletePedometer","Processing");
                if (e == null){
                    if (objects.size()>0){
                        for (ParseObject object:objects){
                            object.deleteInBackground();
                        }
                    }
                    deleteActivities();
                } else {
                    Log.i("deletePedometer", "Find in background failed with error: " + e.toString());
                    deleteActivities();
                }
            }
        });
    }

    private void deleteActivities() {
        ParseQuery<ParseObject> query = new ParseQuery<>("Activity");
        query.whereEqualTo("username", ParseUser.getCurrentUser().getUsername());
        query.findInBackground(new FindCallback<ParseObject>() {
            @Override
            public void done(List<ParseObject> objects, ParseException e) {
                Log.i("deleteActivities","Processing");
                if (e == null){
                    if (objects.size()>0){
                        for (ParseObject object:objects){
                            object.deleteInBackground();
                        }
                    }
                    ParseUser.getCurrentUser().deleteInBackground();
                    ParseUser.logOutInBackground();
                } else {
                    ParseUser.getCurrentUser().deleteInBackground();
                    ParseUser.logOutInBackground();
                    Log.i("deleteActivities", "find in background failed with error: " + e.toString());
                }
            }
        });
    }
}