package com.kamilwnek.pracainzynierskakamilwnek;

import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.kamilwnek.pracainzynierskakamilwnek.Pedometer.Database;
import com.parse.LogInCallback;
import com.parse.ParseException;
import com.parse.ParseUser;
import com.parse.RequestPasswordResetCallback;

public class LogInActivity extends AppCompatActivity {

    EditText editTextEmail;
    EditText editTextPassword;

    public void onClickLogIn(View view){

        Database db = Database.getInstance(getApplicationContext());
        db.deleteData();
        db.close();

        TextView textViewLogin = findViewById(R.id.textViewLogin);
        Animation alpha = AnimationUtils.loadAnimation(this,R.anim.alpha);
        view.startAnimation(alpha);
        textViewLogin.setAnimation(alpha);
        if (isDataCorrect() && Common.checkInternetConnection(LogInActivity.this)){
            ParseUser.logInInBackground(editTextEmail.getText().toString(), editTextPassword.getText().toString(), new LogInCallback() {
                @Override
                public void done(ParseUser user, ParseException e) {
                    if (e == null) {
                        boolean emailVerified = user.getBoolean("emailVerified");
                        if (emailVerified) {
                            Database db = Database.getInstance(getApplicationContext());
                            db.downloadDataFromServer();
                            db.close();
                            Intent intent = new Intent(getApplicationContext(),MainActivity.class);
                            startActivity(intent);
                            finish();
                            Log.i("Logging","Success!");
                        } else {
                            Toast.makeText(LogInActivity.this, R.string.pleaseVerifyEmail, Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Log.i("Log in", "Failed with error: " + e.toString());
                        Toast.makeText(LogInActivity.this, R.string.invalidEmailOrPassword, Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_log_in);
        getSupportActionBar().hide();
        getWindow().setStatusBarColor(getResources().getColor(R.color.colorPrimary));

        editTextEmail = findViewById(R.id.editTextEmail);
        editTextPassword = findViewById(R.id.editTextPassword);
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

        // password
        TextView textViewEnterPassword = findViewById(R.id.textViewEnterPassword);
        if (editTextPassword.getText().toString().equals("")){
            textViewEnterPassword.setVisibility(View.VISIBLE);
            dataCorrect = false;
        } else {
            textViewEnterPassword.setVisibility(View.GONE);
        }

        return dataCorrect;
    }

    public void onClickForgotPassword(View view) {
        Animation alpha = AnimationUtils.loadAnimation(this,R.anim.alpha);
        view.startAnimation(alpha);

        if (Common.checkInternetConnection(LogInActivity.this)) {
            final EditText email = new EditText(this);
            email.setText(editTextEmail.getText().toString());
            if (email.getLayoutParams() instanceof ViewGroup.MarginLayoutParams){
                ViewGroup.MarginLayoutParams  p = (ViewGroup.MarginLayoutParams) email.getLayoutParams();

                int paddingDp = 25;
                float density = this.getResources().getDisplayMetrics().density;
                int paddingPixel = (int)(paddingDp * density);

                p.setMargins(paddingPixel,paddingPixel,paddingPixel,paddingPixel);
                email.requestLayout();
            }

            new AlertDialog.Builder(LogInActivity.this)
                    .setTitle(R.string.forgetPasswordAlertTitle)
                    .setMessage(R.string.forgetPasswordAlertMeesage)
                    .setView(email)
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            ParseUser.requestPasswordResetInBackground(email.getText().toString(), new RequestPasswordResetCallback() {
                                public void done(ParseException e) {
                                    if (e == null) {
                                        Common.infoAlertDialog(LogInActivity.this,R.string.resetPasswordSuccess);
                                    } else {
                                        Common.infoAlertDialog(LogInActivity.this,R.string.somethingWentWrong);
                                    }
                                }
                            });
                        }
                    })
                    .setNegativeButton(R.string.camcel, null)
                    .show();
        }
    }
}