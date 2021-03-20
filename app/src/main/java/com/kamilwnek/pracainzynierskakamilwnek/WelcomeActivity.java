package com.kamilwnek.pracainzynierskakamilwnek;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.TextView;

import com.parse.ParseAnalytics;
import com.parse.ParseUser;

public class WelcomeActivity extends AppCompatActivity {

    public void joinNowOnClick(View view){
        TextView signUpTextView = findViewById(R.id.signUpTextView);
        Animation alpha = AnimationUtils.loadAnimation(this,R.anim.alpha);
        view.startAnimation(alpha);
        signUpTextView.setAnimation(alpha);
        Intent intent = new Intent(getApplicationContext(),SignUpActivity.class);
        startActivity(intent);
    }
    
    public void logInOnClick(View view){
        TextView logInTextView = findViewById(R.id.logInTextView);
        Animation alpha = AnimationUtils.loadAnimation(this,R.anim.alpha);
        view.startAnimation(alpha);
        logInTextView.setAnimation(alpha);
        Intent intent = new Intent(getApplicationContext(),LogInActivity.class);
        startActivity(intent);
    }
    
    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);
        getSupportActionBar().hide();

        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        );

        ParseUser currentUser = ParseUser.getCurrentUser();
        if (currentUser != null) {

            Intent intent = new Intent(getApplicationContext(),MainActivity.class);
            startActivity(intent);
            finish(); return;
        }

        ParseAnalytics.trackAppOpenedInBackground(getIntent());
    }
}