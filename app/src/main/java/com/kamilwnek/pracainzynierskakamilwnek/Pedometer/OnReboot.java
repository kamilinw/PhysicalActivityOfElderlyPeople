package com.kamilwnek.pracainzynierskakamilwnek.Pedometer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;

import com.kamilwnek.pracainzynierskakamilwnek.BuildConfig;
import com.parse.ParseUser;

public class OnReboot extends BroadcastReceiver {

    @Override
    public void onReceive(final Context context, final Intent intent) {
        if (ParseUser.getCurrentUser() == null){
            Log.i("OnReboot", "No user logged in");
            return;
        }

        if (BuildConfig.DEBUG) Log.i("OnReboot","booted");

        SharedPreferences prefs = context.getSharedPreferences("pedometer", Context.MODE_PRIVATE);

        Database db = Database.getInstance(context);

        if (!prefs.getBoolean("correctShutdown", false)) {
            if (BuildConfig.DEBUG) Log.i("OnReboot","Incorrect shutdown");
            int steps = Math.max(0, db.getCurrentSteps());
            if (BuildConfig.DEBUG) Log.i("OnReboot","Trying to recover " + steps + " steps");
            db.addToLastEntry(steps);
        }

        db.removeNegativeEntries();
        db.saveCurrentSteps(0);
        db.close();
        prefs.edit().remove("correctShutdown").apply();

        if (Build.VERSION.SDK_INT >= 26) {
            Log.i("StartService", "Build version > 26");
            context.startForegroundService(new Intent(context, SensorListener.class));
        } else {
            Log.i("StartService", "Build version < 26");
            context.startService(new Intent(context, SensorListener.class));
        }
    }
}