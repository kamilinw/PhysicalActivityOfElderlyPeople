package com.kamilwnek.pracainzynierskakamilwnek;

import androidx.appcompat.app.AppCompatActivity;

import android.app.DatePickerDialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Toast;

import com.parse.ParseException;
import com.parse.ParseUser;
import com.parse.SaveCallback;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class EditProfile extends AppCompatActivity {

    Calendar myCalendar;
    DatePickerDialog.OnDateSetListener date;
    EditText editTextBirthday;
    EditText editTextPassword;
    EditText editTextPasswordCheck;
    EditText editTextWeight;
    EditText editTextHeight;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(R.string.editProfile);
        getWindow().setStatusBarColor(getResources().getColor(R.color.colorPrimary));

        editTextPassword = findViewById(R.id.editTextPassword);
        editTextPasswordCheck = findViewById(R.id.editTextPasswordCheck);
        editTextBirthday = findViewById(R.id.editTextBirthday);
        editTextWeight = findViewById(R.id.editTextWeight);
        editTextHeight = findViewById(R.id.editTextHeight);

        editTextBirthday.setText(ParseUser.getCurrentUser().getString("birthday"));
        editTextHeight.setText(ParseUser.getCurrentUser().getString("height"));
        editTextWeight.setText(ParseUser.getCurrentUser().getString("weight"));

        myCalendar = Calendar.getInstance();
        String dateString = ParseUser.getCurrentUser().getString("birthday");
        String [] dateArray = dateString.split("/");
        int day = Integer.parseInt(dateArray[0]);
        int mouth = Integer.parseInt(dateArray[1]);
        int year = Integer.parseInt(dateArray[2]);
        myCalendar.set(year,mouth-1,day);

        date = new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker datePicker, int year, int month, int day) {
                myCalendar.set(Calendar.YEAR, year);
                myCalendar.set(Calendar.MONTH, month);
                myCalendar.set(Calendar.DAY_OF_MONTH, day);
                updateLabel();
            }
        };

        editTextBirthday.setCursorVisible(false);
        editTextBirthday.setShowSoftInputOnFocus(false);
        editTextBirthday.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean b) {
                if (!b)
                    return;

                InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                inputMethodManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
                showBirthdayPickerDialog();
            }
        });
        editTextBirthday.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showBirthdayPickerDialog();
            }
        });

    }

    private void showBirthdayPickerDialog() {
        DatePickerDialog dialog = new DatePickerDialog(
                EditProfile.this,
                android.R.style.Theme_Holo_Light_Dialog_MinWidth,
                date,
                myCalendar.get(Calendar.YEAR),
                myCalendar.get(Calendar.MONTH),
                myCalendar.get(Calendar.DAY_OF_MONTH));

        dialog.getWindow().setBackgroundDrawable(new ColorDrawable((Color.TRANSPARENT)));
        dialog.setTitle(R.string.selectBirthday);
        dialog.show();
    }

    public void onClickButtonSave (View view){
        if (isDataCorrect() && Common.checkInternetConnection(EditProfile.this)){

            ParseUser user = ParseUser.getCurrentUser();
            if (!editTextPassword.getText().toString().equals("")){
                user.setPassword(editTextPassword.getText().toString());
            }

            user.put("birthday",editTextBirthday.getText().toString());
            user.put("height",editTextHeight.getText().toString());
            user.put("weight",editTextWeight.getText().toString());
            user.saveInBackground(new SaveCallback() {
                @Override
                public void done(ParseException e) {
                    finish();
                }
            });

        } else {
            Toast.makeText(this,"Nie udało się zmienić danych",Toast.LENGTH_SHORT).show();
        }
    }

    private void updateLabel(){
        String myFormat = "dd/MM/yyyy";
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(myFormat, Locale.getDefault());
        editTextBirthday.setText(simpleDateFormat.format(myCalendar.getTime()));
    }

    boolean isDataCorrect(){
        boolean correct = true;

        if (!editTextPassword.getText().toString().matches(editTextPasswordCheck.getText().toString())){
            correct = false;
        }
        if (editTextHeight.getText().toString().equals("")){
            correct = false;
        }
        if (editTextWeight.getText().toString().equals("")){
            correct = false;
        }
        if (editTextBirthday.getText().toString().equals("")){
            correct = false;
        }

        return correct;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        finish();
        return true;
    }
}