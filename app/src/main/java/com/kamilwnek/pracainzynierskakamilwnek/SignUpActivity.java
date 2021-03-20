package com.kamilwnek.pracainzynierskakamilwnek;

import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.parse.SaveCallback;
import com.parse.SignUpCallback;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class SignUpActivity extends AppCompatActivity {
    Calendar myCalendar;
    EditText editTextBirthday;
    DatePickerDialog.OnDateSetListener date;
    EditText editTextEmail;
    EditText editTextUsername;
    EditText editTextPassword;
    EditText editTextHeight;
    EditText editTextWeight;
    EditText editTextPasswordCheck;
    RadioButton radioButtonWoman;
    RadioButton radioButtonMan;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        getSupportActionBar().hide();
        getWindow().setStatusBarColor(getResources().getColor(R.color.colorPrimary));

        editTextEmail = findViewById(R.id.editTextEmail);
        editTextUsername = findViewById(R.id.editTextUsername);
        editTextPassword = findViewById(R.id.editTextPassword);
        editTextPasswordCheck = findViewById(R.id.editTextPasswordCheck);
        editTextHeight = findViewById(R.id.editTextHeight);
        editTextWeight = findViewById(R.id.editTextWeight);
        radioButtonMan = findViewById(R.id.radioButtonMan);
        radioButtonWoman = findViewById(R.id.radioButtonWoman);

        myCalendar = Calendar.getInstance();
        editTextBirthday = findViewById(R.id.editTextBirthday);

        date = new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker datePicker, int year, int month, int day) {
                myCalendar.set(Calendar.YEAR, year);
                myCalendar.set(Calendar.MONTH, month);
                myCalendar.set(Calendar.DAY_OF_MONTH, day);
                updateLabel();
            }
        };

        editTextBirthday.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                DatePickerDialog dialog = new DatePickerDialog(
                        SignUpActivity.this,
                        android.R.style.Theme_Holo_Light_Dialog_MinWidth,
                        date,
                        myCalendar.get(Calendar.YEAR),
                        myCalendar.get(Calendar.MONTH),
                        myCalendar.get(Calendar.DAY_OF_MONTH));

                dialog.getWindow().setBackgroundDrawable(new ColorDrawable((Color.TRANSPARENT)));
                dialog.setTitle(R.string.selectBirthday);
                dialog.show();
            }
        });
    }

    private void updateLabel(){
        String myFormat = "dd/MM/yyyy";
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(myFormat, Locale.getDefault());
        editTextBirthday.setText(simpleDateFormat.format(myCalendar.getTime()));
    }

    public void onClickSignUp(View view) {
        TextView textViewSignUp = findViewById(R.id.textViewSignUp);
        Animation alpha = AnimationUtils.loadAnimation(this,R.anim.alpha);
        view.startAnimation(alpha);
        textViewSignUp.setAnimation(alpha);
        if (!(isDataCorrect() && Common.checkInternetConnection(SignUpActivity.this)))
            return;

        ParseUser user = new ParseUser();
        user.setUsername(editTextUsername.getText().toString());
        user.setEmail(editTextEmail.getText().toString());
        user.setPassword(editTextPassword.getText().toString());
        if (radioButtonMan.isChecked())
            user.put("sex","male");
        else if (radioButtonWoman.isChecked())
            user.put("sex", "female");
        user.put("birthday",editTextBirthday.getText().toString());
        user.put("height",editTextHeight.getText().toString());
        user.put("weight",editTextWeight.getText().toString());
        user.saveInBackground();

        user.signUpInBackground(new SignUpCallback() {
            @Override
            public void done(ParseException e) {
                if (e==null){
                    Log.w("Sign up", "Successful!");

                    initializePedometer();
                    makeDialog();
                }else {
                    Log.w("Sign up", "Failed with error: " + e.toString());
                    Toast.makeText(SignUpActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void makeDialog() {
        new AlertDialog.Builder(SignUpActivity.this)
                .setIcon(android.R.drawable.ic_dialog_email)
                .setTitle(R.string.emailVerificationTitle)
                .setMessage(R.string.emailVerificationMessage)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        ParseUser.getCurrentUser().logOutInBackground();
                        Intent intent = new Intent(getApplicationContext(),LogInActivity.class);
                        startActivity(intent);
                        finish();
                    }
                })
                .show();
    }

    private void initializePedometer() {
        ParseQuery<ParseObject> query = ParseQuery.getQuery("Pedometer");
        query.whereEqualTo("username", ParseUser.getCurrentUser().getUsername());
        query.findInBackground(new FindCallback<ParseObject>() {
            @Override
            public void done(List<ParseObject> objects, ParseException e) {
                if (e == null){
                    if (objects.size() == 0){
                        Log.i("Database", "Adding new object to server");
                        final List<Long> date = new ArrayList<>();
                        final List<Integer> steps = new ArrayList<>();
                        date.add(Common.getToday());
                        steps.add(0);
                        ParseObject parseObject = new ParseObject("Pedometer");
                        parseObject.put("username", ParseUser.getCurrentUser().getUsername());
                        parseObject.addAllUnique("steps", Arrays.asList(steps));
                        parseObject.addAllUnique("date", Arrays.asList(date));
                        parseObject.saveEventually(new SaveCallback() {
                            @Override
                            public void done(ParseException e) {
                                if (e == null){
                                    Log.i("Database", "Saving database on server completed successful");
                                } else {
                                    Log.i("Database", "Saving database on server failed witch error: " + e.toString());
                                }
                            }
                        });
                    }
                } else {
                    Log.i("Database", "update on server query failed with error: " + e.toString());
                }
            }
        });
    }

    private boolean isDataCorrect() {
        boolean dataCorrect = true;

        //email
        TextView textViewWrongEmail = findViewById(R.id.textViewWrongEmail);
        if (editTextEmail.getText().toString().equals("")){
            textViewWrongEmail.setVisibility(View.VISIBLE);
            dataCorrect = false;
        } else {
            textViewWrongEmail.setVisibility(View.GONE);
        }

        // username
        TextView textViewWrongUsername = findViewById(R.id.textViewWrongUsername);
        if (editTextUsername.getText().toString().equals("")){
            textViewWrongUsername.setVisibility(View.VISIBLE);
            dataCorrect = false;
        } else {
            textViewWrongUsername.setVisibility(View.GONE);
        }

        // password
        TextView textViewEnterPassword = findViewById(R.id.textViewEnterPassword);
        if (editTextPassword.getText().toString().equals("")){
            textViewEnterPassword.setVisibility(View.VISIBLE);
            dataCorrect = false;
        } else {
            textViewEnterPassword.setVisibility(View.GONE);
        }

        // password check
        TextView textViewPassDoesNotMatch = findViewById(R.id.textViewPassDoesNotMatch);
        if (!editTextPasswordCheck.getText().toString().matches(editTextPassword.getText().toString())){
            textViewPassDoesNotMatch.setVisibility(View.VISIBLE);
            dataCorrect = false;
        } else {
            textViewPassDoesNotMatch.setVisibility(View.GONE);
        }

        // gender
        TextView textViewNoGenderSelected = findViewById(R.id.textViewNoGenderSelected);
        if (!radioButtonWoman.isChecked() && !radioButtonMan.isChecked()){
            textViewNoGenderSelected.setVisibility(View.VISIBLE);
            dataCorrect = false;
        } else {
            textViewNoGenderSelected.setVisibility(View.GONE);
        }

        // birthday
        TextView textViewNoDateSelected = findViewById(R.id.textViewNoDateSelected);
        if (editTextBirthday.getText().toString().equals("")){
            textViewNoDateSelected.setVisibility(View.VISIBLE);
            dataCorrect = false;
        } else {
            textViewNoDateSelected.setVisibility(View.GONE);
        }

        // height
        TextView textViewEnterHeight = findViewById(R.id.textViewEnterHeight);
        if (editTextHeight.getText().toString().equals("")){
            textViewEnterHeight.setVisibility(View.VISIBLE);
            dataCorrect = false;
        } else {
            textViewEnterHeight.setVisibility(View.GONE);
        }

        // weight
        TextView textViewEnterWeight = findViewById(R.id.textViewEnterWeight);
        if (editTextWeight.getText().toString().equals("")){
            textViewEnterWeight.setVisibility(View.VISIBLE);
            dataCorrect = false;
        } else {
            textViewEnterWeight.setVisibility(View.GONE);
        }
        return dataCorrect;
    }
}